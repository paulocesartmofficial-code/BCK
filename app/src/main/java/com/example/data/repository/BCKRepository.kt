package com.example.data.repository

import android.content.Context
import com.example.data.local.BCKDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class BCKRepository(private val db: BCKDatabase) {

    val allClients: Flow<List<Client>> = db.clientDao().getAllClients()
    val allLoans: Flow<List<Loan>> = db.loanDao().getAllLoans()
    val allInstallments: Flow<List<Installment>> = db.installmentDao().getAllInstallments()
    val allReceipts: Flow<List<Receipt>> = db.receiptDao().getAllReceipts()
    val appSettings: Flow<AppSettings?> = db.settingsDao().getSettings()

    fun searchClients(query: String): Flow<List<Client>> = db.clientDao().searchClients(query)

    fun getLoansForClient(clientId: String): Flow<List<Loan>> = db.loanDao().getLoansForClient(clientId)

    fun getInstallmentsForLoan(loanId: String): Flow<List<Installment>> = db.installmentDao().getInstallmentsForLoan(loanId)

    suspend fun getClientById(id: String): Client? = db.clientDao().getClientById(id)

    suspend fun getLoanById(id: String): Loan? = db.loanDao().getLoanById(id)

    suspend fun getInstallmentById(id: String): Installment? = db.installmentDao().getInstallmentById(id)

    suspend fun insertClient(client: Client) = db.clientDao().insertClient(client)

    suspend fun updateClient(client: Client) = db.clientDao().updateClient(client)

    suspend fun deleteClient(client: Client) = db.clientDao().deleteClient(client)

    suspend fun saveSettings(settings: AppSettings) = db.settingsDao().saveSettings(settings)

    /**
     * Create a new Loan and automatically generate its schedule of installments.
     */
    suspend fun createLoan(
        client: Client,
        loanAmount: Double,
        interestRatePercent: Double,
        loanDate: String, // YYYY-MM-DD
        firstPaymentDate: String, // YYYY-MM-DD
        totalInstallments: Int,
        frequency: PaymentFrequency
    ): Loan {
        val totalInterest = loanAmount * (interestRatePercent / 100.0)
        val totalAmount = loanAmount + totalInterest
        val installmentAmount = totalAmount / totalInstallments

        val loanId = "LOAN-${System.currentTimeMillis()}"
        val loan = Loan(
            id = loanId,
            clientId = client.id,
            clientName = client.fullName,
            clientPhone = client.phone,
            loanAmount = loanAmount,
            interestRate = interestRatePercent,
            totalAmount = totalAmount,
            installmentAmount = installmentAmount,
            remainingBalance = totalAmount,
            loanDate = loanDate,
            firstPaymentDate = firstPaymentDate,
            totalInstallments = totalInstallments,
            frequency = frequency,
            status = LoanStatus.ACTIVE
        )

        db.loanDao().insertLoan(loan)

        // Generate Installments
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val startDate = try {
            dateFormat.parse(firstPaymentDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        cal.time = startDate

        val installments = mutableListOf<Installment>()
        for (i in 1..totalInstallments) {
            val dueDateStr = dateFormat.format(cal.time)
            installments.add(
                Installment(
                    id = "INST-$loanId-$i",
                    loanId = loanId,
                    clientId = client.id,
                    clientName = client.fullName,
                    installmentNumber = i,
                    dueDate = dueDateStr,
                    amount = installmentAmount,
                    status = InstallmentStatus.PENDING
                )
            )

            // Increment date based on frequency
            when (frequency) {
                PaymentFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                PaymentFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                PaymentFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            }
        }

        db.installmentDao().insertInstallments(installments)
        return loan
    }

    /**
     * Mark installment as paid, update loan balance, and generate receipt.
     */
    suspend fun markInstallmentPaid(
        installmentId: String,
        amountPaidOverride: Double? = null,
        context: Context
    ): Receipt? {
        val installment = db.installmentDao().getInstallmentById(installmentId) ?: return null
        val loan = db.loanDao().getLoanById(installment.loanId) ?: return null

        val actualAmountPaid = amountPaidOverride ?: installment.amount
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()

        val paymentDate = dateFormat.format(now)
        val paymentTime = timeFormat.format(now)

        // Update Installment
        installment.status = InstallmentStatus.PAID
        installment.paymentDate = paymentDate
        installment.paymentTime = paymentTime
        installment.amountPaid = actualAmountPaid
        db.installmentDao().updateInstallment(installment)

        // Update Loan Remaining Balance
        val newRemaining = (loan.remainingBalance - actualAmountPaid).coerceAtLeast(0.0)
        val newLoanStatus = if (newRemaining <= 0.05) LoanStatus.COMPLETED else LoanStatus.ACTIVE

        val updatedLoan = loan.copy(
            remainingBalance = newRemaining,
            status = newLoanStatus
        )
        db.loanDao().updateLoan(updatedLoan)

        // Calculate remaining unpaid installments for this loan
        val allLoanInsts = db.installmentDao().getInstallmentsForLoan(loan.id).firstOrNull() ?: emptyList()
        val remainingUnpaidCount = allLoanInsts.count { it.status != InstallmentStatus.PAID }

        // Create Receipt Record
        val receiptNo = "${System.currentTimeMillis().toString().takeLast(6)}"
        val settings = db.settingsDao().getSettingsSync() ?: AppSettings()

        val receipt = Receipt(
            id = "RCPT-$receiptNo",
            receiptNumber = receiptNo,
            loanId = loan.id,
            installmentId = installment.id,
            clientName = loan.clientName,
            clientPhone = loan.clientPhone,
            date = paymentDate,
            time = paymentTime,
            loanAmount = loan.loanAmount,
            installmentNumber = installment.installmentNumber,
            totalInstallments = loan.totalInstallments,
            amountPaid = actualAmountPaid,
            remainingBalance = newRemaining,
            status = "PAGO",
            thankYouMessage = "Obrigado pelo seu pagamento à ${settings.companyName}!",
            installmentDescription = "Parcela nº ${installment.installmentNumber}",
            installmentsRemaining = remainingUnpaidCount
        )

        db.receiptDao().insertReceipt(receipt)
        return receipt
    }

    /**
     * Mark multiple installments as paid at once, update loan balance, and generate a combined receipt.
     */
    suspend fun markMultipleInstallmentsPaid(
        installmentIds: List<String>,
        context: Context
    ): Receipt? {
        if (installmentIds.isEmpty()) return null

        val installmentsToPay = mutableListOf<Installment>()
        for (id in installmentIds) {
            val inst = db.installmentDao().getInstallmentById(id)
            if (inst != null && inst.status != InstallmentStatus.PAID) {
                installmentsToPay.add(inst)
            }
        }

        if (installmentsToPay.isEmpty()) return null

        val firstInst = installmentsToPay.first()
        val loan = db.loanDao().getLoanById(firstInst.loanId) ?: return null

        val totalPaid = installmentsToPay.sumOf { it.amount }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val now = Date()

        val paymentDate = dateFormat.format(now)
        val paymentTime = timeFormat.format(now)

        // Update Installments
        installmentsToPay.forEach { inst ->
            inst.status = InstallmentStatus.PAID
            inst.paymentDate = paymentDate
            inst.paymentTime = paymentTime
            inst.amountPaid = inst.amount
            db.installmentDao().updateInstallment(inst)
        }

        // Update Loan Remaining Balance
        val newRemaining = (loan.remainingBalance - totalPaid).coerceAtLeast(0.0)
        val newLoanStatus = if (newRemaining <= 0.05) LoanStatus.COMPLETED else LoanStatus.ACTIVE

        val updatedLoan = loan.copy(
            remainingBalance = newRemaining,
            status = newLoanStatus
        )
        db.loanDao().updateLoan(updatedLoan)

        // Calculate remaining unpaid installments for this loan
        val allLoanInsts = db.installmentDao().getInstallmentsForLoan(loan.id).firstOrNull() ?: emptyList()
        val remainingUnpaidCount = allLoanInsts.count { it.status != InstallmentStatus.PAID }

        // Format description
        val numbersSorted = installmentsToPay.map { it.installmentNumber }.sorted()
        val instDesc = if (numbersSorted.size == 1) {
            "Parcela nº ${numbersSorted.first()}"
        } else {
            "Parcelas nº ${numbersSorted.joinToString(", ")} (${numbersSorted.size} parcelas baixadas)"
        }

        // Create Receipt Record
        val receiptNo = "${System.currentTimeMillis().toString().takeLast(6)}"
        val settings = db.settingsDao().getSettingsSync() ?: AppSettings()

        val receipt = Receipt(
            id = "RCPT-$receiptNo",
            receiptNumber = receiptNo,
            loanId = loan.id,
            installmentId = installmentsToPay.joinToString(",") { it.id },
            clientName = loan.clientName,
            clientPhone = loan.clientPhone,
            date = paymentDate,
            time = paymentTime,
            loanAmount = loan.loanAmount,
            installmentNumber = numbersSorted.firstOrNull() ?: 1,
            totalInstallments = loan.totalInstallments,
            amountPaid = totalPaid,
            remainingBalance = newRemaining,
            status = "PAGO",
            thankYouMessage = "Obrigado pelo seu pagamento à ${settings.companyName}!",
            installmentDescription = instDesc,
            installmentsRemaining = remainingUnpaidCount
        )

        db.receiptDao().insertReceipt(receipt)
        return receipt
    }

    /**
     * Populate initial settings if DB is empty.
     * Note: We start with zero clients registered so the user begins with a clean slate.
     */
    suspend fun seedSampleDataIfEmpty() {
        val settings = appSettings.firstOrNull()
        if (settings == null) {
            db.settingsDao().saveSettings(
                AppSettings(
                    companyName = "BCK Serviços Financeiros",
                    defaultInterestRate = 12.0,
                    defaultFrequency = PaymentFrequency.MONTHLY,
                    currencySymbol = "R$",
                    receiptFooter = "BCK Gestão de Empréstimos • Transação Financeira Verificada"
                )
            )
        }
    }

    /**
     * Clear all clients, loans, installments, and receipts to reset to clean slate.
     */
    suspend fun clearAllData() {
        db.clientDao().getAllClients().firstOrNull()?.forEach { db.clientDao().deleteClient(it) }
    }
}
