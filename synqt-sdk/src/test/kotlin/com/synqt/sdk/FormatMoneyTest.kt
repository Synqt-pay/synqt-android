package com.synqt.sdk

import com.synqt.sdk.internal.sheet.formatMoney
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatMoneyTest {

    @Test
    fun randUsesTheHouseFormat() = assertEquals("R1,250.00", formatMoney(BigDecimal("1250"), "ZAR"))

    @Test
    fun otherCurrenciesLeadWithTheCode() = assertEquals("USD 1,250.50", formatMoney(BigDecimal("1250.5"), "USD"))

    @Test
    fun realDecimalsAreNeverRoundedAway() = assertEquals("BTC 0.00012345", formatMoney(BigDecimal("0.00012345"), "BTC"))
}
