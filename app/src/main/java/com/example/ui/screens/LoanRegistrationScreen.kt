package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Client
import com.example.data.model.PaymentFrequency
import com.example.ui.theme.DarkBluePrimary
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanRegistrationScreen(
    clients: List<Client>,
    preselectedClient: Client? = null,
    defaultInterestRate: Double = 10.0,
    defaultFrequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    currencySymbol: String = "$",
    onBack: () -> Unit,
    onCreateLoan: (client: Client, loanAmount: Double, interestRate: Double, loanDate: String, firstPaymentDate: String, totalInstallments: Int, frequency: PaymentFrequency) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = dateFormat.format(Date())

    var selectedClient by remember { mutableStateOf<Client?>(preselectedClient ?: clients.firstOrNull()) }
    var clientExpanded by remember { mutableStateOf(false) }

    var loanAmountStr by remember { mutableStateOf("1000") }
    var interestRateStr by remember { mutableStateOf(defaultInterestRate.toString()) }
    var installmentsCountStr by remember { mutableStateOf("5") }
    var selectedFrequency by remember { mutableStateOf(defaultFrequency) }
    var frequencyExpanded by remember { mutableStateOf(false) }

    var loanDate by remember { mutableStateOf(todayStr) }
    var firstPaymentDate by remember { mutableStateOf(todayStr) }

    // Automatic Calculations
    val loanAmount = loanAmountStr.toDoubleOrNull() ?: 0.0
    val interestRate = interestRateStr.toDoubleOrNull() ?: 0.0
    val installmentsCount = installmentsCountStr.toIntOrNull() ?: 1

    val totalInterest = loanAmount * (interestRate / 100.0)
    val totalWithInterest = loanAmount + totalInterest
    val installmentAmount = if (installmentsCount > 0) totalWithInterest / installmentsCount else 0.0

    // Schedule preview calculation
    val schedulePreview = remember(loanAmount, interestRate, installmentsCount, selectedFrequency, firstPaymentDate) {
        val cal = Calendar.getInstance()
        val startDate = try { dateFormat.parse(firstPaymentDate) ?: Date() } catch (e: Exception) { Date() }
        cal.time = startDate

        val list = mutableListOf<Pair<Int, String>>()
        for (i in 1..installmentsCount.coerceAtMost(30)) {
            list.add(i to dateFormat.format(cal.time))
            when (selectedFrequency) {
                PaymentFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                PaymentFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                PaymentFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cadastrar Novo Empréstimo",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Client Selector
                Text(
                    text = "Selecionar Cliente *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = clientExpanded,
                    onExpandedChange = { clientExpanded = !clientExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClient?.fullName ?: "Escolha um cliente...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = clientExpanded,
                        onDismissRequest = { clientExpanded = false }
                    ) {
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = { Text("${client.fullName} (${client.phone})") },
                                onClick = {
                                    selectedClient = client
                                    clientExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Loan Amount & Interest Rate
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = loanAmountStr,
                        onValueChange = { loanAmountStr = it },
                        label = { Text("Valor Empréstimo ($currencySymbol) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = interestRateStr,
                        onValueChange = { interestRateStr = it },
                        label = { Text("Taxa de Juros (%) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Number of Installments & Frequency
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = installmentsCountStr,
                        onValueChange = { installmentsCountStr = it },
                        label = { Text("Nº de Parcelas *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = frequencyExpanded,
                        onExpandedChange = { frequencyExpanded = !frequencyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedFrequency.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequência") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = frequencyExpanded,
                            onDismissRequest = { frequencyExpanded = false }
                        ) {
                            PaymentFrequency.values().forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq.displayName) },
                                    onClick = {
                                        selectedFrequency = freq
                                        frequencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dates
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = loanDate,
                        onValueChange = { loanDate = it },
                        label = { Text("Data Empréstimo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = firstPaymentDate,
                        onValueChange = { firstPaymentDate = it },
                        label = { Text("1º Vencimento") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Automatic Calculation Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = DarkBluePrimary)
                    Text(
                        text = "Detalhamento Calculado",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DarkBluePrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Valor Principal:", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "$currencySymbol${String.format("%.2f", loanAmount)}", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total de Juros ($interestRate%):", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "$currencySymbol${String.format("%.2f", totalInterest)}", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Valor Total a Pagar:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "$currencySymbol${String.format("%.2f", totalWithInterest)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = DarkBluePrimary)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Valor de Cada Parcela:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "$currencySymbol${String.format("%.2f", installmentAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule Preview Table
        Text(
            text = "Prévia do Cronograma de Parcelas",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                schedulePreview.forEach { (num, dueDate) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Parcela #$num",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Vencimento: $dueDate",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "$currencySymbol${String.format("%.2f", installmentAmount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = DarkBluePrimary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm Button
        Button(
            onClick = {
                val client = selectedClient
                if (client != null && loanAmount > 0 && installmentsCount > 0) {
                    onCreateLoan(
                        client,
                        loanAmount,
                        interestRate,
                        loanDate,
                        firstPaymentDate,
                        installmentsCount,
                        selectedFrequency
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkBluePrimary)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Confirmar e Emitir Empréstimo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}
