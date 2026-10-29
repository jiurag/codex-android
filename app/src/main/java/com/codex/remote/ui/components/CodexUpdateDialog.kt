package com.codex.remote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.codex.remote.domain.AppUpdateState
import com.codex.remote.domain.UpdatePhase

@Composable
fun CodexUpdateDialog(
    state: AppUpdateState,
    currentVersionName: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
) {
    if (!state.showDialog || state.phase == UpdatePhase.IDLE) return

    val targetVersion = state.versionName?.let { "Codex $it" } ?: "新版本"
    val title = when (state.phase) {
        UpdatePhase.CHECKING -> "正在检查更新"
        UpdatePhase.UP_TO_DATE -> "已是最新版本"
        UpdatePhase.AVAILABLE -> targetVersion
        UpdatePhase.DOWNLOADING -> "正在下载 $targetVersion"
        UpdatePhase.READY -> "更新已准备好"
        UpdatePhase.ERROR -> "更新未完成"
        UpdatePhase.IDLE -> return
    }

    CodexDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("update-dialog"),
        icon = {
            CodexBrandMark(
                modifier = Modifier.size(42.dp),
                cornerRadius = 8.dp,
            )
        },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.phase) {
                    UpdatePhase.CHECKING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                            Text("正在从 GitHub Release 获取版本信息。")
                        }
                    }
                    UpdatePhase.UP_TO_DATE -> Text(
                        "当前版本 $currentVersionName 已经是最新版本。",
                    )
                    UpdatePhase.AVAILABLE -> {
                        VersionTransition(
                            currentVersionName = currentVersionName,
                            targetVersionName = state.versionName,
                        )
                        state.changelog?.let { changelog ->
                            Text(
                                changelog,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SecurityNote()
                    }
                    UpdatePhase.DOWNLOADING -> {
                        VersionTransition(
                            currentVersionName = currentVersionName,
                            targetVersionName = state.versionName,
                        )
                        LinearProgressIndicator(
                            progress = { (state.progressPercent ?: 0) / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${state.progressPercent ?: 0}% · 下载完成后会自动校验",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    UpdatePhase.READY -> {
                        VersionTransition(
                            currentVersionName = currentVersionName,
                            targetVersionName = state.versionName,
                        )
                        Text("安装包已下载，SHA-256 和应用签名校验通过。")
                        if (state.installPermissionRequired) {
                            Text(
                                "请允许 Codex 安装未知应用，返回后再次点击“安装更新”。",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    UpdatePhase.ERROR -> {
                        state.versionName?.let {
                            VersionTransition(
                                currentVersionName = currentVersionName,
                                targetVersionName = it,
                            )
                        }
                        Text(
                            state.error ?: "更新失败，请稍后重试。",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    UpdatePhase.IDLE -> Unit
                }
            }
        },
        confirmButton = {
            when (state.phase) {
                UpdatePhase.CHECKING -> CodexTextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("update-action-dismiss"),
                ) { Text("隐藏") }
                UpdatePhase.UP_TO_DATE -> CodexButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("update-action-done"),
                ) { Text("完成") }
                UpdatePhase.AVAILABLE -> CodexButton(
                    onClick = onDownload,
                    modifier = Modifier.testTag("update-action-download"),
                ) { Text("下载更新") }
                UpdatePhase.DOWNLOADING -> CodexTextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("update-action-hide"),
                ) { Text("隐藏") }
                UpdatePhase.READY -> CodexButton(
                    onClick = onInstall,
                    modifier = Modifier.testTag("update-action-install"),
                ) { Text("安装更新") }
                UpdatePhase.ERROR -> CodexButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("update-action-retry"),
                ) { Text("重试") }
                UpdatePhase.IDLE -> Unit
            }
        },
        dismissButton = when (state.phase) {
            UpdatePhase.AVAILABLE,
            UpdatePhase.READY,
            UpdatePhase.ERROR,
            -> {
                {
                    CodexTextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update-action-later"),
                    ) { Text("稍后") }
                }
            }
            else -> null
        },
    )
}

@Composable
private fun VersionTransition(
    currentVersionName: String,
    targetVersionName: String?,
) {
    Text(
        "$currentVersionName  →  ${targetVersionName ?: "新版本"}",
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SecurityNote() {
    Text(
        "安装包来自 GitHub Release，并会校验 SHA-256 与应用签名。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
