package com.codex.remote

import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codex.remote.domain.AppUiState
import com.codex.remote.ui.screens.ConnectionsScreen
import com.codex.remote.ui.theme.CodexRemoteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectionsDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun compactEditorKeepsActionsAndPlatformChoicesInsideBounds() {
        val state = mutableStateOf(
            AppUiState(
                showConnections = true,
                showConnectionEditor = true,
            ),
        )
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.3f),
            ) {
                CodexRemoteTheme(darkTheme = false) {
                    ConnectionsScreen(
                        state = state.value,
                        onBack = {},
                        onAdd = {},
                        onEdit = {},
                        onDelete = {},
                        onConnect = {},
                        onSave = { _, _ -> },
                        onCloseEditor = {},
                        onDismissNotice = {},
                    )
                }
            }
        }

        val root = composeRule.onNodeWithTag("connection-editor-root")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val navigationBarBottom = ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom
            ?: 0
        val windowSafeBottom = composeRule.activity.window.decorView.height - navigationBarBottom
        assertTrue(
            "编辑页根节点延伸进系统导航栏: 根节点=$root，窗口安全底边=$windowSafeBottom，导航栏=$navigationBarBottom",
            root.bottom <= windowSafeBottom,
        )
        val actionTags = listOf(
            "connection-action-cancel",
            "connection-action-save",
            "connection-action-save-connect",
        )
        actionTags.forEach { tag ->
            val node = composeRule.onNodeWithTag(tag).fetchSemanticsNode()
            val bounds = node.boundsInRoot
            assertTrue(
                "$tag 没有布局: ${node.layoutInfo}，根节点=$root，窗口安全底边=$windowSafeBottom",
                node.layoutInfo.isPlaced,
            )
            assertTrue("$tag 超出安全区顶部: $bounds，根节点=$root", bounds.top >= root.top)
            assertTrue(
                "$tag 越过导航栏: $bounds，根节点=$root，窗口安全底边=$windowSafeBottom",
                bounds.bottom <= root.bottom && bounds.bottom <= windowSafeBottom,
            )
            assertTrue("$tag 超出安全区左侧: $bounds，根节点=$root", bounds.left >= root.left)
            assertTrue("$tag 超出安全区右侧: $bounds，根节点=$root", bounds.right <= root.right)
        }

        composeRule.onNodeWithTag("connection-platform-options").performScrollTo()
        val autoBounds = composeRule.onNodeWithText("自动识别")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val windowsBounds = composeRule.onNodeWithText("Windows")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue("自动识别选项横向越界", autoBounds.left >= root.left && autoBounds.right <= root.right)
        assertTrue("Windows 选项横向越界", windowsBounds.left >= root.left && windowsBounds.right <= root.right)
        assertTrue(
            "Windows 选项仍被压成多行：${windowsBounds.height}dp",
            windowsBounds.height <= autoBounds.height * 1.25f,
        )
    }
}
