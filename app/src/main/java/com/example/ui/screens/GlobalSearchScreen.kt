package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Client
import com.example.data.model.Installment
import com.example.data.model.Loan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    clients: List<Client>,
    loans: List<Loan>,
    installments: List<Installment>,
    currencySymbol: String = "$",
    onBack: () -> Unit,
    onClientClick: (String) -> Unit,
    onMarkPaid: (String, Context) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val matchingClients = remember(query, clients) {
        if (query.isBlank()) emptyList()
        else clients.filter {
            it.fullName.contains(query, ignoreCase = true) ||
            it.phone.contains(query, ignoreCase = true) ||
            it.address.contains(query, ignoreCase = true) ||
            it.cpf.contains(query, ignoreCase = true)
        }
    }

    val matchingLoans = remember(query, loans) {
        if (query.isBlank()) emptyList()
        else loans.filter {
            it.clientName.contains(query, ignoreCase = true) ||
            it.clientPhone.contains(query, ignoreCase = true) ||
            it.id.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
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
                text = "Busca Global",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Pesquisar clientes, empréstimos, telefone, endereço...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Digite acima para pesquisar em todos os clientes, empréstimos e endereços.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else if (matchingClients.isEmpty() && matchingLoans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum registro encontrado para '$query'",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                if (matchingClients.isNotEmpty()) {
                    item {
                        Text(
                            text = "Clientes Encontrados (${matchingClients.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(matchingClients, key = { "cli-${it.id}" }) { client ->
                        ClientListItemCard(
                            client = client,
                            onClick = { onClientClick(client.id) },
                            onWhatsApp = {},
                            onCall = {},
                            onDelete = {}
                        )
                    }
                }

                if (matchingLoans.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Empréstimos Encontrados (${matchingLoans.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(matchingLoans, key = { "loan-${it.id}" }) { loan ->
                        LoanCardItem(loan = loan, currencySymbol = currencySymbol)
                    }
                }
            }
        }
    }
}
