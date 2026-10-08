package jp.tpp.t9s.ledgerpad.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {

    @Test
    fun testSimpleAddition() {
        assertEquals(15.0, Calc.evaluate("10+5")!!, 0.001)
    }

    @Test
    fun testPrecedence() {
        assertEquals(22.0, Calc.evaluate("10+3*4")!!, 0.001)
        assertEquals(22.0, Calc.evaluate("10+3×4")!!, 0.001)
        assertEquals(11.5, Calc.evaluate("10+3/2")!!, 0.001)
        assertEquals(11.5, Calc.evaluate("10+3÷2")!!, 0.001)
    }

    @Test
    fun testDecimals() {
        assertEquals(25.75, Calc.evaluate("12.50+13.25")!!, 0.001)
    }

    @Test
    fun testSingleNumber() {
        assertEquals(500.0, Calc.evaluate("500")!!, 0.001)
    }

    @Test
    fun testEmptyOrInvalid() {
        assertNull(Calc.evaluate(""))
        assertNull(Calc.evaluate("abc"))
    }
}
