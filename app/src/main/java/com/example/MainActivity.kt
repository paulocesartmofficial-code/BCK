package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Client
import com.example.ui.components.BottomNavBar
import com.example.ui.components.LogoHeader
import com.example.ui.components.ReceiptModal
import com.example.ui.screens.*
import com.example.ui.theme.BCKTheme
import com.example.ui.theme.DarkBluePrimary
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.BCKViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: BCKViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
            val adminName by viewModel.adminName.collectAsStateWithLifecycle()
            val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

            val clients by viewModel.allClients.collectAsStateWithLifecycle()
            val loans by viewModel.allLoans.collectAsStateWithLifecycle()
            val installments by viewModel.allInstallments.collectAsStateWithLifecycle()
            val dashboardMetrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
            val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
            val generatedReceipt by viewModel.generatedReceipt.collectAsStateWithLifecycle()

            BCKTheme(darkTheme = isDarkMode) {
                if (!isLoggedIn) {
                    AuthScreen(
                        onLoginSuccess = { name, email ->
                            viewModel.loginAdmin(name, email)
                        }
                    )
                } else {
                    var selectedClientForLoan by remember { mutableStateOf<Client?>(null) }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            LogoHeader(
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { viewModel.setDarkMode(!isDarkMode) },
                                onSearchClick = { viewModel.navigateTo(AppTab.GlobalSearch) },
                                adminName = adminName,
                                companyName = appSettings.companyName,
                                onLogout = { viewModel.logout() }
                            )
                        },
                        bottomBar = {
                            BottomNavBar(
                                currentTab = currentTab,
                                onTabSelected = { tab -> viewModel.navigateTo(tab) }
                            )
                        },
                        floatingActionButton = {
                            if (currentTab !is AppTab.NewLoan) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        selectedClientForLoan = null
                                        viewModel.navigateTo(AppTab.NewLoan)
                                    },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.White,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Novo Empréstimo") },
                                    text = { Text("Novo Empréstimo", style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedVisibility(
                                visible = currentTab is AppTab.Dashboard,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                DashboardScreen(
                                    metrics = dashboardMetrics,
                                    installments = installments,
                                    currencySymbol = appSettings.currencySymbol,
                                    onNewClientClick = { viewModel.navigateTo(AppTab.Clients) },
                                    onNewLoanClick = {
                                        selectedClientForLoan = null
                                        viewModel.navigateTo(AppTab.NewLoan)
                                    },
                                    onTodayPaymentsClick = { viewModel.navigateTo(AppTab.Payments) },
                                    onReportsClick = { viewModel.navigateTo(AppTab.Reports) },
                                    onMarkPaid = { instId, context ->
                                        viewModel.markInstallmentPaid(instId, context)
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = currentTab is AppTab.Clients,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                ClientsScreen(
                                    clients = clients,
                                    onClientClick = { clientId ->
                                        viewModel.navigateTo(AppTab.ClientDetail(clientId))
                                    },
                                    onAddClient = { fullName, phone, whatsapp, cpf, address, city, state, zipCode, notes ->
                                        viewModel.createClient(fullName, phone, whatsapp, cpf, address, city, state, zipCode, notes)
                                    },
                                    onDeleteClient = { client ->
                                        viewModel.deleteClient(client)
                                    }
                                )
                            }

                            if (currentTab is AppTab.ClientDetail) {
                                val clientId = (currentTab as AppTab.ClientDetail).clientId
                                val client = clients.find { it.id == clientId }
                                if (client != null) {
                                    ClientDetailScreen(
                                        client = client,
                                        loans = loans,
                                        installments = installments,
                                        currencySymbol = appSettings.currencySymbol,
                                        onBack = { viewModel.navigateTo(AppTab.Clients) },
                                        onNewLoanForClient = { c ->
                                            selectedClientForLoan = c
                                            viewModel.navigateTo(AppTab.NewLoan)
                                        },
                                        onMarkPaid = { instId, context ->
                                            viewModel.markInstallmentPaid(instId, context)
                                        },
                                        onMarkMultiplePaid = { instIds, context ->
                                            viewModel.markMultipleInstallmentsPaid(instIds, context)
                                        }
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = currentTab is AppTab.NewLoan,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                LoanRegistrationScreen(
                                    clients = clients,
                                    preselectedClient = selectedClientForLoan,
                                    defaultInterestRate = appSettings.defaultInterestRate,
                                    defaultFrequency = appSettings.defaultFrequency,
                                    currencySymbol = appSettings.currencySymbol,
                                    onBack = { viewModel.navigateTo(AppTab.Dashboard) },
                                    onCreateLoan = { client, loanAmount, interestRate, loanDate, firstPaymentDate, totalInstallments, frequency ->
                                        viewModel.createLoan(
                                            client,
                                            loanAmount,
                                            interestRate,
                                            loanDate,
                                            firstPaymentDate,
                                            totalInstallments,
                                            frequency
                                        )
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = currentTab is AppTab.Payments,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                PaymentsScreen(
                                    installments = installments,
                                    currencySymbol = appSettings.currencySymbol,
                                    onMarkPaid = { instId, context ->
                                        viewModel.markInstallmentPaid(instId, context)
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = currentTab is AppTab.Reports,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                ReportsScreen(
                                    loans = loans,
                                    installments = installments,
                                    currencySymbol = appSettings.currencySymbol
                                )
                            }

                            AnimatedVisibility(
                                visible = currentTab is AppTab.Settings,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                SettingsScreen(
                                    appSettings = appSettings,
                                    isDarkMode = isDarkMode,
                                    adminName = adminName,
                                    adminEmail = userEmail,
                                    onToggleDarkMode = { viewModel.setDarkMode(it) },
                                    onSaveSettings = { updated -> viewModel.saveSettings(updated) },
                                    onLogout = { viewModel.logout() }
                                )
                            }

                            AnimatedVisibility(
                                visible = currentTab is AppTab.GlobalSearch,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                GlobalSearchScreen(
                                    clients = clients,
                                    loans = loans,
                                    installments = installments,
                                    currencySymbol = appSettings.currencySymbol,
                                    onBack = { viewModel.navigateTo(AppTab.Dashboard) },
                                    onClientClick = { clientId -> viewModel.navigateTo(AppTab.ClientDetail(clientId)) },
                                    onMarkPaid = { instId, context -> viewModel.markInstallmentPaid(instId, context) }
                                )
                            }
                        }
                    }

                    // Digital Receipt Modal
                    generatedReceipt?.let { receipt ->
                        ReceiptModal(
                            receipt = receipt,
                            currencySymbol = appSettings.currencySymbol,
                            companyName = appSettings.companyName,
                            onDismiss = { viewModel.clearGeneratedReceipt() }
                        )
                    }
                }
            }
        }
    }
}
