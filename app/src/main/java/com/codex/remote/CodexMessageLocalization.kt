package com.codex.remote

private val AUTO_REVIEW_MARKER = Regex(
    """automatic\s+approval\s+review|auto-review\s+returned""",
    RegexOption.IGNORE_CASE,
)
private val RISK_VALUE = Regex(
    """\brisk\s*:\s*([^,)]+)""",
    RegexOption.IGNORE_CASE,
)
private val AUTHORIZATION_VALUE = Regex(
    """\bauthorization\s*:\s*([^)]+)""",
    RegexOption.IGNORE_CASE,
)
private val IGNORED_CONFIG_COUNT = Regex(
    """Codex\s+is\s+ignoring\s+(\d+)\s+unrecogni[sz]ed\s+configuration\s+settings?\.""",
    RegexOption.IGNORE_CASE,
)
private val IGNORED_CONFIG_DETAIL = Regex(
    """^\s*(.+?)\s*\((.+?)\):\s*`?([A-Za-z0-9_.-]+)`?\s+is\s+ignored\.?\s*$""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
)
private val LATIN_WORD = Regex("""[A-Za-z]{3,}""")
private val CJK_CHARACTER = Regex("""[\u3400-\u9FFF]""")

internal fun localizeCodexMessage(message: String): String {
    val normalized = message.trim()
    if (normalized.isEmpty()) return normalized
    localizeIgnoredConfigWarning(normalized)?.let { return it }
    if (!AUTO_REVIEW_MARKER.containsMatchIn(normalized)) {
        return if (LATIN_WORD.containsMatchIn(normalized) &&
            !CJK_CHARACTER.containsMatchIn(normalized)
        ) {
            "远端 Codex 提示：$normalized"
        } else {
            normalized
        }
    }

    val lower = normalized.lowercase()
    val outcome = when {
        "approved" in lower || "allow decision" in lower -> "已批准本次操作"
        "denied" in lower || "deny decision" in lower || "blocked" in lower -> "已拒绝本次操作"
        "timed out" in lower || "timeout" in lower -> "审查超时"
        "failed" in lower || "error" in lower -> "审查失败"
        else -> "审查已完成"
    }
    val risk = RISK_VALUE.find(normalized)?.groupValues?.getOrNull(1)
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.let(::localizeRisk)
    val authorization = AUTHORIZATION_VALUE.find(normalized)?.groupValues?.getOrNull(1)
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.let(::localizeAuthorization)
    val details = buildList {
        risk?.let { add("风险：$it") }
        authorization?.let { add("授权状态：$it") }
    }
    val reason = when {
        "low-risk allow decision" in lower -> "审查程序判定为低风险，允许继续。"
        "allow decision" in lower -> "审查程序允许本次操作继续。"
        "deny decision" in lower -> "审查程序拒绝本次操作。"
        "timed out" in lower || "timeout" in lower -> "审查程序未在时限内返回决定。"
        "failed" in lower || "error" in lower -> "审查程序没有返回有效决定。"
        else -> null
    }

    return buildString {
        append("自动审批：")
        append(outcome)
        if (details.isNotEmpty()) {
            append("（")
            append(details.joinToString("；"))
            append("）")
        }
        if (reason != null) {
            append("。")
            append(reason)
        }
    }
}

internal fun localizeCodexDiagnostic(message: String): String? =
    localizeIgnoredConfigWarning(message.trim())

internal fun localizeCodexLabel(value: String): String {
    val normalized = value.trim()
    return when (normalized.lowercase()) {
        "default" -> "默认"
        "plan" -> "计划"
        "fast" -> "快速"
        "standard" -> "标准"
        else -> if (LATIN_WORD.containsMatchIn(normalized)) "自定义" else normalized
    }
}

internal fun localizeCodexDescription(value: String): String {
    val normalized = value.trim()
    return when (normalized.lowercase()) {
        "fast" -> "快速响应"
        "balanced" -> "平衡速度与推理深度"
        "deep" -> "更深入的推理"
        "maximum" -> "最大推理深度"
        "priority processing" -> "优先处理"
        "frontier model" -> "前沿模型"
        else -> if (LATIN_WORD.containsMatchIn(normalized)) {
            "详细说明由远端服务提供"
        } else {
            normalized
        }
    }
}

private fun localizeRisk(value: String): String = when (value.lowercase()) {
    "low" -> "低"
    "medium" -> "中"
    "high" -> "高"
    "unknown" -> "未知"
    else -> value
}

private fun localizeAuthorization(value: String): String = when (value.lowercase()) {
    "unknown" -> "未知"
    "authorized" -> "已授权"
    "unauthorized" -> "未授权"
    "not_required", "not required" -> "不需要"
    else -> value
}

private fun localizeIgnoredConfigWarning(message: String): String? {
    val count = IGNORED_CONFIG_COUNT.find(message)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    val detail = IGNORED_CONFIG_DETAIL.find(message)
    if (count == null && detail == null) return null

    return buildString {
        if (count != null) {
            append("远端 Codex 忽略了 ")
            append(count)
            append(" 项无法识别的配置。")
        } else {
            append("远端 Codex 忽略了无法识别的配置。")
        }
        detail?.let { match ->
            val source = match.groupValues[1].trim()
            val path = match.groupValues[2].trim()
            val key = match.groupValues[3].trim()
            append("配置项 ")
            append(key)
            append(" 不会生效")
            if (source.isNotBlank() || path.isNotBlank()) {
                append("（")
                if (source.isNotBlank()) append("来源：$source")
                if (source.isNotBlank() && path.isNotBlank()) append("，")
                if (path.isNotBlank()) append("文件：$path")
                append("）")
            }
            append("。")
        }
        append("请检查服务器上的 Codex 配置文件。")
    }
}
