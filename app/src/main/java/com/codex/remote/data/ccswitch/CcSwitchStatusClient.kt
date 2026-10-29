package com.codex.remote.data.ccswitch

import com.codex.remote.data.ssh.ActiveSshTransport
import com.codex.remote.data.ssh.RemoteCommandResult
import com.codex.remote.domain.CcSwitchQuotaSnapshot
import com.codex.remote.domain.CcSwitchRemoteStatus
import com.codex.remote.domain.RemotePlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

class CcSwitchStatusClient(
    private val remotePlatform: RemotePlatform,
    private val runCommand: suspend (command: String, timeoutSeconds: Long) -> RemoteCommandResult,
) {
    constructor(transport: ActiveSshTransport) : this(transport.remotePlatform, transport::runCommand)

    suspend fun readCurrentStatus(): CcSwitchRemoteStatus? {
        val providerList = runCommand(providerListCommand(), PROVIDER_LIST_TIMEOUT_SECONDS)
        if (providerList.exitStatus != 0) {
            val detail = providerList.stderr.lineSequence()
                .map(String::trim)
                .lastOrNull(String::isNotBlank)
                .orEmpty()
            if (providerList.exitStatus == 127 ||
                detail.contains("command not found", ignoreCase = true) ||
                detail.contains("is not recognized", ignoreCase = true)
            ) {
                return null
            }
            return CcSwitchRemoteStatus(
                providerId = "unknown",
                providerName = "CC Switch",
                status = "error",
                available = false,
                queriedAt = null,
                quotas = emptyList(),
                error = detail.ifBlank { "cc-switch provider list 执行失败" },
            )
        }

        val providerId = parseCurrentProviderId(providerList.stdout + "\n" + providerList.stderr)
            ?: return CcSwitchRemoteStatus(
                providerId = "unknown",
                providerName = "未知供应商",
                status = "error",
                available = false,
                queriedAt = null,
                quotas = emptyList(),
                error = "CC Switch 已运行，但没有识别到当前 Codex 供应商",
            )

        val quotaResult = runCommand(
            quotaCommand(providerId),
            QUOTA_TIMEOUT_SECONDS,
        )
        val jsonText = quotaResult.stdout.trim()
        if (!jsonText.startsWith("{")) {
            val detail = quotaResult.stderr.lineSequence()
                .map(String::trim)
                .lastOrNull(String::isNotBlank)
                ?: jsonText.takeIf(String::isNotBlank)
                ?: "cc-switch provider quota 未返回 JSON"
            return CcSwitchRemoteStatus(
                providerId = providerId,
                providerName = providerId,
                status = "error",
                available = false,
                queriedAt = null,
                quotas = emptyList(),
                error = detail,
            )
        }
        return parseQuotaResult(jsonText, providerId)
    }

    private fun providerListCommand(): String = when (remotePlatform) {
        RemotePlatform.POSIX ->
            "exec \"\${SHELL:-/bin/sh}\" -lc 'exec cc-switch -a codex provider list'"

        RemotePlatform.WINDOWS ->
            "powershell.exe -NoLogo -NonInteractive -Command \"& { cc-switch -a codex provider list }\""

        RemotePlatform.AUTO -> error("查询 CC Switch 前必须先识别远端系统")
    }

    private fun quotaCommand(providerId: String): String {
        require(SAFE_PROVIDER_ID.matches(providerId)) {
            "CC Switch 返回了不受支持的供应商 ID"
        }
        return when (remotePlatform) {
            RemotePlatform.POSIX ->
                "exec \"\${SHELL:-/bin/sh}\" -lc 'exec cc-switch -a codex provider quota $providerId --json'"

            RemotePlatform.WINDOWS ->
                "powershell.exe -NoLogo -NonInteractive -Command \"& { cc-switch -a codex provider quota $providerId --json }\""

            RemotePlatform.AUTO -> error("查询 CC Switch 前必须先识别远端系统")
        }
    }

    private companion object {
        const val PROVIDER_LIST_TIMEOUT_SECONDS = 15L
        const val QUOTA_TIMEOUT_SECONDS = 30L
    }
}

internal fun parseCurrentProviderId(output: String): String? {
    val clean = stripAnsi(output)
    CURRENT_PROVIDER.find(clean)?.groupValues?.getOrNull(1)?.let { return it }
    CHECKED_PROVIDER.find(clean)?.groupValues?.getOrNull(1)?.let { return it }
    return null
}

internal fun parseQuotaResult(
    output: String,
    fallbackProviderId: String,
): CcSwitchRemoteStatus {
    val root = Json.parseToJsonElement(output).jsonObject
    val result = root.objectOrNull("result")
    val quota = result?.objectOrNull("quota")
    val quotas = quota?.arrayOrNull("data")
        ?.mapNotNull(::parseQuotaItem)
        .orEmpty()
    val quotaSucceeded = quota?.booleanOrNull("success")
    val rootError = root.textOrNull("error")
    val error = when {
        !rootError.isNullOrBlank() -> rootError
        quotaSucceeded == false -> "CC Switch 未返回可用的额度数据"
        else -> null
    }
    return CcSwitchRemoteStatus(
        providerId = root.textOrNull("providerId") ?: fallbackProviderId,
        providerName = root.textOrNull("providerName") ?: fallbackProviderId,
        status = root.textOrNull("status"),
        available = root.booleanOrNull("available"),
        queriedAt = root.longOrNull("queriedAt"),
        quotas = quotas,
        error = error,
    )
}

private fun parseQuotaItem(element: JsonElement): CcSwitchQuotaSnapshot? {
    val item = element as? JsonObject ?: return null
    return CcSwitchQuotaSnapshot(
        planName = item.textOrNull("planName"),
        remaining = item.textOrNull("remaining"),
        unit = item.textOrNull("unit"),
        isValid = item.booleanOrNull("isValid"),
    )
}

private fun JsonObject.objectOrNull(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.arrayOrNull(key: String): JsonArray? = try {
    this[key]?.jsonArray
} catch (_: IllegalArgumentException) {
    null
}

private fun JsonObject.textOrNull(key: String): String? {
    val value = this[key]
    if (value is JsonPrimitive) {
        return value.contentOrNull?.takeIf(String::isNotBlank)
    }
    val objectValue = value as? JsonObject ?: return null
    return objectValue["message"]?.let { message ->
        (message as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
    }
}

private fun JsonObject.booleanOrNull(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.booleanOrNull

private fun JsonObject.longOrNull(key: String): Long? =
    (this[key] as? JsonPrimitive)?.longOrNull

private fun stripAnsi(value: String): String = ANSI_ESCAPE.replace(value, "")

private val ANSI_ESCAPE = Regex("\u001B\\[[;\\d]*[ -/]*[@-~]")
private val CURRENT_PROVIDER = Regex("""(?m)^\s*→\s*Current:\s*([A-Za-z0-9._-]+)\s*$""")
private val CHECKED_PROVIDER = Regex("""(?m)^.*?✓\s*┆\s*([A-Za-z0-9._-]+)\s*┆""")
private val SAFE_PROVIDER_ID = Regex("""[A-Za-z0-9._-]+""")
