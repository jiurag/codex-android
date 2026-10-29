package com.codex.remote

import android.content.ContentValues
import android.graphics.Typeface
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.codex.remote.data.rpc.CodexRpcClient
import com.codex.remote.data.store.ConnectionStore
import com.codex.remote.domain.AppUiState
import com.codex.remote.domain.AuthType
import com.codex.remote.domain.CcSwitchQuotaSnapshot
import com.codex.remote.domain.CcSwitchRemoteStatus
import com.codex.remote.domain.ComposerImageAttachment
import com.codex.remote.domain.ComposerMention
import com.codex.remote.domain.FileChangeSummary
import com.codex.remote.domain.PermissionMode
import com.codex.remote.domain.ReasoningEffortOption
import com.codex.remote.domain.RemoteAccount
import com.codex.remote.domain.RemoteCollaborationMode
import com.codex.remote.domain.RemoteModel
import com.codex.remote.domain.RemoteMcpServerStatus
import com.codex.remote.domain.RemotePlugin
import com.codex.remote.domain.RemoteProject
import com.codex.remote.domain.RemoteServiceTier
import com.codex.remote.domain.RemoteSkill
import com.codex.remote.domain.RemoteThread
import com.codex.remote.domain.RemoteThreadTokenUsage
import com.codex.remote.domain.ReviewTargetKind
import com.codex.remote.domain.SavedConnection
import com.codex.remote.domain.ThreadGoal
import com.codex.remote.domain.ThreadGoalStatus
import com.codex.remote.domain.TimelineItem
import com.codex.remote.domain.TimelineKind
import com.codex.remote.ui.screens.WorkspaceScreen
import com.codex.remote.ui.theme.CodexRemoteTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class WorkspaceDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun composerMatchesDesktopLayoutAndPlanIsCommandDriven() {
        val state = mutableStateOf(baseState())
        val callbacks = WorkspaceCallbacks()
        show(state, callbacks)

        composeRule.onAllNodesWithText("Default", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Plan", substring = true).assertCountEquals(0)
        composeRule.onNodeWithTag(COMPOSER_MODEL)
            .assertContentDescriptionEquals("模型：5.6-sol · 极高 · 快速")
        composeRule.onNodeWithTag(COMPOSER_CONTEXT).assertContentDescriptionEquals("上下文已使用 64%")

        val add = composeRule.onNodeWithTag(COMPOSER_ADD).fetchSemanticsNode().boundsInRoot
        val permissions = composeRule.onNodeWithTag(COMPOSER_PERMISSIONS).fetchSemanticsNode().boundsInRoot
        val context = composeRule.onNodeWithTag(COMPOSER_CONTEXT).fetchSemanticsNode().boundsInRoot
        val model = composeRule.onNodeWithTag(COMPOSER_MODEL).fetchSemanticsNode().boundsInRoot
        val send = composeRule.onNodeWithTag(COMPOSER_SEND).fetchSemanticsNode().boundsInRoot
        assertTrue(add.center.x < permissions.center.x)
        assertTrue(permissions.center.x < context.center.x)
        assertTrue(context.center.x < model.center.x)
        assertTrue(model.center.x < send.center.x)

        composeRule.onNodeWithTag(COMPOSER_PERMISSIONS).performClick()
        composeRule.onNodeWithText("替我审批").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(PermissionMode.AUTO_REVIEW, callbacks.permissionMode) }

        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("/plan")
        composeRule.onNodeWithTag(COMPOSER_SEND).performClick()
        composeRule.runOnIdle { assertEquals("plan", callbacks.collaborationMode) }
        composeRule.onAllNodesWithText("Default", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Plan mode", substring = true).assertCountEquals(0)
    }

    @Test
    fun connectionLossShowsDisconnectedStateWithoutBlamingCcSwitch() {
        val message = "SSH 连接已断开，请重新连接。"
        val state = mutableStateOf(
            baseState().copy(
                connectionStatus = com.codex.remote.domain.ConnectionStatus.DISCONNECTED,
                connectionMessage = message,
                ccSwitchStatus = null,
                statusError = null,
            ),
        )
        show(state, WorkspaceCallbacks())

        composeRule.onNodeWithText("已断开连接").assertIsDisplayed()
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText("连接管理").assertIsDisplayed()
        composeRule.onAllNodesWithText("CC Switch", substring = true).assertCountEquals(0)
        captureReviewScreenshot("ssh-connection-lost.png")
    }

    @Test
    fun zeroHistoryCanStartNewSessionAndRevealComposer() {
        val base = baseState(threads = emptyList())
        val state = mutableStateOf(
            base.copy(
                projects = emptyList(),
                selectedProjectPath = null,
                selectedThreadId = null,
                timeline = emptyList(),
                connectionMessage = "已导入 0 个项目、0 个会话 · Codex 0.165.0",
            ),
        )
        var newThreadClicks = 0
        show(
            state = state,
            onNewThread = {
                newThreadClicks += 1
                val created = RemoteThread(
                    id = "new-thread",
                    title = "新会话",
                    cwd = "/root",
                    updatedAt = 2,
                    status = "idle",
                )
                // Reproduce thread/started followed by a thread/list refresh that is still empty.
                val visibleAfterRefresh = mergeVisibleThreads(
                    serverThreads = emptyList(),
                    unlistedThreads = listOf(created),
                )
                state.value = state.value.copy(
                    threads = visibleAfterRefresh,
                    projects = listOf(
                        RemoteProject(
                            id = "/root",
                            name = "root",
                            path = "/root",
                            threads = visibleAfterRefresh,
                            updatedAt = 2,
                        ),
                    ),
                    selectedProjectPath = "/root",
                    selectedThreadId = created.id,
                    connectionMessage = "已导入 1 个项目、1 个会话 · Codex 0.165.0",
                )
            },
        )

        composeRule.onNodeWithContentDescription("打开会话").performClick()
        composeRule.onNodeWithText("新建会话").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, newThreadClicks) }
        composeRule.onNodeWithTag(COMPOSER_INPUT).assertIsDisplayed()
        captureReviewScreenshot("zero-history-new-session-composer.png")
    }

    @Test
    fun longModelNameKeepsNarrowLargeTextComposerInsideItsSurface() {
        val base = baseState()
        val longModel = base.models.single().copy(
            id = "deepseek-v4-flash",
            displayName = "DeepSeek V4 Flash",
        )
        val state = mutableStateOf(
            base.copy(
                models = listOf(longModel),
                selectedModel = longModel.id,
                selectedReasoningEffort = "high",
                selectedServiceTier = null,
            ),
        )
        show(state)

        assertComposerControlsInsideSurface()
        captureReviewScreenshot("composer-narrow-320-font-1.3.png")
    }

    @Test
    fun longModelNameKeepsLargeTextPhoneComposerInsideItsSurface() {
        val base = baseState()
        val longModel = base.models.single().copy(
            id = "deepseek-v4-flash",
            displayName = "DeepSeek V4 Flash",
        )
        val state = mutableStateOf(
            base.copy(
                models = listOf(longModel),
                selectedModel = longModel.id,
                selectedReasoningEffort = "high",
                selectedServiceTier = null,
            ),
        )
        show(state, contentWidth = 411.dp, fontScale = 1.3f)

        assertComposerControlsInsideSurface()
        captureReviewScreenshot("composer-phone-411-font-1.3.png")
    }

    @Test
    fun iconOnlyComposerUsesOneRowAtCommonPhoneWidth() {
        val base = baseState()
        val longModel = base.models.single().copy(
            id = "deepseek-v4-flash",
            displayName = "DeepSeek V4 Flash",
        )
        val state = mutableStateOf(
            base.copy(
                models = listOf(longModel),
                selectedModel = longModel.id,
                selectedReasoningEffort = "high",
                selectedServiceTier = null,
            ),
        )
        show(state, contentWidth = 360.dp, fontScale = 1f)

        composeRule.onNodeWithText("已连接").assertIsDisplayed()
        composeRule.onAllNodesWithText("DeepSeek V4 Flash").assertCountEquals(0)
        assertComposerControlsInsideSurface()
        val add = composeRule.onNodeWithTag(COMPOSER_ADD).fetchSemanticsNode().boundsInRoot
        val context = composeRule.onNodeWithTag(COMPOSER_CONTEXT).fetchSemanticsNode().boundsInRoot
        val model = composeRule.onNodeWithTag(COMPOSER_MODEL).fetchSemanticsNode().boundsInRoot
        val send = composeRule.onNodeWithTag(COMPOSER_SEND).fetchSemanticsNode().boundsInRoot
        val rowCenters = listOf(add.center.y, context.center.y, model.center.y, send.center.y)
        assertTrue(
            "图标操作区没有保持单行",
            rowCenters.maxOrNull()!! - rowCenters.minOrNull()!! <= 1f,
        )
        composeRule.onAllNodesWithText("deepseek-v4-flash", substring = true).assertCountEquals(0)
        captureReviewScreenshot("composer-phone-360-icon-only.png")
    }

    private fun assertComposerControlsInsideSurface() {
        val surface = composeRule.onNodeWithTag(COMPOSER_SURFACE).fetchSemanticsNode().boundsInRoot
        val add = composeRule.onNodeWithTag(COMPOSER_ADD).fetchSemanticsNode().boundsInRoot
        val permissions = composeRule.onNodeWithTag(COMPOSER_PERMISSIONS).fetchSemanticsNode().boundsInRoot
        val context = composeRule.onNodeWithTag(COMPOSER_CONTEXT).fetchSemanticsNode().boundsInRoot
        val model = composeRule.onNodeWithTag(COMPOSER_MODEL).fetchSemanticsNode().boundsInRoot
        val send = composeRule.onNodeWithTag(COMPOSER_SEND).fetchSemanticsNode().boundsInRoot
        val minTouchPx = 48f * composeRule.activity.resources.displayMetrics.density

        assertTrue("发送按钮超出输入框右边界", send.right <= surface.right)
        assertFalse(
            "模型按钮与发送按钮重叠",
            model.left < send.right && send.left < model.right &&
                model.top < send.bottom && send.top < model.bottom,
        )
        assertFalse(
            "上下文按钮与发送按钮重叠",
            context.left < send.right && send.left < context.right &&
                context.top < send.bottom && send.top < context.bottom,
        )
        assertTrue("添加按钮触控区不足 48dp", add.width + 0.5f >= minTouchPx)
        assertTrue("权限按钮触控区不足 48dp", permissions.height + 0.5f >= minTouchPx)
        assertTrue("上下文按钮触控区不足 48dp", context.width + 0.5f >= minTouchPx)
        assertTrue("模型按钮触控区不足 48dp", model.width + 0.5f >= minTouchPx)
        assertTrue("发送按钮触控区不足 48dp", send.width + 0.5f >= minTouchPx)
        assertTrue("发送按钮底部超出输入框", send.bottom <= surface.bottom)
    }

    private fun captureReviewScreenshot(fileName: String) {
        val contextActivity = InstrumentationRegistry.getInstrumentation().targetContext
        val pendingImage = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/CodexReview",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val screenshotUri = contextActivity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            pendingImage,
        )
        assertNotNull("Composer 截图条目未创建", screenshotUri)
        contextActivity.contentResolver.openOutputStream(requireNotNull(screenshotUri)).use { output ->
            assertNotNull("Composer 截图输出流未创建", output)
            composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, requireNotNull(output))
        }
        contextActivity.contentResolver.update(
            requireNotNull(screenshotUri),
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null,
        )
    }

    @Test
    fun remoteStatusUsesCcSwitchQuotaInsteadOfCodexAccountAuthentication() {
        val state = mutableStateOf(
            baseState().copy(
                ccSwitchStatus = CcSwitchRemoteStatus(
                    providerId = "deepseek",
                    providerName = "DeepSeek",
                    status = "ok",
                    available = true,
                    queriedAt = 1_793_253_980_715,
                    quotas = listOf(
                        CcSwitchQuotaSnapshot(
                            planName = "CNY",
                            remaining = "123.45",
                            unit = "CNY",
                            isValid = true,
                        ),
                    ),
                    error = null,
                ),
            ),
        )
        show(state, contentWidth = 320.dp, fontScale = 1.3f)

        composeRule.onNodeWithTag(COMPOSER_CONTEXT).performClick()

        composeRule.onNodeWithText("数据源").assertIsDisplayed()
        composeRule.onNodeWithText("CC Switch").assertIsDisplayed()
        composeRule.onNodeWithText("DeepSeek").assertIsDisplayed()
        composeRule.onNodeWithText("剩余 123.45 CNY").assertIsDisplayed()
        composeRule.onAllNodesWithText("SSH 认证失败", substring = true).assertCountEquals(0)
    }

    @Test
    fun composerRingShowsLiveCcSwitchBalanceWithoutReplacingContextState() {
        val state = mutableStateOf(
            baseState().copy(
                ccSwitchStatus = CcSwitchRemoteStatus(
                    providerId = "deepseek",
                    providerName = "DeepSeek",
                    status = "ok",
                    available = true,
                    queriedAt = 1_793_253_980_715,
                    quotas = listOf(
                        CcSwitchQuotaSnapshot(
                            planName = "CNY",
                            remaining = "123.45",
                            unit = "CNY",
                            isValid = true,
                        ),
                    ),
                    error = null,
                ),
            ),
        )
        show(state, contentWidth = 320.dp, fontScale = 1.3f)

        composeRule.onNodeWithTag(COMPOSER_CONTEXT)
            .assertContentDescriptionEquals("余额：123 · 上下文已使用 64%")
        composeRule.onNodeWithTag("composer-balance", useUnmergedTree = true)
            .assertTextEquals("123")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("¥").assertCountEquals(0)
        composeRule.onAllNodesWithText(".54", substring = true).assertCountEquals(0)
        assertComposerControlsInsideSurface()
        captureReviewScreenshot("composer-cswitch-balance-320-font-1.3.png")
    }

    @Test
    fun automaticApprovalNoticeIsDisplayedInSimplifiedChinese() {
        val state = mutableStateOf(
            baseState().copy(
                notice = "Automatic approval review approved (risk: low, authorization: unknown): " +
                    "Auto-review returned a low-risk allow decision.",
            ),
        )
        show(state)

        composeRule.onNodeWithText(
            "自动审批：已批准本次操作（风险：低；授权状态：未知）。" +
                "审查程序判定为低风险，允许继续。",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            "Automatic approval review approved",
            substring = true,
        ).assertCountEquals(0)
        composeRule.onAllNodesWithText(
            "Auto-review returned",
            substring = true,
        ).assertCountEquals(0)
    }

    @Test
    fun remoteModelMetadataAndMcpCountsAreDisplayedInSimplifiedChinese() {
        val base = baseState()
        val model = base.models.single().copy(
            description = "Provider-specific detail",
            supportedReasoningEfforts = listOf(ReasoningEffortOption("medium", "Balanced")),
            serviceTiers = listOf(RemoteServiceTier("fast", "Fast", "Priority processing")),
        )
        val state = mutableStateOf(
            base.copy(
                models = listOf(model),
                selectedReasoningEffort = "medium",
                mcpServers = listOf(
                    RemoteMcpServerStatus(
                        name = "qa-remote",
                        authStatus = "unsupported",
                        toolCount = 1,
                        resourceCount = 2,
                    ),
                ),
            ),
        )
        var mcpLoaded = false
        show(state, onLoadMcpStatus = { mcpLoaded = true })

        composeRule.onNodeWithTag(COMPOSER_MODEL).performClick()
        composeRule.onNodeWithText("模型").performClick()
        composeRule.onNodeWithText("详细说明由远端服务提供").assertIsDisplayed()
        composeRule.onAllNodesWithText("Provider-specific detail").assertCountEquals(0)

        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("/mcp")
        composeRule.onNodeWithTag("composer-suggestion-/mcp").performClick()
        composeRule.runOnIdle { assertTrue(mcpLoaded) }
        composeRule.onNodeWithText("1 个工具 · 2 个资源").assertIsDisplayed()
        composeRule.onAllNodesWithText("tools", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("resources", substring = true).assertCountEquals(0)
    }

    @Test
    fun completedTurnCollapsesReasoningCommandsToolsAndFileChangesLive() {
        val runningTimeline = listOf(
            TimelineItem("reasoning", TimelineKind.REASONING, title = "Reasoning", body = "private reasoning", status = "inProgress"),
            TimelineItem("cmd-1", TimelineKind.COMMAND, title = "pwd", body = "/workspace/demo", status = "running"),
            TimelineItem("cmd-2", TimelineKind.COMMAND, title = "git status", body = "clean", status = "running"),
            TimelineItem("tool", TimelineKind.TOOL, title = "Remote tool", body = "tool details", status = "started"),
            TimelineItem(
                "files",
                TimelineKind.FILE_CHANGE,
                status = "inProgress",
                fileChanges = listOf(
                    FileChangeSummary(
                        "app/src/Main.kt",
                        "update",
                        "--- a/app/src/Main.kt\n+++ b/app/src/Main.kt\n-old\n+val ready = true",
                    ),
                ),
            ),
        )
        val state = mutableStateOf(baseState(timeline = runningTimeline, isTurnRunning = true))
        show(state)

        scrollTo("timeline-tool-body-reasoning")
        composeRule.onNodeWithText("private reasoning").assertIsDisplayed()
        scrollTo("timeline-tool-body-command-group:cmd-1")
        composeRule.onNodeWithText("运行了多个命令").assertExists()
        composeRule.onNodeWithText("/workspace/demo", substring = true).assertExists()
        scrollTo("timeline-tool-body-tool")
        composeRule.onNodeWithText("tool details").assertIsDisplayed()
        scrollTo("file-changes-files")
        composeRule.onNodeWithText("+val ready = true").assertExists()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                isTurnRunning = false,
                activeTurnId = null,
                timeline = runningTimeline.map { it.copy(status = "completed") },
            )
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("timeline-tool-body-reasoning").fetchSemanticsNodes().isEmpty()
        }

        scrollTo("timeline-tool-reasoning")
        composeRule.onNodeWithText("private reasoning").assertDoesNotExist()
        scrollTo("timeline-tool-command-group:cmd-1")
        composeRule.onNodeWithText("/workspace/demo", substring = true).assertDoesNotExist()
        scrollTo("timeline-tool-tool")
        composeRule.onNodeWithText("tool details").assertDoesNotExist()
        scrollTo("file-changes-files")
        composeRule.onNodeWithText("+val ready = true").assertDoesNotExist()
    }

    @Test
    fun markdownBoldCodeAndLatexRenderAsAndroidSpans() {
        val markdown = """## Rendered heading

**bold-rendered** and inline ${'$'}x^2${'$'}.

```kotlin
val answer = 42
```
""".trimIndent()
        show(mutableStateOf(baseState(timeline = listOf(TimelineItem("markdown", TimelineKind.AGENT, body = markdown)))))
        composeRule.onNodeWithTag("timeline-agent-markdown").assertExists()

        lateinit var rendered: Spanned
        composeRule.runOnIdle {
            val textView = composeRule.activity.window.decorView.descendantTextViews()
                .firstOrNull { it.text.toString().contains("bold-rendered") }
            assertNotNull(textView)
            rendered = textView!!.text as Spanned
        }
        val plainText = rendered.toString()
        val spans = rendered.getSpans(0, rendered.length, Any::class.java)
        assertFalse(plainText.contains("**"))
        assertFalse(plainText.contains("```"))
        assertTrue(plainText.contains("val answer = 42"))
        assertTrue(spans.any { span ->
            (span is StyleSpan && span.style and Typeface.BOLD != 0) ||
                span.javaClass.name.contains("StrongEmphasis", ignoreCase = true)
        })
        assertTrue(spans.any { span ->
            span.javaClass.name.contains("latex", ignoreCase = true) ||
                span.javaClass.name.contains("drawable", ignoreCase = true)
        })
    }

    @Test
    fun bottomButtonReachesSentinelAfterLongAgentResponse() {
        val longAnswer = (1..80).joinToString("\n\n") { "Rendered answer paragraph $it" }
        val timeline = buildList {
            repeat(20) { index -> add(TimelineItem("user-$index", TimelineKind.USER, body = "Question $index")) }
            add(TimelineItem("last-answer", TimelineKind.AGENT, body = longAnswer))
        }
        show(mutableStateOf(baseState(timeline = timeline)))

        composeRule.onNodeWithTag(CONVERSATION_BOTTOM).assertIsDisplayed()
        composeRule.onNodeWithTag(CONVERSATION_LIST).performTouchInput { swipeDown() }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(CONVERSATION_BOTTOM_BUTTON).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(CONVERSATION_BOTTOM_BUTTON).assertIsDisplayed().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(CONVERSATION_BOTTOM_BUTTON).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(CONVERSATION_BOTTOM).assertIsDisplayed()
    }

    @Test
    fun loadingFiveOlderTurnsKeepsTheVisibleMessageAnchored() {
        val current = (0 until 10).flatMap { index ->
            listOf(
                TimelineItem("new-user-$index", TimelineKind.USER, body = "Current question $index"),
                TimelineItem("new-agent-$index", TimelineKind.AGENT, body = "Current answer $index"),
            )
        }
        val older = (0 until 5).flatMap { index ->
            listOf(
                TimelineItem("old-user-$index", TimelineKind.USER, body = "Older question $index"),
                TimelineItem("old-agent-$index", TimelineKind.AGENT, body = "Older answer $index"),
            )
        }
        val state = mutableStateOf(baseState(timeline = current).copy(hasOlderHistory = true, olderHistoryCursor = "page-2"))
        val callbacks = WorkspaceCallbacks()
        callbacks.onLoadOlder = {
            callbacks.olderLoads++
            state.value = state.value.copy(isOlderHistoryLoading = true)
        }
        show(state, callbacks)

        repeat(12) {
            if (callbacks.olderLoads > 0) return@repeat
            composeRule.onNodeWithTag(CONVERSATION_LIST).performTouchInput { swipeDown() }
            composeRule.waitForIdle()
        }
        assertEquals(1, callbacks.olderLoads)
        val before = composeRule.onNodeWithTag("timeline-item-new-agent-0").fetchSemanticsNode().boundsInRoot.top
        composeRule.runOnIdle {
            state.value = state.value.copy(
                timeline = older + current,
                isOlderHistoryLoading = false,
                hasOlderHistory = false,
                olderHistoryCursor = null,
            )
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("timeline-item-new-agent-0").fetchSemanticsNodes().isNotEmpty()
        }
        val after = composeRule.onNodeWithTag("timeline-item-new-agent-0").fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            "History anchor moved from $before to $after (${abs(after - before)} px)",
            abs(after - before) <= 4f,
        )
        composeRule.onNodeWithTag(CONVERSATION_LIST).performScrollToNode(hasTestTag("timeline-item-old-user-0"))
        composeRule.onNodeWithText("Older question 0").assertIsDisplayed()
    }

    @Test
    fun switchingThreadsClearsDraftAndNeverShowsTheOtherTimeline() {
        val threadA = thread("thread-a", "Thread A")
        val threadB = thread("thread-b", "Thread B")
        val state = mutableStateOf(
            baseState(
                timeline = listOf(TimelineItem("a-message", TimelineKind.USER, body = "thread-a-body")),
                threads = listOf(threadA, threadB),
            ),
        )
        show(state)
        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("draft-a")
        composeRule.onNodeWithTag(COMPOSER_ADD).performClick()
        composeRule.onNodeWithText("目标").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(COMPOSER_GOAL_MARKER).assertExists()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                selectedThreadId = "thread-b",
                timeline = (0 until 30).map { index ->
                    TimelineItem("b-message-$index", TimelineKind.USER, body = "thread-b-body-$index")
                },
            )
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(CONVERSATION_BOTTOM).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("thread-b-body-29").assertExists()
        composeRule.onNodeWithText("thread-a-body").assertDoesNotExist()
        composeRule.onAllNodesWithTag(CONVERSATION_BOTTOM_BUTTON).assertCountEquals(0)
        composeRule.onAllNodesWithTag(COMPOSER_GOAL_MARKER).assertCountEquals(0)
        assertFalse(composerText().contains("draft-a"))
        composeRule.runOnIdle {
            assertTrue(state.value.acceptsThreadEvent("thread-b"))
            assertFalse(state.value.acceptsThreadEvent("thread-a"))
            assertFalse(state.value.acceptsThreadEvent(null))
        }
    }

    @Test
    fun addMenuUsesDismissibleGoalMarkerAndStillProvidesPlanSkillsAndPlugins() {
        val state = mutableStateOf(
            baseState().copy(
                skills = listOf(RemoteSkill("qa-check", "QA Skill", "Device test skill", "/skills/qa/SKILL.md", true, setOf("/workspace/demo"))),
                plugins = listOf(RemotePlugin("qa@local", "qa-plugin", "QA Plugin", "Device test plugin", "local", "plugin://qa@local", true)),
            ),
        )
        val callbacks = WorkspaceCallbacks()
        show(state, callbacks)

        composeRule.onNodeWithTag(COMPOSER_ADD).performClick()
        composeRule.onNodeWithText("目标").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("Unsupported method", substring = true).assertCountEquals(0)
        composeRule.onNodeWithTag(COMPOSER_GOAL_MARKER).assertIsDisplayed().performClick()
        composeRule.onAllNodesWithTag(COMPOSER_GOAL_MARKER).assertCountEquals(0)

        composeRule.onNodeWithTag(COMPOSER_ADD).performClick()
        composeRule.onNodeWithText("目标").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("ship-device-tests")
        composeRule.onNodeWithTag(COMPOSER_SEND).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("ship-device-tests" to true), callbacks.sentMessages)
        }
        composeRule.onAllNodesWithTag(COMPOSER_GOAL_MARKER).assertCountEquals(0)

        composeRule.onNodeWithTag(COMPOSER_ADD).performClick()
        composeRule.onNodeWithText("目标").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(COMPOSER_ADD).performClick()
        composeRule.onNodeWithText("计划模式").assertIsDisplayed().performClick()
        assertEquals("plan", callbacks.collaborationMode)
        composeRule.onAllNodesWithTag(COMPOSER_GOAL_MARKER).assertCountEquals(0)

        composeRule.onNodeWithTag(COMPOSER_ADD).performClick()
        composeRule.onNodeWithText("QA Skill").performScrollTo().performClick()
        assertTrue(composerText().contains("${'$'}qa-check"))
        composeRule.onNodeWithTag(COMPOSER_ADD).performClick()
        composeRule.onNodeWithText("QA Plugin").performScrollTo().performClick()
        assertTrue(composerText().contains("${'$'}qa-plugin"))
    }

    @Test
    fun goalSlashCommandActivatesTheSameMarkerAndOnlyMarksTheNextSend() {
        val state = mutableStateOf(baseState())
        val callbacks = WorkspaceCallbacks()
        show(state, callbacks)

        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("/goal")
        composeRule.onNodeWithTag(COMPOSER_SEND).performClick()
        composeRule.onNodeWithTag(COMPOSER_GOAL_MARKER).assertIsDisplayed()
        composeRule.runOnIdle { assertTrue(callbacks.sentMessages.isEmpty()) }

        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("finish-device-coverage")
        composeRule.onNodeWithTag(COMPOSER_SEND).performClick()
        composeRule.onAllNodesWithTag(COMPOSER_GOAL_MARKER).assertCountEquals(0)
        assertEquals("", composerText())

        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("normal-follow-up")
        composeRule.onNodeWithTag(COMPOSER_SEND).performClick()
        assertEquals("", composerText())
        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("/goal inline-objective")
        composeRule.onNodeWithTag(COMPOSER_SEND).performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    "finish-device-coverage" to true,
                    "normal-follow-up" to false,
                    "inline-objective" to true,
                ),
                callbacks.sentMessages,
            )
        }
    }

    @Test
    fun goalMarkerWorksBeforeTheRemoteThreadExists() {
        val state = mutableStateOf(baseState(threads = emptyList()).copy(selectedThreadId = null))
        val callbacks = WorkspaceCallbacks()
        show(state, callbacks)

        composeRule.onNodeWithTag(COMPOSER_ADD).performClick()
        composeRule.onNodeWithText("目标").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(COMPOSER_GOAL_MARKER).assertIsDisplayed()
        composeRule.onNodeWithTag(COMPOSER_INPUT).performTextInput("create-thread-with-goal")
        composeRule.onNodeWithTag(COMPOSER_SEND).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("create-thread-with-goal" to true), callbacks.sentMessages)
        }
        composeRule.onAllNodesWithText("请先发送第一条消息", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag(COMPOSER_GOAL_MARKER).assertCountEquals(0)
    }

    @Test
    fun goalMessagesAreVisiblyDistinguishedFromNormalMessages() {
        val state = mutableStateOf(
            baseState(
                timeline = listOf(
                    TimelineItem("goal-message", TimelineKind.USER, body = "ship it", isGoal = true),
                    TimelineItem("normal-message", TimelineKind.USER, body = "status update"),
                ),
            ),
        )
        show(state)

        composeRule.onNodeWithTag("timeline-goal-goal-message").assertExists()
        composeRule.onNodeWithText("作为目标发送").assertExists()
        composeRule.onAllNodesWithTag("timeline-goal-normal-message").assertCountEquals(0)
    }

    @Test
    fun completedFileChangesUseStructuredExpandablePresentationAndNoChangesTab() {
        val files = TimelineItem(
            id = "files",
            kind = TimelineKind.FILE_CHANGE,
            status = "completed",
            fileChanges = listOf(
                FileChangeSummary("app/src/Main.kt", "update", "--- a/app/src/Main.kt\n+++ b/app/src/Main.kt\n-old\n+new\n+next"),
                FileChangeSummary("app/src/Status.kt", "add", "--- /dev/null\n+++ b/app/src/Status.kt\n+val ready = true"),
            ),
        )
        show(mutableStateOf(baseState(timeline = listOf(files))))

        composeRule.onNodeWithText("已编辑 2 个文件").assertIsDisplayed()
        composeRule.onNodeWithText("--- a/app/src/Main.kt").assertDoesNotExist()
        composeRule.onNodeWithText("已编辑 2 个文件").performClick()
        composeRule.onNodeWithText("app/src/Main.kt").assertExists()
        composeRule.onNodeWithText("app/src/Status.kt").assertExists()
        composeRule.onNodeWithText("修改").assertExists()
        composeRule.onNodeWithText("新增").assertExists()
        composeRule.onNodeWithText("+2  -1").assertExists()
        composeRule.onAllNodesWithText("Changes").assertCountEquals(0)
        composeRule.onAllNodesWithText("{\"changes\"", substring = true).assertCountEquals(0)
    }

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag(CONVERSATION_LIST).performScrollToNode(hasTestTag(tag))
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun composerText(): String = composeRule.onNodeWithTag(COMPOSER_INPUT)
        .fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun show(
        state: MutableState<AppUiState>,
        callbacks: WorkspaceCallbacks = WorkspaceCallbacks(),
        contentWidth: Dp? = null,
        fontScale: Float = 1f,
        onNewThread: () -> Unit = {},
        onLoadMcpStatus: () -> Unit = {},
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = fontScale),
            ) {
                CodexRemoteTheme(darkTheme = false) {
                    val screen: @androidx.compose.runtime.Composable () -> Unit = {
                        WorkspaceScreen(
                            state = state.value,
                            onOpenConnections = {},
                            onNewThread = onNewThread,
                            onSelectProject = {},
                            onSelectThread = {},
                            onLoadOlderHistory = callbacks.onLoadOlder,
                            onRenameThread = { _, _ -> },
                            onArchiveThread = {},
                            onLoadArchivedThreads = {},
                            onUnarchiveThread = {},
                            onDeleteArchivedThread = {},
                            onSetThreadPinned = { _, _ -> },
                            onSend = callbacks.onSend,
                            onStop = {},
                            onCompactThread = {},
                            onForkThread = {},
                            onStartReview = { _, _ -> },
                            onRunInit = {},
                            onLoadMcpStatus = onLoadMcpStatus,
                            onReloadMcpServers = {},
                            onStartMcpLogin = {},
                            onMcpAuthorizationHandled = {},
                            onSubmitFeedback = { _, _ -> },
                            onSetGoal = callbacks.onSetGoal,
                            onSetGoalStatus = {},
                            onClearGoal = {},
                            onShowStatus = {},
                            onSetModel = {},
                            onSetReasoningEffort = {},
                            onSetServiceTier = {},
                            onSetCollaborationMode = { callbacks.collaborationMode = it },
                            onSetPermissionProfile = {},
                            onSetPermissionMode = { callbacks.permissionMode = it },
                            onLoadRemoteDirectory = {},
                            onClearRemoteDirectory = {},
                            onStartLogin = {},
                            onCancelLogin = {},
                            onApproval = { _, _ -> },
                            onTrustHostKey = {},
                            onRejectHostKey = {},
                            onDismissNotice = {},
                        )
                    }
                    if (contentWidth == null) {
                        screen()
                    } else {
                        Box(Modifier.width(contentWidth).fillMaxHeight()) {
                            screen()
                        }
                    }
                }
            }
        }
    }
}

