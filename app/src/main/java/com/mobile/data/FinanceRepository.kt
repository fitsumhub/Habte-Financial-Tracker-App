package com.mobile.data

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

object FinanceRepository {
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _banks = MutableStateFlow<List<Bank>>(emptyList()) 
    val banks: StateFlow<List<Bank>> = _banks.asStateFlow()


    fun addBank(bank: Bank) {
        _banks.update { current -> current + bank }
    }

    fun removeBank(bankId: String) {
        val bank = _banks.value.find { it.id == bankId }
        val shortName = bank?.shortName
        _banks.update { current -> current.filter { it.id != bankId } }
        if (shortName != null) {
            _transactions.update { current ->
                current.filter { it.bankShortName != shortName }
            }
        }
    }

    // BUG FIX: Proper sign-out clears ALL data
    fun clearAll() {
        _banks.value = emptyList()
        _transactions.value = emptyList()
    }


    fun addTransaction(transaction: Transaction) {
        _transactions.update { current ->
            (listOf(transaction) + current).sortedByDescending { it.date }
        }
    }


    // BUG FIX: Now a suspend function running on Dispatchers.IO to prevent ANR on main thread
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
        
        if (newTransactions.isNotEmpty()) {
            _banks.update { currentBanks ->
                val updatedBanks = currentBanks.toMutableList()
                
                // Group transactions by bank and suffix to identify unique accounts
                val groupedByAccount = newTransactions.groupBy { it.bankShortName to it.accountSuffix }
                
                groupedByAccount.forEach { (key, txs) ->
                    val (shortName, suffix) = key
                    val bankMetadata = Data.PRESET_BANKS.find { it.shortName == shortName } ?: return@forEach
                    
                    var existingBank = updatedBanks.find { it.shortName == shortName }
                    
                    if (existingBank == null) {
                        // Create new bank if it doesn't exist
                        val newBank = bankMetadata.copy(
                            id = "${shortName.lowercase()}_auto",
                            accounts = mutableListOf()
                        )
                        updatedBanks.add(newBank)
                        existingBank = newBank
                    }
                    
                    // Check if this specific account (suffix) exists in the bank
                    val actualSuffix = suffix ?: "Main"
                    val hasAccount = existingBank.accounts.any { 
                        (it.accountNumber != "Unknown" && it.accountNumber.takeLast(4) == actualSuffix) || 
                        (it.accountNumber == "Unknown" && actualSuffix == "Main") 
                    }
                    
                    if (!hasAccount) {
                        val newAccount = Account(
                            id = "acc_${shortName}_${actualSuffix}_${System.currentTimeMillis()}",
                            bankId = existingBank.id,
                            accountNumber = if (suffix != null) "•••• $suffix" else "Unknown",
                            label = if (suffix != null) "$shortName Account (*$suffix)" else "$shortName Main Account",
                            balance = txs.mapNotNull { it.balance }.firstOrNull() ?: 0.0,
                            currency = "ETB",
                            type = AccountType.SAVINGS
                        )
                        
                        val bankIndex = updatedBanks.indexOfFirst { it.shortName == shortName }
                        updatedBanks[bankIndex] = existingBank.copy(
                            accounts = existingBank.accounts + newAccount
                        )
                    }
                }
                updatedBanks
            }

            // Now that banks/accounts are created, set all transactions
            _transactions.value = newTransactions
            
            // Final balance calibration for all accounts
            _banks.update { banks ->
                banks.map { bank ->
                    val bankTx = newTransactions.filter { it.bankShortName == bank.shortName }
                    val newAccounts = bank.accounts.map { account ->
                        val accountSuffix = if (account.accountNumber != "Unknown") account.accountNumber.takeLast(4) else null
                        val matchedTx = bankTx.filter { 
                            (accountSuffix == null && it.accountSuffix == null) || (it.accountSuffix == accountSuffix) || (accountSuffix != null && it.accountSuffix == null)
                        }
                        val latestBalance = matchedTx.mapNotNull { it.balance }.firstOrNull()
                        if (latestBalance != null) account.copy(balance = latestBalance) else account
                    }
                    bank.copy(accounts = newAccounts)
                }
            }
        }




    }
}
