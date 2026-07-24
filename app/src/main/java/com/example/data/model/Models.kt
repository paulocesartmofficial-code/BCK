package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentFrequency(val displayName: String) {
    DAILY("Diário"),
    WEEKLY("Semanal"),
    MONTHLY("Mensal")
}

enum class LoanStatus(val displayName: String) {
    ACTIVE("Ativo"),
    COMPLETED("Concluído"),
    CANCELLED("Cancelado")
}

enum class InstallmentStatus(val displayName: String) {
    PAID("Pago"),
    PENDING("Pendente"),
    OVERDUE("Atrasado")
}

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val whatsapp: String,
    val cpf: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val id: String,
    val clientId: String,
    val clientName: String,
    val clientPhone: String,
    val loanAmount: Double,
    val interestRate: Double,
    val totalAmount: Double,
    val installmentAmount: Double,
    val remainingBalance: Double,
    val loanDate: String, // YYYY-MM-DD
    val firstPaymentDate: String, // YYYY-MM-DD
    val totalInstallments: Int,
    val frequency: PaymentFrequency,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey val id: String,
    val loanId: String,
    val clientId: String,
    val clientName: String,
    val installmentNumber: Int,
    val dueDate: String, // YYYY-MM-DD
    val amount: Double,
    var status: InstallmentStatus,
    var paymentDate: String? = null, // YYYY-MM-DD
    var paymentTime: String? = null, // HH:mm:ss
    var amountPaid: Double = 0.0
)

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey val id: String,
    val receiptNumber: String,
    val loanId: String,
    val installmentId: String,
    val clientName: String,
    val clientPhone: String,
    val date: String,
    val time: String,
    val loanAmount: Double,
    val installmentNumber: Int,
    val totalInstallments: Int,
    val amountPaid: Double,
    val remainingBalance: Double,
    val status: String = "PAGO",
    val thankYouMessage: String = "Obrigado pelo seu pagamento!",
    val imagePath: String? = null,
    val installmentDescription: String = "",
    val installmentsRemaining: Int = 0
)

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val defaultInterestRate: Double = 10.0,
    val defaultFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val currencySymbol: String = "R$",
    val companyName: String = "BCK Serviços Financeiros",
    val companyLogoUri: String? = null,
    val receiptFooter: String = "BCK Gestão de Empréstimos • Comprovante Financeiro Oficial",
    val whatsappTemplate: String = "Olá {CLIENT_NAME}, segue o seu comprovante digital nº {RECEIPT_NO} ref. ao pagamento de {AMOUNT_PAID}. Saldo devedor: {REMAINING_BALANCE}. Obrigado!",
    val isDarkMode: Boolean = false
)
