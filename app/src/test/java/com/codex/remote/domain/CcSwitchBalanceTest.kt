package com.codex.remote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CcSwitchBalanceTest {
    @Test
    fun cnyBalanceShowsIntegerOnlyForComposerRing() {
        assertEquals("123", formatComposerBalance(status(quota("123.45", "CNY"))))
    }

    @Test
    fun usdBalanceOmitsCurrencySymbolAndDecimals() {
        assertEquals("4", formatComposerBalance(status(quota("$4.20", "USD"))))
    }

    @Test
    fun unknownUnitKeepsOnlyTheIntegerAmount() {
        assertEquals("1200", formatComposerBalance(status(quota("1,200 tokens", "tokens"))))
    }

    @Test
    fun invalidFirstQuotaIsSkipped() {
        val balance = formatComposerBalance(
            status(
                quota("0", "CNY", isValid = false),
                quota("123.45", "CNY", isValid = true),
            ),
        )

        assertEquals("123", balance)
    }

    @Test
    fun missingOrUnavailableBalanceIsNotFabricated() {
        assertNull(formatComposerBalance(null))
        assertNull(formatComposerBalance(status(quota(null, "CNY"))))
        assertNull(formatComposerBalance(status(quota("123.45", "CNY")).copy(available = false)))
    }

    private fun quota(
        remaining: String?,
        unit: String?,
        isValid: Boolean? = true,
    ) = CcSwitchQuotaSnapshot(
        planName = unit,
        remaining = remaining,
        unit = unit,
        isValid = isValid,
    )

    private fun status(vararg quotas: CcSwitchQuotaSnapshot) = CcSwitchRemoteStatus(
        providerId = "deepseek",
        providerName = "DeepSeek",
        status = "ok",
        available = true,
        queriedAt = 1_793_253_980_715,
        quotas = quotas.toList(),
        error = null,
    )
}
