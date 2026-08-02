package com.mobile.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    // Ignores rows that already exist so a re-sync of the SMS inbox never clobbers
    // a category/reason the user already edited on a previously-imported transaction.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: String, category: String)

    @Query("UPDATE transactions SET reason = :reason WHERE id = :id")
    suspend fun updateReason(id: String, reason: String)

    @Query("DELETE FROM transactions WHERE bankShortName = :shortName")
    suspend fun deleteByBank(shortName: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface BankDao {
    @androidx.room.Transaction
    @Query("SELECT * FROM banks")
    fun observeAllWithAccounts(): Flow<List<BankWithAccounts>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bank: BankEntity)

    @Query("SELECT * FROM banks WHERE shortName = :shortName LIMIT 1")
    suspend fun findByShortName(shortName: String): BankEntity?

    @Query("UPDATE banks SET colorFrom = :colorFrom, colorTo = :colorTo WHERE id = :id")
    suspend fun updateColors(id: String, colorFrom: String, colorTo: String)

    @Query("DELETE FROM banks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM banks")
    suspend fun deleteAll()
}

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE bankId = :bankId")
    suspend fun getForBank(bankId: String): List<AccountEntity>

    @Query("UPDATE accounts SET balance = :balance WHERE id = :id")
    suspend fun updateBalance(id: String, balance: Double)

    // Bank SMS only ever reveals the last few digits ("...1234"), so the auto-detected
    // accountNumber is a masked placeholder — this is what lets the user fill in their real,
    // full account number so it can actually be shared with customers. See AccountDetailSheet.
    @Query("UPDATE accounts SET accountNumber = :accountNumber WHERE id = :id")
    suspend fun updateAccountNumber(id: String, accountNumber: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface PaymentReminderDao {
    @Query("SELECT * FROM payment_reminders ORDER BY dueDateMillis ASC")
    fun observeAll(): Flow<List<PaymentReminderEntity>>

    @Query("SELECT * FROM payment_reminders WHERE enabled = 1")
    suspend fun getEnabled(): List<PaymentReminderEntity>

    @Query("SELECT * FROM payment_reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PaymentReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: PaymentReminderEntity): Long

    @Query("UPDATE payment_reminders SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE payment_reminders SET dueDateMillis = :dueDateMillis WHERE id = :id")
    suspend fun updateDueDate(id: Long, dueDateMillis: Long)

    @Query("UPDATE payment_reminders SET lastPaidCycle = :cycle WHERE id = :id")
    suspend fun markPaid(id: Long, cycle: String)

    @Query("DELETE FROM payment_reminders WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DeletedTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markDeleted(entity: DeletedTransactionEntity)

    @Query("SELECT transactionId FROM deleted_transactions")
    suspend fun getAllIds(): List<String>
}

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates ORDER BY generatedAtMillis DESC")
    fun observeAll(): Flow<List<CertificateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(certificate: CertificateEntity): Long

    @Query("DELETE FROM certificates WHERE id = :id")
    suspend fun delete(id: Long)
}
