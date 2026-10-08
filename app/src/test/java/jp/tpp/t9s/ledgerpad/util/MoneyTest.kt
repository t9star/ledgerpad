package jp.tpp.t9s.ledgerpad.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun testFormatUSD() {
        val formatted = Money.format(1250L, "USD")
        assertEquals("$12.50", formatted)
    }

    @Test
    fun testFormatINR() {
        val formatted = Money.format(125000L, "INR")
        assertEquals("₹1,250.00", formatted)
    }

    @Test
    fun testFormatIDR_noDecimals() {
        val formatted = Money.format(5000000L, "IDR")
        assertEquals("Rp50,000", formatted)
    }

    @Test
    fun testParseToMinor() {
        assertEquals(1250L, Money.parseToMinor("12.50", "USD"))
        assertEquals(50000L, Money.parseToMinor("500", "USD"))
    }
}
