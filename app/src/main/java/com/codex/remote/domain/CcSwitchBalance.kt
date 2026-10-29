package com.codex.remote.domain

internal fun formatComposerBalance(status: CcSwitchRemoteStatus?): String? {
    if (status == null || status.available == false) return null

    val quota = status.quotas.firstOrNull { snapshot ->
        snapshot.isValid != false && !snapshot.remaining.isNullOrBlank()
    } ?: return null

    return Regex("""[+-]?\d[\d,]*""")
        .find(quota.remaining.orEmpty())
        ?.value
        ?.replace(",", "")
}
