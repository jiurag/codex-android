package com.codex.remote.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.codex.remote.BuildConfig
import com.codex.remote.R
import com.codex.remote.domain.AppUiState
import com.codex.remote.domain.AuthType
import com.codex.remote.domain.ConnectionDraft
import com.codex.remote.domain.ConnectionDraftIssue
import com.codex.remote.domain.ConnectionStatus
import com.codex.remote.domain.RemotePlatform
import com.codex.remote.domain.SavedConnection
import com.codex.remote.domain.validationIssues
import com.codex.remote.ui.components.CodexChoiceChip
import com.codex.remote.ui.components.CodexButton
import com.codex.remote.ui.components.CodexBrandMark
import com.codex.remote.ui.components.CodexDialog as AlertDialog
import com.codex.remote.ui.components.CodexOutlinedButton
import com.codex.remote.ui.components.CodexPanel
import com.codex.remote.ui.components.CodexSectionHeader
import com.codex.remote.ui.components.CodexTextButton as TextButton
import com.codex.remote.ui.components.CodexTopBar
import com.codex.remote.ui.components.codexTextFieldColors
import com.codex.remote.ui.icons.CodexIcons
import com.codex.remote.ui.theme.CodexMidnight
import com.codex.remote.domain.UpdatePhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (SavedConnection?) -> Unit,
    onDelete: (SavedConnection) -> Unit,
    onConnect: (SavedConnection) -> Unit,
    onSave: (ConnectionDraft, Boolean) -> Unit,
    onCloseEditor: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDismissNotice: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<SavedConnection?>(null) }
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbarHostState.showSnackbar(it)
            onDismissNotice()
        }
    }

    if (state.showConnectionEditor) {
        ConnectionEditor(
            original = state.editingConnection,
            busy = state.isBusy,
            onDismiss = onCloseEditor,
            onSave = onSave,
        )
    } else {
        val hasConnections = state.savedConnections.isNotEmpty()
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CodexTopBar(
                    title = "远程主机",
                    subtitle = if (hasConnections) {
                        "${state.savedConnections.size} 台主机 · 凭据仅保存在本机"
                    } else {
                        "配置 SSH，连接远端 Codex"
                    },
                    navigationIcon = {
                        if (state.activeConnection != null) {
                            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                                Icon(CodexIcons.ArrowBack, contentDescription = "返回工作区")
                            }
                        }
                    },
                    actions = {
                        if (state.update.phase == UpdatePhase.CHECKING) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(horizontal = 13.dp).size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            IconButton(
                                onClick = onCheckForUpdates,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    CodexIcons.Refresh,
                                    contentDescription = "检查更新",
                                )
                            }
                        }
                        if (hasConnections) {
                            IconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) {
                                Icon(CodexIcons.Add, contentDescription = "添加连接")
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (state.savedConnections.isEmpty()) {
                    EmptyConnections(onAdd)
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CodexSectionHeader(
                            title = "SSH 主机",
                            hint = "${state.savedConnections.size} 个连接",
                        )
                        CodexPanel(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                state.savedConnections.forEachIndexed { index, connection ->
                                    ConnectionRow(
                                        connection = connection,
                                        isActive = state.activeConnection?.id == connection.id,
                                        isConnecting = state.activeConnection?.id == connection.id && state.connectionStatus == ConnectionStatus.CONNECTING,
                                        onConnect = { onConnect(connection) },
                                        onEdit = { onEdit(connection) },
                                        onDelete = { pendingDelete = connection },
                                    )
                                    if (index != state.savedConnections.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 64.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                CodexIcons.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "凭据由 Android Keystore 加密；主机密钥变化时会阻止连接。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            stringResource(R.string.misans_attribution),
                            modifier = Modifier.padding(start = 25.dp, top = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Codex ${BuildConfig.VERSION_NAME}",
                            modifier = Modifier.padding(start = 25.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        pendingDelete?.let { connection ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除 ${connection.name}？") },
                text = { Text("保存的主机和加密凭据将从此设备移除。") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDelete = null
                        onDelete(connection)
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
            )
        }
    }
}

@Composable
private fun EmptyConnections(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight().widthIn(max = 460.dp).padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CodexBrandMark(
            modifier = Modifier.size(64.dp),
            cornerRadius = 12.dp,
        )
        Spacer(Modifier.height(22.dp))
        Text("连接一台远端 Codex", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "智能体、仓库和工具都在远端运行。连接后导入已有项目与任务，不复制代码到手机。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 340.dp),
        )
        Spacer(Modifier.height(26.dp))
        CodexButton(
            onClick = onAdd,
            modifier = Modifier.height(48.dp),
        ) {
            Icon(CodexIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("添加 SSH 连接")
        }
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.misans_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Codex ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectionRow(
    connection: SavedConnection,
    isActive: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember(connection.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 74.dp).padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    CodexIcons.Router,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    connection.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                if (isActive) {
                    Spacer(Modifier.width(7.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "当前",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            Text(
                "${connection.username}@${connection.host}:${connection.port}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isConnecting) {
            CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(20.dp), strokeWidth = 2.dp)
        } else {
            TextButton(
                onClick = onConnect,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (isActive) "重新连接" else "连接")
            }
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(CodexIcons.MoreVert, contentDescription = "主机操作")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("编辑连接") },
                    leadingIcon = { Icon(CodexIcons.Edit, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除连接", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(CodexIcons.DeleteOutline, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectionEditor(
    original: SavedConnection?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (ConnectionDraft, Boolean) -> Unit,
) {
    var draft by remember(original?.id) { mutableStateOf(original.toDraft()) }
    var attemptedSave by remember(original?.id) { mutableStateOf(false) }
    val validationIssues = draft.validationIssues(original)
    val validationIssue = validationIssues.firstOrNull()
    val submit: (Boolean) -> Unit = { connectAfterSave ->
        attemptedSave = true
        if (validationIssues.isEmpty()) onSave(draft, connectAfterSave)
    }
    BackHandler(enabled = !busy, onBack = onDismiss)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("connection-editor-root"),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 8.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            enabled = !busy,
                            modifier = Modifier.size(48.dp).testTag("connection-action-cancel"),
                        ) {
                            Icon(CodexIcons.ArrowBack, contentDescription = "关闭")
                        }
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                if (original == null) "添加 SSH 连接" else "编辑 SSH 连接",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.testTag("connection-editor-title"),
                            )
                            Text(
                                "仅保存连接参数，Codex 仍在远端运行",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            bottomBar = {
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider()
                    if (attemptedSave && validationIssue != null) {
                        Text(
                            validationIssue.message,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    ConnectionEditorActions(
                        busy = busy,
                        onSave = { submit(false) },
                        onSaveAndConnect = { submit(true) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            },
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(it),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.testTag("connection-editor-scroll")
                        .widthIn(max = 720.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                        CodexSectionHeader("连接信息", hint = "用于在连接列表中识别这台主机")
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { draft = draft.copy(name = it) },
                            label = { Text("显示名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = codexTextFieldColors(),
                            isError = attemptedSave && ConnectionDraftIssue.CONNECTION_NAME in validationIssues,
                        )
                        Text(
                            "项目和会话来自远端 Codex 历史记录。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CodexSectionHeader("SSH 主机", hint = "地址、端口和登录账户")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = draft.host,
                                onValueChange = { draft = draft.copy(host = it) },
                                label = { Text("主机地址") },
                                placeholder = { Text("devbox.example.com") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                colors = codexTextFieldColors(),
                                isError = attemptedSave && ConnectionDraftIssue.HOST in validationIssues,
                            )
                            OutlinedTextField(
                                value = draft.port,
                                onValueChange = { draft = draft.copy(port = it.filter(Char::isDigit)) },
                                label = { Text("端口") },
                                singleLine = true,
                                modifier = Modifier.width(104.dp),
                                shape = MaterialTheme.shapes.small,
                                colors = codexTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = attemptedSave && ConnectionDraftIssue.PORT in validationIssues,
                            )
                        }
                        OutlinedTextField(
                            value = draft.username,
                            onValueChange = { draft = draft.copy(username = it) },
                            label = { Text("用户名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = codexTextFieldColors(),
                            isError = attemptedSave && ConnectionDraftIssue.USERNAME in validationIssues,
                        )
                        CodexSectionHeader("认证方式", hint = "选择一种凭据")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CodexChoiceChip(
                                selected = draft.authType == AuthType.PASSWORD,
                                onClick = { draft = draft.copy(authType = AuthType.PASSWORD) },
                                label = "密码",
                                leadingIcon = {
                                    Icon(CodexIcons.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                            )
                            CodexChoiceChip(
                                selected = draft.authType == AuthType.PRIVATE_KEY,
                                onClick = { draft = draft.copy(authType = AuthType.PRIVATE_KEY) },
                                label = "私钥",
                                leadingIcon = {
                                    Icon(CodexIcons.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                            )
                        }
                        if (draft.authType == AuthType.PASSWORD) {
                            OutlinedTextField(
                                value = draft.password,
                                onValueChange = { draft = draft.copy(password = it) },
                                label = { Text(if (original == null) "密码" else "密码（留空则保持不变）") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = codexTextFieldColors(),
                                isError = attemptedSave && ConnectionDraftIssue.PASSWORD in validationIssues,
                            )
                        } else {
                            OutlinedTextField(
                                value = draft.privateKey,
                                onValueChange = { draft = draft.copy(privateKey = it) },
                                label = { Text(if (original == null) "OpenSSH / PEM 私钥" else "私钥（留空则保持不变）") },
                                minLines = 5,
                                maxLines = 9,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = codexTextFieldColors(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                isError = attemptedSave && ConnectionDraftIssue.PRIVATE_KEY in validationIssues,
                            )
                            OutlinedTextField(
                                value = draft.passphrase,
                                onValueChange = { draft = draft.copy(passphrase = it) },
                                label = { Text("私钥口令（可选）") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = codexTextFieldColors(),
                            )
                        }
                        CodexSectionHeader("远端系统", hint = "自动识别失败时再手动指定")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().testTag("connection-platform-options"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RemotePlatform.entries.forEach { platform ->
                                CodexChoiceChip(
                                    selected = draft.platform == platform,
                                    onClick = { draft = draft.copy(platform = platform) },
                                    label = platform.displayName,
                                )
                            }
                        }
                        if (draft.hostKeyFingerprint.isNotBlank()) {
                            CodexSectionHeader("主机密钥", hint = "指纹变化时会阻止连接")
                            OutlinedTextField(
                                value = draft.hostKeyFingerprint,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("已固定指纹") },
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = codexTextFieldColors(),
                                trailingIcon = {
                                    TextButton(onClick = {
                                        draft = draft.copy(hostKeyFingerprint = "", clearHostKeyFingerprint = true)
                                    }) { Text("清除") }
                                },
                            )
                        }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ConnectionEditorActions(
    busy: Boolean,
    onSave: () -> Unit,
    onSaveAndConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.testTag("connection-actions"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CodexOutlinedButton(
            onClick = onSave,
            enabled = !busy,
            modifier = Modifier.weight(0.85f).height(48.dp).testTag("connection-action-save"),
        ) { Text("仅保存") }
        CodexButton(
            onClick = onSaveAndConnect,
            enabled = !busy,
            modifier = Modifier.weight(1.15f).height(48.dp).testTag("connection-action-save-connect"),
        ) { Text("保存并连接") }
    }
}

private val ConnectionDraftIssue.message: String
    get() = when (this) {
        ConnectionDraftIssue.CONNECTION_NAME -> "请填写显示名称。"
        ConnectionDraftIssue.HOST -> "请填写 SSH 主机地址。"
        ConnectionDraftIssue.PORT -> "端口必须在 1 到 65535 之间。"
        ConnectionDraftIssue.USERNAME -> "请填写用户名。"
        ConnectionDraftIssue.PASSWORD -> "请填写密码。"
        ConnectionDraftIssue.PRIVATE_KEY -> "请粘贴 OpenSSH 或 PEM 私钥。"
    }

private fun SavedConnection?.toDraft(): ConnectionDraft = if (this == null) ConnectionDraft() else ConnectionDraft(
    id = id,
    name = name,
    host = host,
    port = port.toString(),
    username = username,
    authType = authType,
    hostKeyFingerprint = hostKeyFingerprint,
    platform = platform,
)

private val RemotePlatform.displayName: String
    get() = when (this) {
        RemotePlatform.AUTO -> "自动识别"
        RemotePlatform.POSIX -> "Linux / macOS"
        RemotePlatform.WINDOWS -> "Windows"
    }
