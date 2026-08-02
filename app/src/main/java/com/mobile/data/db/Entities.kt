package com.mobile.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.mobile.data.Account
import com.mobile.data.AccountType
import com.mobile.data.Bank
import com.mobile.data.Budget
import com.mobile.data.Certificate
import com.mobile.data.CertificatePeriod
import com.mobile.data.InstitutionCatalog
import com.mobile.data.PaymentReminder
import com.mobile.data.ReminderRepeat
import com.mobile.data.Transaction

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val type: String,
    val bankShortName: String,
    val category: String,
    val balance: Double?,
    val accountSuffix: String?,
    val time: String,
    val reason: String
)

@Entity(tableName = "banks")
data class BankEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val colorFrom: String,
    val colorTo: String,
    val logoText: String,
    val logoResId: Int?,
    val domain: String?
)

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = BankEntity::class,
            parentColumns = ["id"],
            childColumns = ["bankId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bankId")]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val bankId: String,
    val accountNumber: String,
    val label: String,
    val balance: Double,
    val currency: String,
    val type: String
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: String,
    val category: String?,
    @ColumnInfo(name = "limit_amount") val limitAmount: Double
)

data class BankWithAccounts(
    @Embedded val bank: BankEntity,
    @Relation(parentColumn = "id", entityColumn = "bankId")
    val accounts: List<AccountEntity>
)

fun TransactionEntity.toDomain() = Transaction(
    id = id, title = title, amount = amount, date = date, type = type,
    bankShortName = bankShortName, category = category, balance = balance,
    accountSuffix = accountSuffix, time = time, reason = reason
)

fun Transaction.toEntity() = TransactionEntity(
    id = id, title = title, amount = amount, date = date, type = type,
    bankShortName = bankShortName, category = category, balance = balance,
    accountSuffix = accountSuffix, time = time, reason = reason
)

fun AccountEntity.toDomain() = Account(
    id = id, bankId = bankId, accountNumber = accountNumber, label = label,
    balance = balance, currency = currency, type = AccountType.valueOf(type)
)

fun Account.toEntity(bankId: String) = AccountEntity(
    id = id, bankId = bankId, accountNumber = accountNumber, label = label,
    balance = balance, currency = currency, type = type.name
)

// logoResId/domain are re-resolved from the live catalog rather than trusting the
// persisted columns: logoResId is a raw R.drawable int baked in at insert time, and
// Android resource IDs aren't guaranteed stable across rebuilds, so a value stored
// before a catalog/asset update (or a previous APK build) would otherwise point at
// the wrong drawable or stay null forever. colorFrom/colorTo/logoText stay persisted
// since colors are user-customizable via updateBankColors.
fun BankWithAccounts.toDomain(): Bank {
    val catalogEntry = InstitutionCatalog.ALL.find { it.id == bank.id }
    return Bank(
        id = bank.id, name = bank.name, shortName = bank.shortName,
        accounts = accounts.map { it.toDomain() }, colorFrom = bank.colorFrom,
        colorTo = bank.colorTo, logoText = bank.logoText,
        logoResId = catalogEntry?.logoResId ?: bank.logoResId,
        domain = catalogEntry?.domain ?: bank.domain
    )
}

fun Bank.toEntity() = BankEntity(
    id = id, name = name, shortName = shortName, colorFrom = colorFrom,
    colorTo = colorTo, logoText = logoText, logoResId = logoResId, domain = domain
)

fun BudgetEntity.toDomain() = Budget(id = id, period = period, category = category, limit = limitAmount)

fun Budget.toEntity() = BudgetEntity(id = id, period = period, category = category, limitAmount = limit)

@Entity(tableName = "payment_reminders")
data class PaymentReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val category: String,
    val amount: Double,
    val payee: String,
    val dueDateMillis: Long,
    val repeat: String,
    val daysBefore: Int,
    val enabled: Boolean,
    val lastPaidCycle: String
)

fun PaymentReminderEntity.toDomain() = PaymentReminder(
    id = id, label = label, category = category, amount = amount, payee = payee,
    dueDateMillis = dueDateMillis, repeat = ReminderRepeat.valueOf(repeat),
    daysBefore = daysBefore, enabled = enabled, lastPaidCycle = lastPaidCycle
)

fun PaymentReminder.toEntity() = PaymentReminderEntity(
    id = id, label = label, category = category, amount = amount, payee = payee,
    dueDateMillis = dueDateMillis, repeat = repeat.name,
    daysBefore = daysBefore, enabled = enabled, lastPaidCycle = lastPaidCycle
)

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: String,
    val periodLabel: String,
    val userName: String,
    val photoPath: String?,
    val achievementTitle: String,
    val achievementSubtitle: String,
    val generatedAtMillis: Long,
    val imagePath: String
)

fun CertificateEntity.toDomain() = Certificate(
    id = id, period = CertificatePeriod.valueOf(period), periodLabel = periodLabel,
    userName = userName, photoPath = photoPath, achievementTitle = achievementTitle,
    achievementSubtitle = achievementSubtitle, generatedAtMillis = generatedAtMillis,
    imagePath = imagePath
)

fun Certificate.toEntity() = CertificateEntity(
    id = id, period = period.name, periodLabel = periodLabel, userName = userName,
    photoPath = photoPath, achievementTitle = achievementTitle, achievementSubtitle = achievementSubtitle,
    generatedAtMillis = generatedAtMillis, imagePath = imagePath
)

// Tombstone marking a transaction id the user deliberately deleted (Duplicate Check) — see
// MIGRATION_3_4 in AppDatabase for why this exists and how it's consulted before a resync.
@Entity(tableName = "deleted_transactions")
data class DeletedTransactionEntity(
    @PrimaryKey val transactionId: String,
    val deletedAtMillis: Long
)
