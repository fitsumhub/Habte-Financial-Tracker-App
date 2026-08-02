package com.mobile.data

import android.content.Context
import android.net.Uri
import com.mobile.data.db.AccountEntity
import com.mobile.data.db.AppDatabase
import com.mobile.data.db.toDomain
import com.mobile.data.db.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FinanceRepository {

    private lateinit var db: AppDatabase
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _banks = MutableStateFlow<List<Bank>>(emptyList())
    val banks: StateFlow<List<Bank>> = _banks.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    // Set when a transaction notification is tapped, so HomeScreen can jump
    // straight to that transaction's categorize sheet once it's available, then cleared
    // by the observer after it's been consumed.
    private val _pendingTransactionId = MutableStateFlow<String?>(null)
    val pendingTransactionId: StateFlow<String?> = _pendingTransactionId.asStateFlow()

    fun setPendingTransaction(id: String?) {
        _pendingTransactionId.value = id
    }

    // Opens the on-device database and mirrors it into the StateFlows every screen
    // already observes, so transactions/banks/budgets survive process death and app
    // restarts instead of living only in memory. Safe to call more than once — only
    // the first call (from MainActivity.onCreate) takes effect.
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        db = AppDatabase.getInstance(context)
        scope.launch {
            db.transactionDao().observeAll().collect { entities ->
                _transactions.value = entities.map { it.toDomain() }
            }
        }
        scope.launch {
            db.bankDao().observeAllWithAccounts().collect { rows ->
                _banks.value = rows.map { it.toDomain() }
            }
        }
        scope.launch {
            db.budgetDao().observeAll().collect { entities ->
                _budgets.value = entities.map { it.toDomain() }
            }
        }
    }

    fun addBank(bank: Bank) {
        scope.launch { db.bankDao().insert(bank.toEntity()) }
    }

    fun removeBank(bankId: String) {
        scope.launch {
            val shortName = _banks.value.find { it.id == bankId }?.shortName
            db.bankDao().deleteById(bankId)
            if (shortName != null) db.transactionDao().deleteByBank(shortName)
        }
    }

    fun updateBankColors(bankId: String, colorFrom: String, colorTo: String) {
        scope.launch { db.bankDao().updateColors(bankId, colorFrom, colorTo) }
    }

    fun updateAccountNumber(accountId: String, accountNumber: String) {
        scope.launch { db.accountDao().updateAccountNumber(accountId, accountNumber) }
    }

    // Proper sign-out clears ALL persisted data, not just the in-memory view of it.
    fun clearAll() {
        scope.launch {
            db.transactionDao().deleteAll()
            db.accountDao().deleteAll()
            db.bankDao().deleteAll()
            db.budgetDao().deleteAll()
        }
    }

    fun addTransaction(transaction: Transaction) {
        scope.launch {
            // A transaction id is a deterministic hash of the SMS content (see
            // stableTransactionKey), so a genuinely resent SMS reproduces the exact same id —
            // if the user already deleted it as a duplicate, don't let it come back.
            if (db.deletedTransactionDao().getAllIds().contains(transaction.id)) return@launch
            val account = ensureBankAndAccount(transaction)
            if (account != null && transaction.balance != null) {
                db.accountDao().updateBalance(account.id, transaction.balance)
            }
            db.transactionDao().upsert(transaction.toEntity())
        }
    }

    // Used by the Duplicate Check tool to remove a resent SMS that slipped past the
    // id-based insert-ignore dedupe (see stableTransactionKey in SmsParser). Also records a
    // tombstone (see MIGRATION_3_4) so the *same* SMS re-appearing on a future syncHistoricalSms
    // pass — the original message never leaves the phone's SMS inbox — can't silently undo
    // this delete by re-inserting the identical id.
    fun deleteTransaction(transactionId: String) {
        scope.launch {
            db.transactionDao().deleteById(transactionId)
            db.deletedTransactionDao().markDeleted(
                com.mobile.data.db.DeletedTransactionEntity(transactionId, System.currentTimeMillis())
            )
        }
    }

    fun updateTransactionCategory(transactionId: String, newCategory: String) {
        scope.launch { db.transactionDao().updateCategory(transactionId, newCategory) }
    }

    // Reason is its own field, independent of category — saving a note never
    // overwrites whichever category chip was selected, and vice versa.
    fun updateTransactionReason(transactionId: String, newReason: String) {
        scope.launch { db.transactionDao().updateReason(transactionId, newReason) }
    }

    fun setBudget(period: String, category: String?, limit: Double) {
        scope.launch {
            val existing = _budgets.value.find { it.period == period && it.category == category }
            db.budgetDao().upsert(
                Budget(id = existing?.id ?: 0, period = period, category = category, limit = limit).toEntity()
            )
        }
    }

    fun deleteBudget(id: Long) {
        scope.launch { db.budgetDao().delete(id) }
    }

    // Creates the Bank/Account rows for a transaction's institution if they don't
    // already exist, so both the historical sync and the live SmsReceiver path always
    // have a matching account for the transaction to reference. Returns the account
    // the transaction belongs to (existing or newly created), or null if the SMS
    // sender didn't match a known institution.
    private suspend fun ensureBankAndAccount(tx: Transaction): AccountEntity? {
        val bankMetadata = Data.PRESET_BANKS.find { it.shortName == tx.bankShortName } ?: return null
        val institution = InstitutionCatalog.ALL.find { it.shortName == tx.bankShortName }
        val defaultAccountType = if (institution?.type == InstitutionType.DIGITAL_WALLET) {
            AccountType.MOBILE_WALLET
        } else {
            AccountType.SAVINGS
        }

        var bankEntity = db.bankDao().findByShortName(tx.bankShortName)
        if (bankEntity == null) {
            bankEntity = bankMetadata.copy(id = "${tx.bankShortName.lowercase()}_auto").toEntity()
            db.bankDao().insert(bankEntity)
        }

        val actualSuffix = tx.accountSuffix ?: "Main"
        val existingAccounts = db.accountDao().getForBank(bankEntity.id)
        val match = existingAccounts.find {
            (it.accountNumber != "Unknown" && it.accountNumber.takeLast(4) == actualSuffix) ||
                (it.accountNumber == "Unknown" && actualSuffix == "Main")
        }
        if (match != null) return match

        val newAccount = Account(
            id = "acc_${tx.bankShortName}_${actualSuffix}_${System.currentTimeMillis()}",
            bankId = bankEntity.id,
            accountNumber = if (tx.accountSuffix != null) "•••• ${tx.accountSuffix}" else "Unknown",
            label = if (tx.accountSuffix != null) "${tx.bankShortName} Account (*${tx.accountSuffix})" else "${tx.bankShortName} Main Account",
            balance = tx.balance ?: 0.0,
            currency = "ETB",
            type = defaultAccountType
        )
        val entity = newAccount.toEntity(bankEntity.id)
        db.accountDao().insert(entity)
        return entity
    }

    // Runs on Dispatchers.IO to prevent ANR on main thread. Re-parses the whole SMS
    // inbox but only ever inserts transactions that aren't already persisted, so a
    // manual re-sync can pick up messages the live receiver missed without ever
    // clobbering a category/reason the user already edited.
    suspend fun syncHistoricalSms(context: Context) = withContext(Dispatchers.IO) {
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("address", "body", "date"),
            null,
            null,
            "date DESC"
        )

        val newTransactions = mutableListOf<Transaction>()

        cursor?.use {
            val addressIndex = it.getColumnIndex("address")
            val bodyIndex = it.getColumnIndex("body")
            val dateIndex = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val address = it.getString(addressIndex) ?: continue
                val body = it.getString(bodyIndex) ?: continue
                val date = it.getLong(dateIndex)

                val parsed = SmsParser.parseMessage(address, body, date)
                if (parsed != null) {
                    newTransactions.add(parsed)
                }
            }
        }

        if (newTransactions.isEmpty()) return@withContext

        // The full SMS inbox is re-scanned on every call (there's no "since last sync"
        // filter above), and a transaction id is a deterministic hash of the SMS content —
        // so re-parsing the same message the user already deleted via Duplicate Check would
        // reproduce the exact same id and silently undo that delete via insertIgnoringExisting
        // (a conflict-free id, since the row was removed, is no longer "existing" to ignore).
        // Drop anything tombstoned before it ever reaches the insert.
        val deletedIds = db.deletedTransactionDao().getAllIds().toSet()
        val toInsert = if (deletedIds.isEmpty()) newTransactions else newTransactions.filter { it.id !in deletedIds }
        if (toInsert.isEmpty()) return@withContext

        // toInsert is ordered newest-first (SQL "date DESC"), so the first balance seen per
        // account during this pass is its most recent one.
        val calibratedAccounts = mutableSetOf<String>()
        toInsert.forEach { tx ->
            val account = ensureBankAndAccount(tx)
            if (account != null && tx.balance != null && calibratedAccounts.add(account.id)) {
                db.accountDao().updateBalance(account.id, tx.balance)
            }
        }

        db.transactionDao().insertIgnoringExisting(toInsert.map { it.toEntity() })
    }

    // Restores transactions from a previously exported JSON backup. Like the SMS sync
    // path, existing rows are left untouched (insert-ignore) so restoring a backup can
    // never overwrite edits made since the backup was taken.
    suspend fun restoreTransactions(items: List<Transaction>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        items.forEach { ensureBankAndAccount(it) }
        db.transactionDao().insertIgnoringExisting(items.map { it.toEntity() })
    }
}
