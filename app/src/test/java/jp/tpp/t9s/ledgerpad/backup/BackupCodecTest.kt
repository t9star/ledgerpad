package jp.tpp.t9s.ledgerpad.backup

import jp.tpp.t9s.ledgerpad.data.BackupCustomer
import jp.tpp.t9s.ledgerpad.data.BackupFile
import jp.tpp.t9s.ledgerpad.data.BackupTxn
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {

    @Test
    fun testJsonEncodeDecode() {
        val original = BackupFile(
            exportedAt = 1700000000000L,
            shopName = "Test Shop",
            currency = "INR",
            customers = listOf(
                BackupCustomer(id = 1L, name = "Rahul", phone = "+9199999", createdAt = 1700000000000L)
            ),
            txns = listOf(
                BackupTxn(id = 1L, customerId = 1L, type = 0, amountMinor = 50000L, note = "Goods", occurredAt = 1700000000000L, createdAt = 1700000000000L)
            )
        )

        val jsonStr = BackupCodec.encodeJson(original)
        val decoded = BackupCodec.decodeJson(jsonStr)

        assertEquals(original.shopName, decoded.shopName)
        assertEquals(original.customers.size, decoded.customers.size)
        assertEquals(original.customers.first().name, decoded.customers.first().name)
        assertEquals(original.txns.first().amountMinor, decoded.txns.first().amountMinor)
    }
}
