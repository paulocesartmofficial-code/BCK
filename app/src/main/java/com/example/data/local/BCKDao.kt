package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY fullName ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: String): Client?

    @Query("SELECT * FROM clients WHERE fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY fullName ASC")
    fun searchClients(query: String): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY createdAt DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getLoansForClient(clientId: String): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: String): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan)

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY dueDate ASC")
    fun getAllInstallments(): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun getInstallmentsForLoan(loanId: String): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE clientId = :clientId ORDER BY dueDate ASC")
    fun getInstallmentsForClient(clientId: String): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE dueDate = :date ORDER BY installmentNumber ASC")
    fun getInstallmentsForDate(date: String): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE id = :id LIMIT 1")
    suspend fun getInstallmentById(id: String): Installment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<Installment>)

    @Update
    suspend fun updateInstallment(installment: Installment)

    @Query("DELETE FROM installments WHERE loanId = :loanId")
    suspend fun deleteInstallmentsForLoan(loanId: String)
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY date DESC, time DESC")
    fun getAllReceipts(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    suspend fun getReceiptById(id: String): Receipt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: Receipt)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}