@RunWith(AndroidJUnit4::class)
class RemoteStateDeviceTest {
    @Test
    fun goalAndPlanParamsMatchTheOfficialAppServerProtocol() {
        val goalParams = CodexRpcClient.threadGoalSetParams(
            threadId = "thread-a",
            objective = "ship",
            status = ThreadGoalStatus.ACTIVE,
        )
        assertEquals("thread-a", goalParams["threadId"]?.jsonPrimitive?.content)
        assertEquals("ship", goalParams["objective"]?.jsonPrimitive?.content)
        assertEquals("active", goalParams["status"]?.jsonPrimitive?.content)

        val turnParams = CodexRpcClient.turnStartParams(
            threadId = "thread-a",
            text = "plan request",
            cwd = "/workspace/demo",
            model = "gpt-5.6-sol",
            reasoningEffort = "ultra",
            serviceTier = "fast",
            approvalPolicy = "on-request",
            collaborationMode = RemoteCollaborationMode("Plan", "plan", "gpt-5.6-sol", "ultra"),
            mentions = emptyList(),
        )
        assertEquals("plan", turnParams["collaborationMode"]?.jsonObject?.get("mode")?.jsonPrimitive?.content)
        assertEquals("gpt-5.6-sol", turnParams["model"]?.jsonPrimitive?.content)
        assertEquals("ultra", turnParams["effort"]?.jsonPrimitive?.content)
        assertEquals("fast", turnParams["serviceTier"]?.jsonPrimitive?.content)
    }

