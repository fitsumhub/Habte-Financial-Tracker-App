package com.mobile.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SmsParser — validates SMS transaction parsing
 * for all supported Ethiopian banks and transaction types.
 */
class SmsParserTest {

    // ── CBE (Commercial Bank of Ethiopia) ────────────────────────────────

    @Test
    fun `parse CBE credit SMS`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your account has been credited with ETB 5,000.00. Your balance is ETB 25,000.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("CBE", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(5000.0, tx.amount, 0.01)
        assertEquals(25000.0, tx.balance!!, 0.01)
    }

    @Test
    fun `parse CBE debit SMS`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your account has been debited with ETB 1,200.50. Your balance is ETB 23,799.50",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("CBE", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(1200.50, tx.amount, 0.01)
        assertEquals(23799.50, tx.balance!!, 0.01)
    }

    @Test
    fun `parse CBE from sender 1000`() {
        val tx = SmsParser.parseMessage(
            "1000",
            "ETB 500.00 has been credited to your account. Balance is 10,000.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("CBE", tx!!.bankShortName)
    }

    @Test
    fun `parse CBE from sender 8008`() {
        val tx = SmsParser.parseMessage(
            "8008",
            "ETB 300.00 debited from your account. Balance: ETB 9,700.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("CBE", tx!!.bankShortName)
        assertEquals("debit", tx.type)
    }

    // ── BOA (Bank of Abyssinia) ──────────────────────────────────────────

    @Test
    fun `parse BOA credit SMS`() {
        val tx = SmsParser.parseMessage(
            "BOA",
            "Amount of 3,500.00 Birr has been credited to your account. Available balance: 15,000.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("BOA", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(3500.0, tx.amount, 0.01)
    }

    @Test
    fun `parse BOA via abyssinia sender`() {
        val tx = SmsParser.parseMessage(
            "Abyssinia",
            "Your account received ETB 2,000.00. Balance is ETB 12,000.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("BOA", tx!!.bankShortName)
    }

    // ── Telebirr ─────────────────────────────────────────────────────────

    @Test
    fun `parse Telebirr credit SMS`() {
        val tx = SmsParser.parseMessage(
            "telebirr",
            "You have received ETB 1,000.00. Your balance is ETB 3,500.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("TEL", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(1000.0, tx.amount, 0.01)
        // Telebirr forces null accountSuffix
        assertNull(tx.accountSuffix)
    }

    @Test
    fun `parse Telebirr debit SMS`() {
        val tx = SmsParser.parseMessage(
            "127",
            "ETB 500.00 has been debited from your wallet. Balance: 3,000.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("TEL", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertNull(tx.accountSuffix) // Telebirr always null suffix
    }

    // ── Awash Bank ───────────────────────────────────────────────────────

    @Test
    fun `parse Awash credit SMS`() {
        val tx = SmsParser.parseMessage(
            "awash",
            "Deposited ETB 4,000.00 to your account. Current balance: ETB 20,000.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("AWA", tx!!.bankShortName)
        assertEquals("credit", tx.type)
    }

    // ── Dashen Bank ──────────────────────────────────────────────────────

    @Test
    fun `parse Dashen debit SMS`() {
        val tx = SmsParser.parseMessage(
            "dashen",
            "Transfer of ETB 2,500.00 has been debited from your account 1234567890. Balance: 17,500.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("DAS", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(2500.0, tx.amount, 0.01)
    }

    // ── Edge cases ───────────────────────────────────────────────────────

    @Test
    fun `unknown sender returns null`() {
        val tx = SmsParser.parseMessage(
            "UnknownBank",
            "Your account has been credited with ETB 1,000.00",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `no amount in message returns null`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your account has been credited. Check your balance.",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `no transaction type keywords returns null`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your OTP is 123456. Do not share this with anyone.",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `zero amount returns null`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your account has been credited with ETB 0.00",
            1700000000000L
        )
        assertNull(tx)
    }

    // ── Category classification ──────────────────────────────────────────

    @Test
    fun `credit transactions are categorized as Income`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Salary deposit of ETB 10,000.00 credited to your account",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("Income", tx!!.category)
    }

    @Test
    fun `ATM withdrawal categorized as Cash`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "ATM withdrawal of ETB 2,000.00 debited from your account",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("Cash", tx!!.category)
        assertEquals("ATM Withdrawal", tx.title)
    }

    @Test
    fun `airtime categorized as Bills and Utilities`() {
        val tx = SmsParser.parseMessage(
            "telebirr",
            "Airtime recharge of ETB 100.00 has been debited. Balance: 900.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("Bills & Utilities", tx!!.category)
    }

    @Test
    fun `transfer categorized as Transfers`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Transfer of ETB 5,000.00 debited from your account to account 9876543210",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("Transfers", tx!!.category)
    }

    // ── All bank short-name detection ────────────────────────────────────

    @Test
    fun `detect Hibret bank`() {
        val tx = SmsParser.parseMessage("hibret", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("HIB", tx!!.bankShortName)
    }

    @Test
    fun `detect Zemen bank`() {
        val tx = SmsParser.parseMessage("zemen", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("ZEM", tx!!.bankShortName)
    }

    @Test
    fun `detect Nib bank`() {
        val tx = SmsParser.parseMessage("nib", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("NIB", tx!!.bankShortName)
    }

    @Test
    fun `detect Coop bank`() {
        val tx = SmsParser.parseMessage("coopbank", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("COO", tx!!.bankShortName)
    }

    @Test
    fun `detect Abay bank`() {
        val tx = SmsParser.parseMessage("abay", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("ABY", tx!!.bankShortName)
    }

    @Test
    fun `detect Berhan bank`() {
        val tx = SmsParser.parseMessage("berhan", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("BER", tx!!.bankShortName)
    }

    @Test
    fun `detect Bunna bank`() {
        val tx = SmsParser.parseMessage("bunna", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("BUN", tx!!.bankShortName)
    }

    @Test
    fun `detect Wegagen bank`() {
        val tx = SmsParser.parseMessage("wegagen", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("WEG", tx!!.bankShortName)
    }

    @Test
    fun `detect Oromia bank`() {
        val tx = SmsParser.parseMessage("oromia", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("ORO", tx!!.bankShortName)
    }

    @Test
    fun `detect Lion bank`() {
        val tx = SmsParser.parseMessage("lion", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("LIO", tx!!.bankShortName)
    }

    @Test
    fun `detect Enat bank`() {
        val tx = SmsParser.parseMessage("enat", "ETB 100.00 credited to your account", 1700000000000L)
        assertNotNull(tx)
        assertEquals("ENA", tx!!.bankShortName)
    }

    // ── Transaction ID uniqueness ────────────────────────────────────────

    @Test
    fun `transaction ID includes bank short name`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "ETB 1,000.00 credited to your account",
            1700000000000L
        )
        assertNotNull(tx)
        assertTrue(tx!!.id.contains("CBE"))
    }

    @Test
    fun `different banks produce different IDs for same timestamp`() {
        val tx1 = SmsParser.parseMessage("CBE", "ETB 1,000.00 credited to your account", 1700000000000L)
        val tx2 = SmsParser.parseMessage("BOA", "ETB 1,000.00 credited to your account", 1700000000000L)
        assertNotNull(tx1)
        assertNotNull(tx2)
        assertNotEquals(tx1!!.id, tx2!!.id)
    }

    // ── Date formatting ──────────────────────────────────────────────────

    @Test
    fun `date is formatted as MMM dd, yyyy`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "ETB 1,000.00 credited to your account",
            1700000000000L // Nov 14, 2023
        )
        assertNotNull(tx)
        // Just check it's non-empty and contains a comma
        assertTrue(tx!!.date.contains(","))
    }

    // ── Amount format variations ─────────────────────────────────────────

    @Test
    fun `parse Birr amount format`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "1,500.00 Birr has been credited to your account",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals(1500.0, tx!!.amount, 0.01)
    }

    @Test
    fun `parse amount without decimals`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "ETB 500 has been credited to your account",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals(500.0, tx!!.amount, 0.01)
    }

    @Test
    fun `parse Br amount without trailing period`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "500 Br has been debited from your account",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals(500.0, tx!!.amount, 0.01)
    }

    // ── Failed / non-completed transactions ────────────────────────────────

    @Test
    fun `failed transaction is not recorded even with a debited keyword`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your account has been debited with ETB 500.00 for Merchant X. Transaction failed, amount will be reversed within 24 hours.",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `declined transaction is not recorded`() {
        val tx = SmsParser.parseMessage(
            "BOA",
            "Your payment of ETB 200.00 was declined due to insufficient balance.",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `insufficient balance message is not recorded`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Unable to complete: insufficient funds for a debit of ETB 1,000.00.",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `a real reversal refund credit is still recorded`() {
        // Unlike a failed *attempt*, a reversal that actually credits money back is a
        // genuine transaction and must not be swallowed by the failure guard.
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your transaction has been reversed. ETB 500.00 has been credited back to your account. Balance: ETB 5,500.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("credit", tx!!.type)
        assertEquals(500.0, tx.amount, 0.01)
    }

    // ── Pending / future-dated transactions ─────────────────────────────────

    @Test
    fun `future-dated standing order is not recorded`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your subscription payment of ETB 100.00 will be debited on 2026-08-01.",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `message stating the transaction is scheduled to be debited is not recorded`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your account is scheduled to be debited with ETB 250.00 tomorrow.",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `a completed standing-order payment is still recorded despite the word scheduled`() {
        // "scheduled payment" here names the recurring plan, not a pending state — the
        // message says the money has already moved, so it must not be dropped by the
        // pending-transaction guard (that guard only rejects unambiguous future tense
        // like "will be debited" / "is scheduled to be debited").
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your scheduled payment of ETB 250.00 has been debited from your account. Balance: ETB 750.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("debit", tx!!.type)
        assertEquals(250.0, tx.amount, 0.01)
    }

    // ── "sent" as a debit keyword (common mobile-money wallet phrasing) ─────

    @Test
    fun `wallet sent message is recorded as debit`() {
        val tx = SmsParser.parseMessage(
            "telebirr",
            "You have sent ETB 150.00 to 0912345678. Balance: ETB 850.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("debit", tx!!.type)
        assertEquals(150.0, tx.amount, 0.01)
    }

    // ── Loan categorization ──────────────────────────────────────────────────

    @Test
    fun `loan repayment is categorized as Loan`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your loan repayment of ETB 3,000.00 has been debited from your account. Balance: ETB 7,000.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("Loan Payment", tx!!.title)
        assertEquals("Loan", tx.category)
    }

    // ── Stable, content-derived transaction IDs (dedup across live-receive vs. resync) ──

    @Test
    fun `identical SMS content produces the same ID regardless of timestamp`() {
        val body = "Your account has been debited with ETB 500.00. Balance: ETB 4,500.00"
        // Simulates the live SmsReceiver path (delivery timestamp) vs. the historical
        // resync path (the inbox's stored `date` column), which can legitimately differ
        // by a few milliseconds for the exact same real message.
        val tx1 = SmsParser.parseMessage("CBE", body, 1700000000000L)
        val tx2 = SmsParser.parseMessage("CBE", body, 1700000000037L)
        assertNotNull(tx1)
        assertNotNull(tx2)
        assertEquals(tx1!!.id, tx2!!.id)
    }

    @Test
    fun `different SMS content from the same bank produces different IDs`() {
        val tx1 = SmsParser.parseMessage(
            "CBE", "Your account has been debited with ETB 500.00. Balance: ETB 4,500.00", 1700000000000L
        )
        val tx2 = SmsParser.parseMessage(
            "CBE", "Your account has been debited with ETB 200.00. Balance: ETB 4,300.00", 1700000000000L
        )
        assertNotNull(tx1)
        assertNotNull(tx2)
        assertNotEquals(tx1!!.id, tx2!!.id)
    }

    @Test
    fun `bank-stated reference number is used as the dedup key`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your account has been debited with ETB 500.00. Ref: FT23198ABCDE. Balance: ETB 4,500.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertTrue(tx!!.id.contains("FT23198ABCDE"))
    }

    // ── parseBody (NotificationCaptureListenerService's entry point) ────────────────────

    @Test
    fun `parseBody produces the same result as parseMessage for the same institution and body`() {
        val institution = InstitutionCatalog.findBySmsSender("CBE")!!
        val body = "Your account has been credited with ETB 750.00. Your balance is ETB 5,750.00"

        val viaSms = SmsParser.parseMessage("CBE", body, 1700000000000L)
        val viaNotification = SmsParser.parseBody(institution, body, 1700000000000L, idPrefix = "notif", sourceKey = "com.cbe.app")

        assertNotNull(viaSms)
        assertNotNull(viaNotification)
        assertEquals(viaSms!!.amount, viaNotification!!.amount, 0.01)
        assertEquals(viaSms.type, viaNotification.type)
        assertEquals(viaSms.bankShortName, viaNotification.bankShortName)
    }

    @Test
    fun `parseBody ids are namespaced by idPrefix so an SMS and a notification for the same event never collide`() {
        val institution = InstitutionCatalog.findBySmsSender("CBE")!!
        val body = "Your account has been debited with ETB 300.00. Balance: ETB 4,700.00"

        val smsTx = SmsParser.parseBody(institution, body, 1700000000000L, idPrefix = "sms", sourceKey = "CBE")
        val notifTx = SmsParser.parseBody(institution, body, 1700000000000L, idPrefix = "notif", sourceKey = "com.cbe.app")

        assertNotNull(smsTx)
        assertNotNull(notifTx)
        assertTrue(smsTx!!.id.startsWith("sms-CBE-"))
        assertTrue(notifTx!!.id.startsWith("notif-CBE-"))
        assertNotEquals(smsTx.id, notifTx.id)
    }

    @Test
    fun `money in and money out phrasing is recognized as credit and debit`() {
        val institution = InstitutionCatalog.findBySmsSender("CBE")!!

        val moneyIn = SmsParser.parseBody(
            institution, "Money In: ETB 1,000.00 to your account.", 1700000000000L, idPrefix = "notif", sourceKey = "com.cbe.app"
        )
        val moneyOut = SmsParser.parseBody(
            institution, "Money Out: ETB 250.00 from your account.", 1700000000000L, idPrefix = "notif", sourceKey = "com.cbe.app"
        )

        assertNotNull(moneyIn)
        assertEquals("credit", moneyIn!!.type)
        assertNotNull(moneyOut)
        assertEquals("debit", moneyOut!!.type)
    }
}
