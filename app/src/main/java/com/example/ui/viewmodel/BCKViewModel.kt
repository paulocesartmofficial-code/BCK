package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BCKDatabase
import com.example.data.model.*
import com.example.data.repository.BCKRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class AppTab {
    object Dashboard : AppTab()
    object Clients : AppTab()
    object Loans : AppTab()
    object Payments : AppTab()
    object Reports : AppTab()
    object Settings : AppTab()
    object GlobalSearch : AppTab()
    object NewLoan : AppTab()
    object NewClient : AppTab()
    data class ClientDetail(val clientId: String) : AppTab()
    data class LoanDetail(val loanId: String) : AppTab()
}

data class DashboardMetrics(
    val totalActiveLoans: Int = 0,
    val totalClients: Int = 0,
    val amountLent: Double = 0.0,
    val amountCollectedToday: Double = 0.0,
    val pendingPaymentsToday: Double = 0.0,
    val overduePaymentsAmount: Double = 0.0,
    val overdueCount: Int = 0,
    val expectedMonthlyRevenue: Double = 0.0
)

data class ReportMetrics(
    val rangeName: String = "Month",
    val amountCollected: Double = 0.0,
    val amountLent: Double = 0.0,
    val pendingPayments: Double = 0.0,
    val overduePayments: Double = 0.0,
    val profitFromInterest: Double = 0.0
)

class BCKViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BCKDatabase.getInstance(application)
    val repository = BCKRepository(db)

    // Navigation state
    private val _currentTab = MutableStateFlow<AppTab>(AppTab.Dashboard)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Auth & Admin State
    val isLoggedIn = MutableStateFlow(true)
    val adminName = MutableStateFlow("Romario Guimarães")
    val adminRole = MutableStateFlow("Administrador Principal")
    val userEmail = MutableStateFlow("romario.guimaraes@bck.com")

    fun loginAdmin(name: String, email: String) {
        adminName.value = name
        userEmail.value = email
        isLoggedIn.value = true
    }

    fun logout() {
        isLoggedIn.value = false
    }

    // Theme state
    val isDarkMode = MutableStateFlow(false)

    // Active Modal Receipt
    private val _generatedReceipt = MutableStateFlow<Receipt?>(null)
    val generatedReceipt: StateFlow<Receipt?> = _generatedReceipt.asStateFlow()

    // Search Query
    val globalSearchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }

        // Sync dark mode setting
        viewModelScope.launch {
            repository.appSettings.collect { settings ->
                if (settings != null) {
                    isDarkMode.value = settings.isDarkMode
                }
            }
        }
    }

    fun navigateTo(tab: AppTab) {
        _currentTab.value = tab
    }

    fun clearGeneratedReceipt() {
        _generatedReceipt.value = null
    }

    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        viewModelScope.launch {
            val currentSettings = repository.appSettings.firstOrNull() ?: AppSettings()
            repository.saveSettings(currentSettings.copy(isDarkMode = enabled))
        }
    }

    // Dynamic Streams
    val allClients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLoans: StateFlow<List<Loan>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInstallments: StateFlow<List<Installment>> = repository.allInstallments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReceipts: StateFlow<List<Receipt>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettings> = repository.appSettings
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allClients,
        allLoans,
        allInstallments
    ) { clients, loans, installments ->
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())

        val activeLoans = loans.filter { it.status == LoanStatus.ACTIVE }
        val totalLent = loans.sumOf { it.loanAmount }

        val todayInsts = installments.filter { it.dueDate == todayStr }
        val collectedToday = installments
            .filter { it.paymentDate == todayStr && it.status == InstallmentStatus.PAID }
            .sumOf { it.amountPaid }

        val pendingToday = todayInsts
            .filter { it.status == InstallmentStatus.PENDING }
            .sumOf { it.amount }

        val overdueInsts = installments.filter { it.status == InstallmentStatus.OVERDUE }
        val overdueAmt = overdueInsts.sumOf { it.amount }

        val monthlyRev = activeLoans.sumOf {
            when (it.frequency) {
                PaymentFrequency.DAILY -> it.installmentAmount * 30
                PaymentFrequency.WEEKLY -> it.installmentAmount * 4
                PaymentFrequency.MONTHLY -> it.installmentAmount
            }
        }

        DashboardMetrics(
            totalActiveLoans = activeLoans.size,
            totalClients = clients.size,
            amountLent = totalLent,
            amountCollectedToday = collectedToday,
            pendingPaymentsToday = pendingToday,
            overduePaymentsAmount = overdueAmt,
            overdueCount = overdueInsts.size,
            expectedMonthlyRevenue = monthlyRev
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Actions
    fun createClient(
        fullName: String,
        phone: String,
        whatsapp: String,
        cpf: String,
        address: String,
        city: String,
        state: String,
        zipCode: String,
        notes: String
    ) {
        viewModelScope.launch {
            val newClient = Client(
                id = "CLI-${System.currentTimeMillis().toString().takeLast(6)}",
                fullName = fullName,
                phone = phone,
                whatsapp = whatsapp,
                cpf = cpf,
                address = address,
                city = city,
                state = state,
                zipCode = zipCode,
                notes = notes
            )
            repository.insertClient(newClient)
            _currentTab.value = AppTab.Clients
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    fun createLoan(
        client: Client,
        loanAmount: Double,
        interestRate: Double,
        loanDate: String,
        firstPaymentDate: String,
        totalInstallments: Int,
        frequency: PaymentFrequency
    ) {
        viewModelScope.launch {
            repository.createLoan(
                client = client,
                loanAmount = loanAmount,
                interestRatePercent = interestRate,
                loanDate = loanDate,
                firstPaymentDate = firstPaymentDate,
                totalInstallments = totalInstallments,
                frequency = frequency
            )
            _currentTab.value = AppTab.Loans
        }
    }

    fun markInstallmentPaid(installmentId: String, context: Context) {
        viewModelScope.launch {
            val receipt = repository.markInstallmentPaid(installmentId = installmentId, context = context)
            if (receipt != null) {
                _generatedReceipt.value = receipt
            }
        }
    }

    fun markMultipleInstallmentsPaid(installmentIds: List<String>, context: Context) {
        viewModelScope.launch {
            val receipt = repository.markMultipleInstallmentsPaid(installmentIds = installmentIds, context = context)
            if (receipt != null) {
                _generatedReceipt.value = receipt
            }
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    fun clearAllClientsData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
