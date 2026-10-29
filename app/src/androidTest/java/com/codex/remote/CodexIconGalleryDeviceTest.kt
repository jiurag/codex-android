package com.codex.remote

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codex.remote.ui.icons.CodexIcons
import com.codex.remote.ui.theme.CodexRemoteTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CodexIconGalleryDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun writesCompleteIconGallery() {
        composeRule.setContent {
            CodexRemoteTheme(darkTheme = false) {
                CodexIconGallery()
            }
        }
        composeRule.waitForIdle()

        val bitmap = composeRule.onNodeWithTag("codex-icon-gallery")
            .captureToImage()
            .asAndroidBitmap()
        val outputDirectory = requireNotNull(composeRule.activity.getExternalFilesDir(null))
        val output = File(outputDirectory, "codex-icon-gallery.png")
        FileOutputStream(output).use { stream ->
            assertTrue("图标总览截图写入失败", bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertTrue("图标总览截图为空", output.length() > 0)
    }
}

@Composable
private fun CodexIconGallery() {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .testTag("codex-icon-gallery"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Codex icon language · ${CodexIcons.all.size}", style = MaterialTheme.typography.titleMedium)
            CodexIcons.all.chunked(5).forEach { rowIcons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rowIcons.forEach { (name, icon) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                modifier = Modifier.size(30.dp),
                            )
                            Text(
                                text = name,
                                fontSize = 7.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    repeat(5 - rowIcons.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
