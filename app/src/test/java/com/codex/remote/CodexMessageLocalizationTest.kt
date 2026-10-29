package com.codex.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class CodexMessageLocalizationTest {
    @Test
    fun translatesTheObservedLowRiskApprovalWarning() {
        val message = "Automatic approval review approved (risk: low, authorization: unknown): " +
            "Auto-review returned a low-risk allow decision."

        assertEquals(
            "自动审批：已批准本次操作（风险：低；授权状态：未知）。" +
                "审查程序判定为低风险，允许继续。",
            localizeCodexMessage(message),
        )
    }

    @Test
    fun translatesDeniedAndTimedOutAutoReviews() {
        assertEquals(
            "自动审批：已拒绝本次操作（风险：高；授权状态：未知）。审查程序拒绝本次操作。",
            localizeCodexMessage(
                "Automatic approval review denied (risk: high, authorization: unknown): " +
                    "Auto-review returned a deny decision.",
            ),
        )
        assertEquals(
            "自动审批：审查超时。审查程序未在时限内返回决定。",
            localizeCodexMessage("Automatic approval review timed out while waiting for Auto-review."),
        )
    }

    @Test
    fun leavesChineseRemoteMessagesUnchangedAndHidesUnknownEnglishDetails() {
        val message = "远端 Codex 提示：配置已重新加载。"

        assertEquals(message, localizeCodexMessage(message))
        assertEquals(
            "远端 Codex 返回通用提示，请在服务器端的 Codex 日志中查看技术详情。",
            localizeCodexMessage("Codex is ignoring 1 unrecognized configuration setting."),
        )
    }

    @Test
    fun localizesKnownRemoteChoiceLabelsAndDescriptions() {
        assertEquals("快速", localizeCodexLabel("Fast"))
        assertEquals("标准", localizeCodexLabel("standard"))
        assertEquals("优先处理", localizeCodexDescription("Priority processing"))
        assertEquals("平衡速度与推理深度", localizeCodexDescription("Balanced"))
        assertEquals(
            "详细说明由远端服务提供",
            localizeCodexDescription("Provider-specific detail"),
        )
    }
}
