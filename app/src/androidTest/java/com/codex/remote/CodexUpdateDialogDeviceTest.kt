package com.codex.remote

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codex.remote.domain.AppUpdateState
import com.codex.remote.domain.UpdateFailureStage
import com.codex.remote.domain.UpdatePhase
import com.codex.remote.ui.components.CodexUpdateDialog
import com.codex.remote.ui.theme.CodexRemoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CodexUpdateDialogDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun availableUpdateShowsVersionAndStartsDownload() {
        val state = mutableStateOf(
            AppUpdateState(
                phase = UpdatePhase.AVAILABLE,
                versionCode = 23,
                versionName = "1.0.14",
                changelog = "改善更新体验。",
                showDialog = true,
            ),
        )
        var downloads = 0
        show(state, onDownload = { downloads += 1 })

        composeRule.onNodeWithText("Codex 1.0.14").assertIsDisplayed()
        composeRule.onNodeWithText("1.0.13  →  1.0.14").assertIsDisplayed()
        composeRule.onNodeWithTag("update-action-download").performClick()
        composeRule.runOnIdle { assertEquals(1, downloads) }
    }

    @Test
    fun verifiedUpdateStartsSystemInstallerFlow() {
        val state = mutableStateOf(
            AppUpdateState(
                phase = UpdatePhase.READY,
                versionCode = 23,
                versionName = "1.0.14",
                progressPercent = 100,
                showDialog = true,
            ),
        )
        var installs = 0
        show(state, onInstall = { installs += 1 })

        composeRule.onNodeWithText("安装包已下载，SHA-256 和应用签名校验通过。")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("update-action-install").performClick()
        composeRule.runOnIdle { assertEquals(1, installs) }
    }

    @Test
    fun failedDownloadOffersRetry() {
        val state = mutableStateOf(
            AppUpdateState(
                phase = UpdatePhase.ERROR,
                versionCode = 23,
                versionName = "1.0.14",
                error = "更新包 SHA-256 校验失败",
                failureStage = UpdateFailureStage.DOWNLOAD,
                showDialog = true,
            ),
        )
        var retries = 0
        show(state, onRetry = { retries += 1 })

        composeRule.onNodeWithText("更新包 SHA-256 校验失败").assertIsDisplayed()
        composeRule.onNodeWithTag("update-action-retry").performClick()
        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    private fun show(
        state: androidx.compose.runtime.MutableState<AppUpdateState>,
        onDownload: () -> Unit = {},
        onInstall: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        composeRule.setContent {
            CodexRemoteTheme(darkTheme = false) {
                CodexUpdateDialog(
                    state = state.value,
                    currentVersionName = "1.0.13",
                    onDismiss = {},
                    onDownload = onDownload,
                    onInstall = onInstall,
                    onRetry = onRetry,
                )
            }
        }
    }
}
