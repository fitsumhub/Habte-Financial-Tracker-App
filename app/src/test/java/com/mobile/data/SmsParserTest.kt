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

    @Test
    fun `parse M-PESA Kenya credit SMS`() {
        val tx = SmsParser.parseMessage(
            "MPESA",
            "M-PESA Confirmed. You have received KES 5,000 from 0712345678 on 31/8/26 at 1:47 AM. New M-PESA balance is KES 12,500.",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("MPESA", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(5000.0, tx.amount, 0.01)
        assertEquals(12500.0, tx.balance!!, 0.01)
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
    fun `otp sent message is not recorded as debit`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your one-time password has been sent to your phone. Balance: ETB 3,500.00",
            1700000000000L
        )
        assertNull(tx)
    }

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

    @Test
    fun `bank send message is recorded as debit`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Transfer send of ETB 1,200.00 to 0912345678. Your balance is ETB 5,800.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("debit", tx!!.type)
        assertEquals(1200.0, tx.amount, 0.01)
    }

    @Test
    fun `airtime purchase message is recorded as debit`() {
        val tx = SmsParser.parseMessage(
            "BOA",
            "Airtime purchase of ETB 50.00 has been debited from your account. Balance: ETB 1,450.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("debit", tx!!.type)
        assertEquals(50.0, tx.amount, 0.01)
        assertEquals("Bills & Utilities", tx.category)
    }

    @Test
    fun `failed transfer reversal message is not recorded`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "Your transfer of ETB 300.00 to 0912345678 has been reversed due to failed verification. Balance: ETB 7,700.00",
            1700000000000L
        )
        assertNull(tx)
    }

    @Test
    fun `safaricom M-PESA reference message is parsed as debit`() {
        val tx = SmsParser.parseMessage(
            "MPESA",
            "ውድ Etsub፣ የ5.00 ብር የሳፋሪኮም ጥቅል ለ 251718688451 በ31/8/26 በ1:47 AM ላይ ገዝተዋል። የገንዘብ ዝውውር መለያ ቁጥር UHV2KE5Z8A ነው። የአገልግሎት ክፍያ 0.00 ብር ነው። አሁን ያለዎት የM-PESA ቀሪ ሒሳብ 0.00 ብር ነው። የ M-PESA የሂሳብ ቁጥርዎን ከፋይዳ ጋር ለማስተሳሰር ከታች ያለውን ማስፈንጠሪያ ይጠቀሙ ፡፡  https://m-pesabusiness.safaricom.et/nid/login",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("MPESA", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(5.0, tx.amount, 0.01)
        assertEquals(0.0, tx.balance!!, 0.01)
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
    fun `parseBody ids are unified so an SMS and a notification for the same event converge`() {
        val institution = InstitutionCatalog.findBySmsSender("CBE")!!
        val body = "Your account has been debited with ETB 300.00. Balance: ETB 4,700.00"

        val smsTx = SmsParser.parseBody(institution, body, 1700000000000L, idPrefix = "sms", sourceKey = "CBE")
        val notifTx = SmsParser.parseBody(institution, body, 1700000000000L, idPrefix = "notif", sourceKey = "com.cbe.app")

        assertNotNull(smsTx)
        assertNotNull(notifTx)
        assertTrue(smsTx!!.id.startsWith("tx-CBE-"))
        assertTrue(notifTx!!.id.startsWith("tx-CBE-"))
        assertEquals(smsTx.id, notifTx.id)
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

    @Test
    fun `parse Amharic CBE credit SMS`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "የአካውንት ቁጥርዎ ••••1234 በ ETB 5,000.00 ገቢ ሆኗል። ቀሪ ሂሳብዎ ETB 25,000.00 ነው። ማጣቀሻ FT26A1B2C3",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("CBE", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(5000.0, tx.amount, 0.01)
        assertEquals(25000.0, tx.balance!!, 0.01)
        assertEquals("1234", tx.accountSuffix)
        assertTrue(tx.id.contains("FT26A1B2C3"))
    }

    @Test
    fun `parse Amharic CBE debit SMS`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "የአካውንት ቁጥርዎ ••••1234 በ ETB 1,200.50 ወጪ ሆኗል። ቀሪ ሂሳብዎ ETB 23,799.50 ነው። ማጣቀሻ FT26X9Y8Z7",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("CBE", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(1200.50, tx.amount, 0.01)
        assertEquals(23799.50, tx.balance!!, 0.01)
        assertEquals("1234", tx.accountSuffix)
        assertTrue(tx.id.contains("FT26X9Y8Z7"))
    }

    @Test
    fun `parse Amharic counterparty credit and debit`() {
        val creditTx = SmsParser.parseMessage(
            "BOA",
            "Amount of 3,500.00 Birr has been credited to your account from ABEBE KEBEDE. Available balance: 15,000.00",
            1700000000000L
        )
        assertNotNull(creditTx)
        assertEquals("Abebe Kebede", creditTx!!.title)

        val debitTx = SmsParser.parseMessage(
            "BOA",
            "የ100.00 ብር ዝውውር ለ SELAM PLC አስተላልፈዋል። ቀሪ ሒሳብዎ 900.00 ብር ነው።",
            1700000000000L
        )
        assertNotNull(debitTx)
        assertEquals("debit", debitTx!!.type)
        assertEquals("Selam Plc", debitTx.title)
    }

    @Test
    fun `parse Bunna Bank SMS withdrawal`() {
        val tx = SmsParser.parseMessage(
            "BunnaBank",
            "FOR BUNA BANK Dear Customer\nA Withdrawal of 5.00 ETB has been made from your account 363***222 on 08-10-2025 14:52:44 by ETHIO TELECOM TOP UP FOR 0920209609, your current balance is 95.00 ETB. Thank you.\nBunna Bank",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("BUN", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(5.0, tx.amount, 0.01)
        assertEquals(95.0, tx.balance!!, 0.01)
        assertEquals("222", tx.accountSuffix)
        // Counterparty name is captured up to (but not including) the phone-number digits
        assertEquals("Ethio Telecom Top Up For", tx.title)
    }

    // ── Siinqee Bank ─────────────────────────────────────────────────────

    @Test
    fun `parse Siinqee Bank debit SMS`() {
        val smsBody = """
            Dear YOHANIS SOLOMON TSEGAYE, Your account XXXXXX13470118 has been debited with ETB 2,009 Ref: M10OIPS260500022 on 19-FEB-2026 12:02:51. Available Balance is ETB 4,068.83. Thank you for banking with SIINQEE BANK.
            https://receipts.siinqeebank.com:871/generate/M10OIPS20500022
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "Siinqee",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("SIB", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(2009.0, tx.amount, 0.01)
        assertEquals(4068.83, tx.balance!!, 0.01)
        assertEquals("0118", tx.accountSuffix)
        assertEquals("tx-SIB-M10OIPS260500022", tx.id)
    }

    @Test
    fun `parse Telebirr transfer debit SMS`() {
        val smsBody = """
            Dear Yeshi 
            You have transferred ETB 30.00 to Medhin Gebremedhin (2519****4057) on 31/08/2026 21:05:42. Your transaction number is DHV6C15J1S. The service fee is  ETB 0.87 and  15% VAT on the service fee is ETB 0.13. Your current E-Money Account  balance is ETB 34.00. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DHV6C15J1S.

            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "telebirr",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("TEL", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(30.0, tx.amount, 0.01)
        assertEquals(34.0, tx.balance!!, 0.01)
        assertNull(tx.accountSuffix)
        assertEquals("Medhin Gebremedhin", tx.title)
        assertEquals("tx-TEL-DHV6C15J1S", tx.id)
    }

    @Test
    fun `parse Dashen Bank credit SMS with quoted account`() {
        val smsBody = """
            Dear Customer, your account '5293**011' is credited with ETB 100.00 on 30/06/2026 at 08:50:23 AM. Your current balance is ETB 100.00.
            Dashen Bank - Always one step ahead!.
            [Google Docs](https://forms.gle/VUEBDtQu2WFg8VBJ9)
            Survey on Dashen Bank Branch Service
            Dear customer,
            Dashen Bank is conducting this survey to betteR
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "DashenBank",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("DAS", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(100.0, tx.amount, 0.01)
        assertEquals(100.0, tx.balance!!, 0.01)
        assertEquals("011", tx.accountSuffix)
    }

    @Test
    fun `parse Bank of Abyssinia debit with receipt link`() {
        val smsBody = """
            Dear Bilal, your account 1*****88 was debited with ETB 750.00. Available Balance: ETB 60.24.
            Receipt: https://cs.bankofabyssinia.com/slip/?trx=TT2623279B4J69888
            Feedback: https://cs.bankofabyssinia.com/cs/?trx=DTT2623279B4J
            Link your Fayda: https://cs.bankofabyssinia.com/fayda_connect 
            For help, call 8397 (24/7 Toll-Free). Bank of Abyssinia.
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "BOA",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("BOA", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(750.0, tx.amount, 0.01)
        assertEquals(60.24, tx.balance!!, 0.01)
        assertEquals("88", tx.accountSuffix)
        assertEquals("tx-BOA-TT2623279B4J69888", tx.id)
    }

    @Test
    fun `parse Bank of Abyssinia fee debit`() {
        val smsBody = """
            Dear Bilal, your account 1******88 was debited with ETB 3.01 for the Mobile Banking Monthly Maintenance Fee, including 15% VAT and 5% Disaster Fund. Available balance: ETB 57.23. 
            Receipt: https://cs.bankofabyssinia.com/slip/?trx=FT26236PHDG969888 
            Link your Fayda: https://cs.bankofabyssinia.com/fayda_connect 
            For help, call 8397. Bank of Abyssinia.
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "BOA",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("BOA", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(3.01, tx.amount, 0.01)
        assertEquals(57.23, tx.balance!!, 0.01)
        assertEquals("88", tx.accountSuffix)
        assertEquals("Bank Fee", tx.title)
        assertEquals("tx-BOA-FT26236PHDG969888", tx.id)
    }

    @Test
    fun `parse CBE transfer credit with counterparty in parentheses`() {
        val smsBody = """
            Dear Bilal Essa Ebre You have received ETB 3,100.00 from account 1******0117 (Aminat Yimer Yesuf) to your account 1******8767. Your current balance is ETB4,314.50. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxGvPvkNE9eVhFuwW  for feedback: https://forms.gle/kGNGQpG3mQCCk3iD6
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "CBE",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("CBE", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(3100.0, tx.amount, 0.01)
        assertEquals(4314.50, tx.balance!!, 0.01)
        assertEquals("8767", tx.accountSuffix)
        assertEquals("Aminat Yimer Yesuf", tx.title)
    }

    @Test
    fun `parse Telebirr to bank transfer debit SMS`() {
        val smsBody = """
            Dear hirut
            You have transferred ETB 100.00 successfully from your telebirr account 251959028756 to Cooperative Bank of Oromia account number 1051000222221 on 25/08/2026 19:59:03. Your telebirr transaction number is DHP15VW8MX and your bank transaction number is TransactionId: FT262376TKD6. The service fee is  ETB 0.87 and  15% VAT on the service fee is ETB 0.13. Your current balance is ETB 737.39. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DHP15VW8MX
            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "telebirr",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("TEL", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(100.0, tx.amount, 0.01)
        assertEquals(737.39, tx.balance!!, 0.01)
        assertEquals("Bank Transfer", tx.title)
        assertEquals("tx-TEL-DHP15VW8MX", tx.id)
    }

    @Test
    fun `parse Awash Bank credit SMS with security warning footer`() {
        val smsBody = """
            Dear Customer, ETB 400 has been credited to your account from SEFI  SHEMSU on : 2026-08-28 18:51:57  with Txn ID: 260828185185437 . Your available balance is now ETB 453.13. Receipt  Link: https://awashpay.awashbank.com:8225/-2KGETB2PI5-5PBWBN. Contact center  8980.

            Alert: Awash Bank will never ask for your PIN, password, or OTP. Do not share your confidential information with anyone
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "Awash",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("AWA", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(400.0, tx.amount, 0.01)
        assertEquals(453.13, tx.balance!!, 0.01)
        assertEquals("Sefi Shemsu", tx.title)
        assertEquals("tx-AWA-260828185185437", tx.id)
    }

    @Test
    fun `parse CBE credit SMS with BranchReceipt URL`() {
        val smsBody = """
            Dear Mr Bilal your Account 1********8767 has been credited with ETB 750.00. Your Current Balance is ETB 5064.5. Thank you for Banking with CBE! for Reciept https://apps.cbe.com.et:100/BranchReceipt/FT26232DCWR1&75628767
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "CBE",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("CBE", tx!!.bankShortName)
        assertEquals("credit", tx.type)
        assertEquals(750.0, tx.amount, 0.01)
        assertEquals(5064.5, tx.balance!!, 0.01)
        assertEquals("8767", tx.accountSuffix)
        assertEquals("tx-CBE-FT26232DCWR1", tx.id)
    }

    @Test
    fun `parse Wegagen Bank withdrawal SMS`() {
        val smsBody = """
            Dear HABTAMU, 
            A Withdrawal of ETB 160 has been made from your account number 1*42630101 on 07 Jul, 2026 at 07:22 PM. Service charge of .72 and VAT(15%) of .04 with a total of 160.76 Your current balance is ETB 68.54. 
            Thank you for choosing Wegagen Bank. 
            For more information, contact our customer support toll free number 866. 
            https://transinfo.wegagenbanksc.com.et:8183/?id=061IPOU2618843981053DFTD
        """.trimIndent()

        val tx = SmsParser.parseMessage(
            "Wegagen",
            smsBody,
            1700000000000L
        )

        assertNotNull(tx)
        assertEquals("WEG", tx!!.bankShortName)
        assertEquals("debit", tx.type)
        assertEquals(160.0, tx.amount, 0.01)
        assertEquals(68.54, tx.balance!!, 0.01)
        assertEquals("0101", tx.accountSuffix)
        assertEquals("ATM Withdrawal", tx.title)
        assertEquals("tx-WEG-061IPOU2618843981053DFTD", tx.id)
    }

    // ── Unified Convergence & Amharic & Reversals & Promo Tests ───────────

    @Test
    fun `SMS and Notification for same event produce identical transaction ID`() {
        val smsBody = "Dear Mr Bilal your Account 1********8767 has been credited with ETB 750.00. Your Current Balance is ETB 5064.5. Ref: FT26232DCWR1"
        val institution = InstitutionCatalog.findBySmsSender("CBE")!!

        val smsTx = SmsParser.parseMessage("CBE", smsBody, 1700000000000L)
        val notifTx = SmsParser.parseBody(institution, smsBody, 1700000000000L, idPrefix = "notif", sourceKey = "com.cbe.ethiopia")

        assertNotNull(smsTx)
        assertNotNull(notifTx)
        assertEquals(smsTx!!.id, notifTx!!.id)
        assertEquals("tx-CBE-FT26232DCWR1", smsTx.id)
    }

    @Test
    fun `parse Amharic credit and debit SMS`() {
        val creditTx = SmsParser.parseMessage(
            "CBE",
            "የ500.00 ብር ገቢ ሆኗል ቀሪ ሂሳብዎ 1,500.00 ብር",
            1700000000000L
        )
        assertNotNull(creditTx)
        assertEquals("credit", creditTx!!.type)
        assertEquals(500.0, creditTx.amount, 0.01)
        assertEquals(1500.0, creditTx.balance!!, 0.01)

        val debitTx = SmsParser.parseMessage(
            "CBE",
            "ከአካውንትዎ 200.00 ብር ወጪ ሆኗል ቀሪ ሂሳብ 1,300.00",
            1700000000000L
        )
        assertNotNull(debitTx)
        assertEquals("debit", debitTx!!.type)
        assertEquals(200.0, debitTx.amount, 0.01)
        assertEquals(1300.0, debitTx.balance!!, 0.01)
    }

    @Test
    fun `parse completed reversal refund as credit`() {
        val tx = SmsParser.parseMessage(
            "CBE",
            "ETB 500.00 reversal refund has been credited to your account. Available Balance: ETB 2,500.00",
            1700000000000L
        )
        assertNotNull(tx)
        assertEquals("credit", tx!!.type)
        assertEquals(500.0, tx.amount, 0.01)
    }

    @Test
    fun `reject failed transaction attempt and promo SMS`() {
        val failedTx = SmsParser.parseMessage(
            "CBE",
            "Transaction of ETB 100.00 failed due to insufficient funds.",
            1700000000000L
        )
        assertNull(failedTx)

        val promoTx = SmsParser.parseMessage(
            "telebirr",
            "Win a car! Subscribe to our new promo now. Dial *804#",
            1700000000000L
        )
        assertNull(promoTx)
    }
}