    @Test
    fun lastUsedServerPersistsAndIsSelectedForAutomaticRestore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ConnectionStore(context)
        val suffix = UUID.randomUUID().toString()
        val first = store.save(
            com.codex.remote.domain.ConnectionDraft(
                name = "device-first-$suffix",
                host = "127.0.0.1",
                port = "22",
                username = "tester",
                password = "secret",
            ),
        )
        val second = store.save(
            com.codex.remote.domain.ConnectionDraft(
                name = "device-second-$suffix",
                host = "127.0.0.2",
                port = "22",
                username = "tester",
                password = "secret",
            ),
        )
        try {
            store.recordUsed(first.id)
            val persisted = withTimeout(5_000) {
                store.connections.first { connections ->
                    connections.any { it.id == first.id && it.lastUsedAt > 0 } &&
                        connections.any { it.id == second.id }
                }
            }
            assertEquals(first.id, persisted.lastUsedConnectionOrNull()?.id)
        } finally {
            store.delete(first.id)
            store.delete(second.id)
        }
    }
}

private class WorkspaceCallbacks {
    var collaborationMode: String? = null
    var permissionMode: PermissionMode? = null
    val sentMessages = mutableListOf<Pair<String, Boolean>>()
    var olderLoads: Int = 0
    var onLoadOlder: () -> Unit = {}
    var onSetGoal: (String) -> Unit = {}
    var onSend: (String, List<ComposerMention>, List<ComposerImageAttachment>, Boolean) -> Unit =
        { text, _, _, asGoal -> sentMessages += text.trim() to asGoal }
}

