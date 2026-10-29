package com.codex.remote.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

@Serializable
data class UpdateManifest(
    val formatVersion: Int,
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val changelog: String = "",
)

class GitHubUpdateClient(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    suspend fun findUpdate(
        manifestUrl: String,
        currentVersionCode: Int,
    ): UpdateManifest? = withContext(Dispatchers.IO) {
        validateManifestUrl(manifestUrl)
        val connection = openConnection(
            rawUrl = manifestUrl,
            allowedHosts = MANIFEST_HOSTS,
            connectTimeoutMs = 15_000,
            readTimeoutMs = 20_000,
        )
        try {
            val response = readLimited(connection.inputStream, MAX_MANIFEST_BYTES)
                .toString(StandardCharsets.UTF_8)
            val manifest = runCatching { json.decodeFromString<UpdateManifest>(response) }
                .getOrElse { throw IOException("更新清单格式无效") }
            validateManifest(manifest)
            manifest.takeIf { it.versionCode > currentVersionCode }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadAndVerify(
        manifest: UpdateManifest,
        currentVersionCode: Int,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        validateManifest(manifest)
        require(manifest.versionCode > currentVersionCode) {
            "更新版本不高于当前版本"
        }

        val updateDirectory = File(context.cacheDir, UPDATE_DIRECTORY).apply { mkdirs() }
        if (!updateDirectory.isDirectory) {
            throw IOException("无法创建更新缓存目录")
        }
        val target = File(updateDirectory, "codex-android-${manifest.versionCode}.apk")
        if (target.isFile &&
            target.length() == manifest.sizeBytes &&
            sha256(target) == manifest.sha256
        ) {
            verifyPackage(target, manifest, currentVersionCode)
            onProgress(100)
            return@withContext target
        }

        val temporary = File(updateDirectory, "${target.name}.part")
        temporary.delete()
        val digest = MessageDigest.getInstance("SHA-256")
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(
                rawUrl = manifest.apkUrl,
                allowedHosts = DOWNLOAD_HOSTS,
                connectTimeoutMs = 20_000,
                readTimeoutMs = 60_000,
            )
            val contentLength = connection.contentLengthLong
            if (contentLength > manifest.sizeBytes) {
                throw IOException("更新包大小与清单不一致")
            }

            var downloaded = 0L
            var lastProgress = -1
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        if (downloaded > manifest.sizeBytes) {
                            throw IOException("更新包大小与清单不一致")
                        }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        val progress = ((downloaded * 100) / manifest.sizeBytes)
                            .coerceIn(0, 100)
                            .toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(progress)
                        }
                    }
                    output.fd.sync()
                }
            }

            if (downloaded != manifest.sizeBytes) {
                throw IOException("更新包下载不完整")
            }
            val actualHash = digest.digest().toHex()
            if (actualHash != manifest.sha256) {
                throw IOException("更新包 SHA-256 校验失败")
            }
            verifyPackage(temporary, manifest, currentVersionCode)
            removeIfExists(target)
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            onProgress(100)
            target
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        } finally {
            connection?.disconnect()
        }
    }

    fun deleteDownloadedUpdate(versionCode: Int) {
        val directory = File(context.cacheDir, UPDATE_DIRECTORY)
        File(directory, "codex-android-$versionCode.apk").delete()
        File(directory, "codex-android-$versionCode.apk.part").delete()
    }

    private fun validateManifest(manifest: UpdateManifest) {
        if (manifest.formatVersion != MANIFEST_FORMAT_VERSION) {
            throw IOException("更新清单版本不受支持")
        }
        if (manifest.versionCode <= 0) {
            throw IOException("更新版本号无效")
        }
        if (!VERSION_NAME_PATTERN.matches(manifest.versionName)) {
            throw IOException("更新版本名称无效")
        }
        if (manifest.sizeBytes !in 1..MAX_APK_BYTES) {
            throw IOException("更新包大小超出限制")
        }
        if (!SHA256_PATTERN.matches(manifest.sha256.lowercase(Locale.ROOT))) {
            throw IOException("更新包校验值无效")
        }
        if (manifest.changelog.length > MAX_CHANGELOG_CHARACTERS) {
            throw IOException("更新说明过长")
        }
        val apkUrl = runCatching { URL(manifest.apkUrl) }
            .getOrElse { throw IOException("更新包地址无效") }
        if (apkUrl.protocol != "https" ||
            apkUrl.host.lowercase(Locale.ROOT) !in DOWNLOAD_HOSTS ||
            !apkUrl.path.startsWith(APK_RELEASE_PATH_PREFIX)
        ) {
            throw IOException("更新包地址不是允许的 GitHub Release")
        }
    }

    private fun validateManifestUrl(manifestUrl: String) {
        val url = runCatching { URL(manifestUrl) }
            .getOrElse { throw IOException("更新清单地址无效") }
        if (url.protocol != "https" ||
            url.host.lowercase(Locale.ROOT) !in MANIFEST_HOSTS ||
            url.path != MANIFEST_PATH
        ) {
            throw IOException("更新清单地址不是允许的 GitHub Release")
        }
    }

    private fun openConnection(
        rawUrl: String,
        allowedHosts: Set<String>,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
    ): HttpURLConnection {
        var current = URL(rawUrl)
        repeat(MAX_REDIRECTS + 1) {
            if (current.protocol != "https" ||
                current.host.lowercase(Locale.ROOT) !in allowedHosts
            ) {
                throw IOException("更新地址跳转到了不受信任的主机")
            }
            val connection = current.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/octet-stream, application/json")
            connection.setRequestProperty("User-Agent", "${context.packageName}/${appVersionName()}")
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IOException("更新地址跳转缺少目标")
                current = URL(current, location)
                connection.disconnect()
                return@repeat
            }
            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw IOException("更新服务器返回 HTTP $code")
            }
            return connection
        }
        throw IOException("更新地址跳转次数过多")
    }

    private fun verifyPackage(
        file: File,
        manifest: UpdateManifest,
        currentVersionCode: Int,
    ) {
        val packageManager = context.packageManager
        val archive = packageManager.getPackageArchiveInfo(file.absolutePath, signingFlags())
            ?: throw IOException("下载的文件不是有效 APK")
        if (archive.packageName != context.packageName) {
            throw IOException("更新包应用标识不匹配")
        }
        if (archive.versionCodeLong() != manifest.versionCode.toLong()) {
            throw IOException("更新包版本与清单不一致")
        }
        if (archive.versionCodeLong() <= currentVersionCode.toLong()) {
            throw IOException("更新包版本不高于当前版本")
        }
        val installed = packageManager.getPackageInfo(context.packageName, signingFlags())
        val archiveSigners = archive.signers()
        val installedSigners = installed.signers()
        if (archiveSigners.isEmpty() ||
            installedSigners.isEmpty() ||
            archiveSigners.size != installedSigners.size ||
            archiveSigners.all { candidate ->
                installedSigners.any { installedSigner -> installedSigner.contentEquals(candidate) }
            }.not()
        ) {
            throw IOException("更新包签名与当前应用不一致")
        }
    }

    private fun signingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private fun PackageInfo.signers(): List<ByteArray> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            signatures?.map { it.toByteArray() }.orEmpty()
        }

    private fun PackageInfo.versionCodeLong(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }

    private fun appVersionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    private fun readLimited(
        input: java.io.InputStream,
        limit: Int,
    ): ByteArray = input.use {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = it.read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) throw IOException("更新清单过大")
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    private fun removeIfExists(file: File) {
        if (file.exists() && !file.delete()) {
            throw IOException("无法替换旧更新包")
        }
    }

    private companion object {
        const val UPDATE_DIRECTORY = "updates"
        const val MANIFEST_FORMAT_VERSION = 1
        const val MAX_MANIFEST_BYTES = 64 * 1024
        const val MAX_APK_BYTES = 200L * 1024 * 1024
        const val MAX_CHANGELOG_CHARACTERS = 4_000
        const val MAX_REDIRECTS = 5
        const val MANIFEST_PATH = "/jiurag/codex-android/releases/latest/download/latest.json"
        const val APK_RELEASE_PATH_PREFIX = "/jiurag/codex-android/releases/download/"
        val MANIFEST_HOSTS = setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
        )
        val DOWNLOAD_HOSTS = setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
        )
        val VERSION_NAME_PATTERN = Regex("^[0-9A-Za-z][0-9A-Za-z._+-]{0,63}$")
        val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
    }
}
