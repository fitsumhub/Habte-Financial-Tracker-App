package com.mobile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, BankEntity::class, AccountEntity::class, BudgetEntity::class, PaymentReminderEntity::class, CertificateEntity::class, DeletedTransactionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun bankDao(): BankDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun paymentReminderDao(): PaymentReminderDao
    abstract fun certificateDao(): CertificateDao
    abstract fun deletedTransactionDao(): DeletedTransactionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // Additive-only — creates the payment_reminders table without touching any
        // existing rows, so upgrading never loses a user's synced transactions/accounts.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS payment_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        category TEXT NOT NULL,
                        amount REAL NOT NULL,
                        payee TEXT NOT NULL,
                        dueDateMillis INTEGER NOT NULL,
                        repeat TEXT NOT NULL,
                        daysBefore INTEGER NOT NULL,
                        enabled INTEGER NOT NULL,
                        lastPaidCycle TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Additive-only, same as MIGRATION_1_2 — creates the certificates table (Achievement
        // Certificates feature) without touching any existing rows.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS certificates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        period TEXT NOT NULL,
                        periodLabel TEXT NOT NULL,
                        userName TEXT NOT NULL,
                        photoPath TEXT,
                        achievementTitle TEXT NOT NULL,
                        achievementSubtitle TEXT NOT NULL,
                        generatedAtMillis INTEGER NOT NULL,
                        imagePath TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Additive-only — a tombstone table so a transaction the user deliberately deleted
        // (via Duplicate Check) can never be silently re-inserted by a later SMS re-sync.
        // Without this, syncHistoricalSms re-parses the *entire* SMS inbox every time it
        // runs (the original SMS never leaves the phone's inbox just because the derived
        // transaction was deleted) and insertIgnoringExisting only skips a row if one with
        // the same id *already exists* — once deleted, that id is free again, so the exact
        // "duplicate" the user just removed would silently come back on the next sync.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS deleted_transactions (
                        transactionId TEXT PRIMARY KEY NOT NULL,
                        deletedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habte.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
