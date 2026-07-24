package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Installment
import com.example.data.model.InstallmentStatus
import com.example.ui.theme.DarkBluePrimary
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PendingAmber
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    installments: List<Installment>,
    currencySymbol: String = "$",
    onMarkPaid: (String, Context) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Today, 1: Overdue, 2: Pending, 3: Paid
    var searchQuery by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = dateFormat.format(Date())

    val filteredList = remember(installments, selectedTab, searchQuery) {
        var base = when (selectedTab) {
            0 -> installments.filter { it.dueDate == todayStr }
            1 -> installments.filter { it.status == InstallmentStatus.OVERDUE }
            2 -> installments.filter { it.status == InstallmentStatus.PENDING }
            3 -> installments.filter { it.status == InstallmentStatus.PAID }
            else -> installments
        }

        if (searchQuery.isNotBlank()) {
            base = base.filter {
                it.clientName.contains(searchQuery, ignoreCase = true) ||
                it.dueDate.contains(searchQuery, ignoreCase = true)
            }
        }
        base
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gestão de Pagamentos",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filtrar por nome do cliente ou data...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(
                    text = "Hoje (${installments.count { it.dueDate == todayStr }})",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(
                    text = "Atrasadas (${installments.count { it.status == InstallmentStatus.OVERDUE }})",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    color = OverdueRed
                )
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text(
                    text = "Pendentes (${installments.count { it.status == InstallmentStatus.PENDING }})",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                Text(
                    text = "Pagas (${installments.count { it.status == InstallmentStatus.PAID }})",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nenhum pagamento encontrado nesta seção.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredList, key = { it.id }) { installment ->
                    PaymentItemCard(
                        installment = installment,
                        currencySymbol = currencySymbol,
                        onMarkPaid = { onMarkPaid(installment.id, context) }
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentItemCard(
    installment: Installment,
    currencySymbol: String,
    onMarkPaid: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = installment.clientName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Parcela #${installment.installmentNumber} • Vencimento: ${installment.dueDate}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                // Status Pill
                val (statusBg, statusText, statusColor) = when (installment.status) {
                    InstallmentStatus.PAID -> Triple(Color(0xFFDCFCE7), "PAGO", Color(0xFF15803D))
                    InstallmentStatus.OVERDUE -> Triple(Color(0xFFFEE2E2), "ATRASADO", OverdueRed)
                    InstallmentStatus.PENDING -> Triple(Color(0xFFFEF3C7), "PENDENTE", PendingAmber)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Valor a Pagar",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "$currencySymbol${String.format("%.2f", installment.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkBluePrimary
                        )
                    )
                }

                if (installment.status == InstallmentStatus.PAID) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Pago em ${installment.paymentDate ?: ""} ${installment.paymentTime ?: ""}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Button(
                        onClick = onMarkPaid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Pagar Agora", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
