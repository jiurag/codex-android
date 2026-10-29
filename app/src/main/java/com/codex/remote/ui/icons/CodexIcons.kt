package com.codex.remote.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The app's complete icon language.
 *
 * Every glyph is drawn on the same 24dp grid with a 1.8dp round stroke,
 * generous negative space, and explicit connection points. The mark mirrors
 * Codex's orbital identity while keeping Android action icons immediately legible.
 */
object CodexIcons {
    val ArrowBack = icon("CodexArrowBack", autoMirror = true) {
        line {
            moveTo(19.5f, 12f)
            lineTo(5f, 12f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    val CallSplit = icon("CodexCallSplit") {
        line {
            circle(6f, 5f, 1.7f)
            circle(6f, 19f, 1.7f)
            circle(18f, 5f, 1.7f)
            moveTo(6f, 6.7f)
            lineTo(6f, 17.3f)
            moveTo(6f, 12f)
            curveTo(11f, 12f, 11f, 6.7f, 16.3f, 6.7f)
        }
    }

    val List = icon("CodexList", autoMirror = true) {
        fill {
            circle(5f, 7f, 1.15f)
            circle(5f, 12f, 1.15f)
            circle(5f, 17f, 1.15f)
        }
        line {
            moveTo(8.5f, 7f)
            lineTo(19.5f, 7f)
            moveTo(8.5f, 12f)
            lineTo(19.5f, 12f)
            moveTo(8.5f, 17f)
            lineTo(19.5f, 17f)
        }
    }

    val Send = icon("CodexSend", autoMirror = true) {
        line {
            moveTo(3.8f, 5.1f)
            lineTo(20.2f, 12f)
            lineTo(3.8f, 18.9f)
            lineTo(6.8f, 12f)
            close()
            moveTo(6.8f, 12f)
            lineTo(20.2f, 12f)
        }
    }

    val Add = icon("CodexAdd") {
        line {
            moveTo(12f, 4.5f)
            lineTo(12f, 19.5f)
            moveTo(4.5f, 12f)
            lineTo(19.5f, 12f)
        }
    }

    val Archive = icon("CodexArchive") {
        line {
            roundRect(3.5f, 4.5f, 20.5f, 8.5f, 1.3f)
            moveTo(5.2f, 8.5f)
            lineTo(5.2f, 18.3f)
            quadTo(5.2f, 20f, 6.9f, 20f)
            lineTo(17.1f, 20f)
            quadTo(18.8f, 20f, 18.8f, 18.3f)
            lineTo(18.8f, 8.5f)
            moveTo(9f, 12f)
            lineTo(15f, 12f)
        }
    }

    val Build = icon("CodexBuild") {
        line {
            moveTo(14.2f, 4.2f)
            curveTo(11.7f, 4.4f, 10.1f, 6.2f, 10.2f, 8.6f)
            curveTo(10.3f, 10.1f, 11f, 11.2f, 12.1f, 11.9f)
            lineTo(5f, 19f)
            lineTo(6.8f, 20.8f)
            lineTo(13.9f, 13.7f)
            curveTo(16f, 14.9f, 18.8f, 13.9f, 19.7f, 11.7f)
            curveTo(20.4f, 10.1f, 20f, 8.4f, 19f, 7.3f)
            lineTo(16.1f, 10.2f)
            lineTo(14f, 8.1f)
            lineTo(16.9f, 5.2f)
            curveTo(16f, 4.6f, 15.1f, 4.2f, 14.2f, 4.2f)
        }
    }

    val Check = icon("CodexCheck") {
        line {
            moveTo(4.8f, 12.4f)
            lineTo(9.5f, 17.2f)
            lineTo(19.5f, 6.6f)
        }
    }

    val CheckCircle = icon("CodexCheckCircle") {
        line {
            circle(12f, 12f, 8.2f)
            moveTo(8.1f, 12.2f)
            lineTo(10.8f, 15f)
            lineTo(16f, 9.3f)
        }
    }

    val ChevronRight = icon("CodexChevronRight", autoMirror = true) {
        line {
            moveTo(9f, 5.5f)
            lineTo(15.5f, 12f)
            lineTo(9f, 18.5f)
        }
    }

    val Close = icon("CodexClose") {
        line {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }

    val Code = icon("CodexCode") {
        line {
            moveTo(8.5f, 7f)
            lineTo(3.8f, 12f)
            lineTo(8.5f, 17f)
            moveTo(15.5f, 7f)
            lineTo(20.2f, 12f)
            lineTo(15.5f, 17f)
            moveTo(13.8f, 4.8f)
            lineTo(10.2f, 19.2f)
        }
    }

    val Compress = icon("CodexCompress") {
        line {
            moveTo(4f, 9f)
            lineTo(9f, 9f)
            lineTo(9f, 4f)
            moveTo(20f, 9f)
            lineTo(15f, 9f)
            lineTo(15f, 4f)
            moveTo(4f, 15f)
            lineTo(9f, 15f)
            lineTo(9f, 20f)
            moveTo(20f, 15f)
            lineTo(15f, 15f)
            lineTo(15f, 20f)
        }
    }

    val Computer = icon("CodexComputer") {
        line {
            roundRect(3.2f, 4f, 20.8f, 16.2f, 2f)
            moveTo(12f, 16.2f)
            lineTo(12f, 20f)
            moveTo(8.5f, 20f)
            lineTo(15.5f, 20f)
        }
    }

    val DeleteOutline = icon("CodexDelete") {
        line {
            moveTo(4.2f, 7f)
            lineTo(19.8f, 7f)
            moveTo(9f, 4.2f)
            lineTo(15f, 4.2f)
            moveTo(6.3f, 7f)
            lineTo(7.2f, 19f)
            quadTo(7.3f, 20.5f, 8.8f, 20.5f)
            lineTo(15.2f, 20.5f)
            quadTo(16.7f, 20.5f, 16.8f, 19f)
            lineTo(17.7f, 7f)
            moveTo(10f, 10.5f)
            lineTo(10.4f, 17f)
            moveTo(14f, 10.5f)
            lineTo(13.6f, 17f)
        }
    }

    val Description = icon("CodexDescription") {
        line {
            moveTo(6f, 3.5f)
            lineTo(14f, 3.5f)
            lineTo(19f, 8.5f)
            lineTo(19f, 20.5f)
            lineTo(6f, 20.5f)
            close()
            moveTo(14f, 3.5f)
            lineTo(14f, 8.5f)
            lineTo(19f, 8.5f)
            moveTo(9f, 12f)
            lineTo(16f, 12f)
            moveTo(9f, 15.5f)
            lineTo(16f, 15.5f)
        }
    }

    val Edit = icon("CodexEdit") {
        line {
            moveTo(5f, 16.5f)
            lineTo(4f, 20f)
            lineTo(7.5f, 19f)
            lineTo(18.5f, 8f)
            lineTo(16f, 5.5f)
            close()
            moveTo(14.5f, 7f)
            lineTo(17f, 9.5f)
        }
    }

    val ErrorOutline = icon("CodexError") {
        line {
            circle(12f, 12f, 8.3f)
            moveTo(12f, 7.3f)
            lineTo(12f, 12.8f)
        }
        fill {
            circle(12f, 16.2f, 1.05f)
        }
    }

    val ExpandMore = icon("CodexExpandMore") {
        line {
            moveTo(5.5f, 9f)
            lineTo(12f, 15.5f)
            lineTo(18.5f, 9f)
        }
    }

    val Extension = icon("CodexExtension") {
        line {
            moveTo(9f, 4f)
            lineTo(15f, 4f)
            lineTo(15f, 7f)
            curveTo(14.2f, 7f, 14.2f, 9f, 15f, 9f)
            lineTo(20f, 9f)
            lineTo(20f, 15f)
            lineTo(17f, 15f)
            curveTo(17f, 14.2f, 15f, 14.2f, 15f, 15f)
            lineTo(15f, 20f)
            lineTo(9f, 20f)
            lineTo(9f, 17f)
            curveTo(9.8f, 17f, 9.8f, 15f, 9f, 15f)
            lineTo(4f, 15f)
            lineTo(4f, 9f)
            lineTo(9f, 9f)
            close()
        }
    }

    val Flag = icon("CodexFlag") {
        line {
            moveTo(6f, 3.5f)
            lineTo(6f, 20.5f)
            moveTo(6f, 4.5f)
            lineTo(18.5f, 4.5f)
            lineTo(14.8f, 8.5f)
            lineTo(18.5f, 12.5f)
            lineTo(6f, 12.5f)
        }
    }

    val FolderOpen = icon("CodexFolderOpen") {
        line {
            moveTo(3.5f, 7f)
            lineTo(3.5f, 18.8f)
            lineTo(19.5f, 18.8f)
            lineTo(21f, 9f)
            lineTo(8f, 9f)
            lineTo(6.3f, 6f)
            lineTo(3.5f, 6f)
            close()
            moveTo(3.5f, 7f)
            lineTo(8f, 7f)
        }
    }

    val Hub = icon("CodexHub") {
        line {
            circle(12f, 12f, 2.3f)
            moveTo(10.5f, 10.4f)
            lineTo(6.2f, 6.2f)
            moveTo(13.5f, 10.4f)
            lineTo(17.8f, 6.2f)
            moveTo(12f, 14.3f)
            lineTo(12f, 18.5f)
        }
        fill {
            circle(5.5f, 5.5f, 1.5f)
            circle(18.5f, 5.5f, 1.5f)
            circle(12f, 19.5f, 1.5f)
        }
    }

    val Image = icon("CodexImage") {
        line {
            roundRect(3.5f, 4f, 20.5f, 20f, 2f)
            circle(8.1f, 8.3f, 1.5f)
            moveTo(5f, 18f)
            lineTo(10f, 13f)
            lineTo(13f, 16f)
            lineTo(16f, 13f)
            lineTo(19f, 16.2f)
        }
    }

    val Key = icon("CodexKey") {
        line {
            circle(8f, 8f, 3.8f)
            moveTo(10.8f, 10.8f)
            lineTo(19.5f, 19.5f)
            moveTo(15f, 15f)
            lineTo(17.2f, 12.8f)
            moveTo(17.2f, 17.2f)
            lineTo(19.4f, 15f)
        }
    }

    val KeyboardArrowDown = icon("CodexKeyboardArrowDown") {
        line {
            moveTo(6f, 9f)
            lineTo(12f, 15f)
            lineTo(18f, 9f)
        }
    }

    val KeyboardArrowUp = icon("CodexKeyboardArrowUp") {
        line {
            moveTo(6f, 15f)
            lineTo(12f, 9f)
            lineTo(18f, 15f)
        }
    }

    val KeyboardHide = icon("CodexKeyboardHide") {
        line {
            roundRect(3.2f, 5.5f, 20.8f, 15.5f, 1.8f)
            moveTo(6.5f, 9f)
            lineTo(7.5f, 9f)
            moveTo(10.5f, 9f)
            lineTo(11.5f, 9f)
            moveTo(14.5f, 9f)
            lineTo(15.5f, 9f)
            moveTo(6.5f, 12f)
            lineTo(7.5f, 12f)
            moveTo(9.8f, 12f)
            lineTo(14.2f, 12f)
            moveTo(16.5f, 12f)
            lineTo(17.5f, 12f)
            moveTo(8f, 19f)
            lineTo(12f, 22f)
            lineTo(16f, 19f)
        }
    }

    val Lock = icon("CodexLock") {
        line {
            roundRect(5f, 10f, 19f, 20f, 2.2f)
            moveTo(8f, 10f)
            lineTo(8f, 7.5f)
            curveTo(8f, 3.8f, 16f, 3.8f, 16f, 7.5f)
            lineTo(16f, 10f)
        }
        fill {
            circle(12f, 15f, 1.4f)
        }
    }

    val Menu = icon("CodexMenu") {
        line {
            moveTo(4f, 7f)
            lineTo(20f, 7f)
            moveTo(4f, 12f)
            lineTo(16f, 12f)
            moveTo(4f, 17f)
            lineTo(20f, 17f)
        }
    }

    val MoreVert = icon("CodexMoreVert") {
        fill {
            circle(12f, 5.2f, 1.5f)
            circle(12f, 12f, 1.5f)
            circle(12f, 18.8f, 1.5f)
        }
    }

    val Pause = icon("CodexPause") {
        fill {
            roundRect(6.4f, 5.5f, 9.9f, 18.5f, 1.1f)
            roundRect(14.1f, 5.5f, 17.6f, 18.5f, 1.1f)
        }
    }

    val PlayArrow = icon("CodexPlay") {
        fill {
            moveTo(7.5f, 5.2f)
            lineTo(19.2f, 12f)
            lineTo(7.5f, 18.8f)
            close()
        }
    }

    val Psychology = icon("CodexPsychology") {
        line {
            moveTo(12f, 5.2f)
            curveTo(8.8f, 3.7f, 5.5f, 5.6f, 5.5f, 8.7f)
            curveTo(3.4f, 9.8f, 3.2f, 12.5f, 4.7f, 14.2f)
            curveTo(4.3f, 17.1f, 6.5f, 19.5f, 9.4f, 18.9f)
            curveTo(10.2f, 20.5f, 13.8f, 20.5f, 14.6f, 18.9f)
            curveTo(17.5f, 19.5f, 19.7f, 17.1f, 19.3f, 14.2f)
            curveTo(20.8f, 12.5f, 20.6f, 9.8f, 18.5f, 8.7f)
            curveTo(18.5f, 5.6f, 15.2f, 3.7f, 12f, 5.2f)
            close()
            moveTo(12f, 5.2f)
            lineTo(12f, 19.5f)
            moveTo(8f, 10f)
            lineTo(10.5f, 12.2f)
            moveTo(10.5f, 12.2f)
            lineTo(10.8f, 15.2f)
            moveTo(16f, 10f)
            lineTo(13.5f, 12.2f)
            moveTo(13.5f, 12.2f)
            lineTo(13.2f, 15.2f)
        }
        fill {
            circle(12f, 12.2f, 1.05f)
        }
    }

    val PushPin = icon("CodexPushPin") {
        line {
            moveTo(12f, 21f)
            lineTo(12f, 14.5f)
            curveTo(7.2f, 13.7f, 6.4f, 7.5f, 8.2f, 4.2f)
            lineTo(15.8f, 4.2f)
            curveTo(17.6f, 7.5f, 16.8f, 13.7f, 12f, 14.5f)
        }
        fill {
            circle(12f, 10f, 1.2f)
        }
    }

    val Refresh = icon("CodexRefresh") {
        line {
            moveTo(19.2f, 9f)
            curveTo(17.3f, 4.6f, 11.7f, 3f, 7.3f, 5.8f)
            curveTo(2.8f, 8.7f, 3f, 15.4f, 7.2f, 18.4f)
            curveTo(10.6f, 20.8f, 15.5f, 20.1f, 18.3f, 17.2f)
            moveTo(19.2f, 4.8f)
            lineTo(19.2f, 9f)
            lineTo(15f, 9f)
        }
    }

    val Router = icon("CodexRouter") {
        line {
            roundRect(3.5f, 5f, 20.5f, 11f, 1.6f)
            roundRect(3.5f, 13f, 20.5f, 19f, 1.6f)
            moveTo(7f, 8f)
            lineTo(8.5f, 8f)
            moveTo(7f, 16f)
            lineTo(8.5f, 16f)
            moveTo(11.5f, 8f)
            lineTo(17f, 8f)
            moveTo(11.5f, 16f)
            lineTo(17f, 16f)
        }
    }

    val Search = icon("CodexSearch") {
        line {
            circle(10.5f, 10.5f, 6.3f)
            moveTo(15.2f, 15.2f)
            lineTo(20.3f, 20.3f)
        }
    }

    val Settings = icon("CodexSettings") {
        line {
            circle(12f, 12f, 5.3f)
            moveTo(12f, 2.8f)
            lineTo(12f, 5f)
            moveTo(12f, 19f)
            lineTo(12f, 21.2f)
            moveTo(3.6f, 7.2f)
            lineTo(5.5f, 8.3f)
            moveTo(18.5f, 15.7f)
            lineTo(20.4f, 16.8f)
            moveTo(3.6f, 16.8f)
            lineTo(5.5f, 15.7f)
            moveTo(18.5f, 8.3f)
            lineTo(20.4f, 7.2f)
        }
        fill {
            circle(12f, 12f, 1.25f)
        }
    }

    val SmartToy = icon("CodexSmartToy") {
        line {
            roundRect(3.5f, 7f, 20.5f, 18f, 3f)
            moveTo(12f, 3f)
            lineTo(12f, 7f)
            moveTo(2f, 11f)
            lineTo(3.5f, 11f)
            moveTo(20.5f, 11f)
            lineTo(22f, 11f)
            moveTo(7.5f, 13f)
            lineTo(10f, 15.5f)
            moveTo(16.5f, 13f)
            lineTo(14f, 15.5f)
        }
        fill {
            circle(12f, 3f, 1.4f)
            circle(8f, 17f, 1.15f)
            circle(12f, 17f, 1.15f)
            circle(16f, 17f, 1.15f)
        }
    }

    val Stop = icon("CodexStop") {
        fill {
            roundRect(6f, 6f, 18f, 18f, 2.2f)
        }
    }

    val Terminal = icon("CodexTerminal") {
        line {
            roundRect(3f, 4f, 21f, 20f, 2.4f)
            moveTo(3f, 8.2f)
            lineTo(21f, 8.2f)
            moveTo(7.2f, 12f)
            lineTo(10.7f, 15f)
            lineTo(7.2f, 18f)
            moveTo(13f, 18f)
            lineTo(17f, 18f)
        }
        fill {
            circle(5.8f, 6.1f, 0.75f)
            circle(8.2f, 6.1f, 0.75f)
        }
    }

    val Tune = icon("CodexTune") {
        line {
            moveTo(4f, 6f)
            lineTo(20f, 6f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 18f)
            lineTo(20f, 18f)
        }
        fill {
            circle(9f, 6f, 1.7f)
            circle(15f, 12f, 1.7f)
            circle(8f, 18f, 1.7f)
        }
    }

    val Unarchive = icon("CodexUnarchive") {
        line {
            roundRect(3.5f, 4.5f, 20.5f, 8.5f, 1.3f)
            moveTo(5.2f, 8.5f)
            lineTo(5.2f, 18.3f)
            quadTo(5.2f, 20f, 6.9f, 20f)
            lineTo(17.1f, 20f)
            quadTo(18.8f, 20f, 18.8f, 18.3f)
            lineTo(18.8f, 8.5f)
            moveTo(12f, 17.5f)
            lineTo(12f, 11.5f)
            moveTo(9f, 14.5f)
            lineTo(12f, 11.5f)
            lineTo(15f, 14.5f)
        }
    }

    val all: List<Pair<String, ImageVector>> = listOf(
        "ArrowBack" to ArrowBack,
        "CallSplit" to CallSplit,
        "List" to List,
        "Send" to Send,
        "Add" to Add,
        "Archive" to Archive,
        "Build" to Build,
        "Check" to Check,
        "CheckCircle" to CheckCircle,
        "ChevronRight" to ChevronRight,
        "Close" to Close,
        "Code" to Code,
        "Compress" to Compress,
        "Computer" to Computer,
        "DeleteOutline" to DeleteOutline,
        "Description" to Description,
        "Edit" to Edit,
        "ErrorOutline" to ErrorOutline,
        "ExpandMore" to ExpandMore,
        "Extension" to Extension,
        "Flag" to Flag,
        "FolderOpen" to FolderOpen,
        "Hub" to Hub,
        "Image" to Image,
        "Key" to Key,
        "KeyboardArrowDown" to KeyboardArrowDown,
        "KeyboardArrowUp" to KeyboardArrowUp,
        "KeyboardHide" to KeyboardHide,
        "Lock" to Lock,
        "Menu" to Menu,
        "MoreVert" to MoreVert,
        "Pause" to Pause,
        "PlayArrow" to PlayArrow,
        "Psychology" to Psychology,
        "PushPin" to PushPin,
        "Refresh" to Refresh,
        "Router" to Router,
        "Search" to Search,
        "Settings" to Settings,
        "SmartToy" to SmartToy,
        "Stop" to Stop,
        "Terminal" to Terminal,
        "Tune" to Tune,
        "Unarchive" to Unarchive,
    )
}

private fun icon(
    name: String,
    autoMirror: Boolean = false,
    content: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror,
).apply(content).build()

private fun ImageVector.Builder.line(
    pathBuilder: PathBuilder.() -> Unit,
) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}

private fun ImageVector.Builder.fill(
    pathBuilder: PathBuilder.() -> Unit,
) {
    path(
        fill = SolidColor(Color.Black),
        pathBuilder = pathBuilder,
    )
}

private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    val control = radius * 0.5522848f
    moveTo(centerX - radius, centerY)
    curveTo(centerX - radius, centerY - control, centerX - control, centerY - radius, centerX, centerY - radius)
    curveTo(centerX + control, centerY - radius, centerX + radius, centerY - control, centerX + radius, centerY)
    curveTo(centerX + radius, centerY + control, centerX + control, centerY + radius, centerX, centerY + radius)
    curveTo(centerX - control, centerY + radius, centerX - radius, centerY + control, centerX - radius, centerY)
    close()
}

private fun PathBuilder.roundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
) {
    moveTo(left + radius, top)
    lineTo(right - radius, top)
    quadTo(right, top, right, top + radius)
    lineTo(right, bottom - radius)
    quadTo(right, bottom, right - radius, bottom)
    lineTo(left + radius, bottom)
    quadTo(left, bottom, left, bottom - radius)
    lineTo(left, top + radius)
    quadTo(left, top, left + radius, top)
    close()
}