private fun baseState(
    timeline: List<TimelineItem> = emptyList(),
    isTurnRunning: Boolean = false,
    threads: List<RemoteThread> = listOf(thread("thread-a", "Thread A")),
): AppUiState {
    val connection = SavedConnection(
        id = "connection",
        name = "QA Host",
        host = "10.0.2.2",
        username = "tester",
        authType = AuthType.PASSWORD,
        encryptedPassword = "encrypted",
        lastUsedAt = 1,
    )
    val project = RemoteProject(
        id = "/workspace/demo",
        name = "demo",
        path = "/workspace/demo",
        threads = threads,
        updatedAt = 1,
    )
    val model = RemoteModel(
        id = "gpt-5.6-sol",
        displayName = "GPT-5.6-Sol",
        description = "Device test model",
        isDefault = true,
        supportedReasoningEfforts = listOf(ReasoningEffortOption("ultra", "Maximum")),
        defaultReasoningEffort = "ultra",
        inputModalities = setOf("text", "image"),
        serviceTiers = listOf(RemoteServiceTier("fast", "Fast", "Priority processing")),
        defaultServiceTier = "fast",
    )
    return AppUiState(
        savedConnections = listOf(connection),
        activeConnection = connection,
        connectionStatus = com.codex.remote.domain.ConnectionStatus.CONNECTED,
        threads = threads,
        projects = listOf(project),
        selectedProjectPath = project.path,
        selectedThreadId = threads.firstOrNull()?.id,
        timeline = timeline,
        models = listOf(model),
        selectedModel = model.id,
        selectedReasoningEffort = "ultra",
        selectedServiceTier = "fast",
        collaborationModes = listOf(
            RemoteCollaborationMode("Default", "default"),
            RemoteCollaborationMode("Plan", "plan", model.id, "ultra"),
        ),
        selectedCollaborationMode = "default",
        selectedPermissionProfile = ":workspace",
        approvalPolicy = "on-request",
        approvalsReviewer = "user",
        remoteAccount = RemoteAccount("chatgpt", "qa@example.invalid", "plus", false),
        threadTokenUsage = RemoteThreadTokenUsage(128_000, 120_000, 8_000, 200_000),
        isTurnRunning = isTurnRunning,
        activeTurnId = if (isTurnRunning) "turn-a" else null,
        isRestoringLastConnection = false,
    )
}

