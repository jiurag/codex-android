package com.codex.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.codex.remote.data.update.GitHubUpdateClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GitHubUpdateDeviceTest {
    @Test
    fun publicManifestIsReadableThroughOfficialGitHubRedirects() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val update = withTimeout(30_000) {
            GitHubUpdateClient(context).findUpdate(
                manifestUrl = BuildConfig.UPDATE_MANIFEST_URL,
                currentVersionCode = 20,
            )
        }

        assertNotNull("公开仓库应返回可安装的更新", update)
        assertTrue("更新版本应高于 versionCode 20", requireNotNull(update).versionCode > 20)
        assertTrue("更新 APK 地址必须指向 GitHub Release", update.apkUrl.contains("/releases/download/"))
    }
}