private fun thread(id: String, title: String) = RemoteThread(
    id = id,
    title = title,
    cwd = "/workspace/demo",
    updatedAt = 1,
    status = "idle",
)

private fun goal(objective: String) = ThreadGoal(
    threadId = "thread-a",
    objective = objective,
    status = ThreadGoalStatus.ACTIVE,
    tokenBudget = null,
    tokensUsed = 0,
    timeUsedSeconds = 0,
    createdAt = 1,
    updatedAt = 1,
)

private fun View.descendantTextViews(): List<TextView> = buildList {
    if (this@descendantTextViews is TextView) add(this@descendantTextViews)
    if (this@descendantTextViews is ViewGroup) {
        repeat(childCount) { childIndex -> addAll(getChildAt(childIndex).descendantTextViews()) }
    }
}

private const val CONVERSATION_LIST = "conversation-list"
private const val CONVERSATION_BOTTOM = "conversation-bottom"
private const val CONVERSATION_BOTTOM_BUTTON = "conversation-bottom-button"
private const val COMPOSER_INPUT = "composer-input"
private const val COMPOSER_ADD = "composer-add"
private const val COMPOSER_PERMISSIONS = "composer-permissions"
private const val COMPOSER_CONTEXT = "composer-context"
private const val COMPOSER_MODEL = "composer-model"
private const val COMPOSER_SEND = "composer-send"
private const val COMPOSER_SURFACE = "composer-surface"
private const val COMPOSER_GOAL_MARKER = "composer-goal-marker"
