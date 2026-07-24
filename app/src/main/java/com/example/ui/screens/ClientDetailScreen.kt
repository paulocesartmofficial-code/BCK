package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Installment
import com.example.data.model.InstallmentStatus
import com.example.data.model.Loan
import com.example.ui.theme.DarkBluePrimary
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PendingAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    client: Client,
    loans: List<Loan>,
    installments: List<Installment>,
    currencySymbol: String = "R$",
    onBack: () -> Unit,
    onNewLoanForClient: (Client) -> Unit,
    onMarkPaid: (String, Context) -> Unit,
    onMarkMultiplePaid: ((List<String>, Context) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Atrasos/Faltas, 1: A Vencer, 2: Em Dia/Pagas, 3: Empréstimos
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val clientLoans = remember(loans, client) { loans.filter { it.clientId == client.id } }
    val clientInstallments = remember(installments, client) { installments.filter { it.clientId == client.id } }

    val overdueInsts = remember(clientInstallments) { clientInstallments.filter { it.status == InstallmentStatus.OVERDUE } }
    val pendingInsts = remember(clientInstallments) { clientInstallments.filter { it.status == InstallmentStatus.PENDING } }
    val paidInsts = remember(clientInstallments) { clientInstallments.filter { it.status == InstallmentStatus.PAID } }

    val overdueSum = remember(overdueInsts) { overdueInsts.sumOf { it.amount } }
    val pendingSum = remember(pendingInsts) { pendingInsts.sumOf { it.amount } }
    val paidSum = remember(paidInsts) { paidInsts.sumOf { it.amountPaid } }

    fun toggleSelect(id: String) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    }

    fun sendWhatsAppCollectionMessage(specificInst: Installment? = null) {
        val cleanPhone = client.whatsapp.replace(Regex("[^0-9]"), "")
        val msg = if (specificInst != null) {
            """
                Olá, *${client.fullName}*!
                
                Lembrete de cobrança da Parcela #${specificInst.installmentNumber} no valor de *$currencySymbol${String.format("%.2f", specificInst.amount)}* com vencimento em *${specificInst.dueDate}*.
                
                Por favor, solicite a chave PIX para efetuar o pagamento.
                
                _BCK Serviços Financeiros_
            """.trimIndent()
        } else {
            """
                Olá, *${client.fullName}*!
                
                *Resumo do seu Empréstimo na BCK Finanças:*
                
                🔴 *Em Atraso / Faltas*: ${overdueInsts.size} parcela(s) ($currencySymbol${String.format("%.2f", overdueSum)})
                🟡 *A Vencer*: ${pendingInsts.size} parcela(s) ($currencySymbol${String.format("%.2f", pendingSum)})
                🟢 *Pagas*: ${paidInsts.size} parcela(s)
                
                Favor entrar em contato para envio do comprovante ou chave PIX de quitação. Obrigado!
            """.trimIndent()
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o WhatsApp.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendWhatsAppSelectedMessage() {
        val selectedList = clientInstallments.filter { selectedIds.contains(it.id) }.sortedBy { it.installmentNumber }
        if (selectedList.isEmpty()) return
        val instNumbers = selectedList.map { "#${it.installmentNumber}" }.joinToString(", ")
        val totalVal = selectedList.sumOf { it.amount }
        val cleanPhone = client.whatsapp.replace(Regex("[^0-9]"), "")

        val msg = """
            Olá, *${client.fullName}*!
            
            Lembrete de cobrança das parcelas selecionadas (*$instNumbers*).
            
            • *Total a Pagar:* $currencySymbol${String.format("%.2f", totalVal)}
            • *Quantidade:* ${selectedList.size} parcela(s)
            
            Por favor, solicite a chave PIX para efetuar o pagamento.
            
            _BCK Serviços Financeiros_
        """.trimIndent()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o WhatsApp.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = client.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Aba do Cliente • Faltas, Seleção & Histórico",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile & Quick Collection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val initials = client.fullName.split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString("")
                            .uppercase()

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(DarkBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = client.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Tel: ${client.phone}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                if (client.cpf.isNotEmpty()) {
                                    Text(
                                        text = "CPF: ${client.cpf}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Row: WhatsApp Collection & New Loan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { sendWhatsAppCollectionMessage(null) },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Cobrar no WhatsApp", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = { onNewLoanForClient(client) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "+ Empréstimo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Organized Status Metric Cards Row (Faltas / Atrasos, Pendentes, Pagas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Overdue Badges (Faltas)
                Card(
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTab == 0) OverdueRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (selectedTab == 0) 2.dp else 1.dp,
                        if (overdueInsts.isNotEmpty()) OverdueRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(OverdueRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Atrasos / Faltas",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = OverdueRed)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${overdueInsts.size} parcelas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "$currencySymbol${String.format("%.2f", overdueSum)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = OverdueRed)
                        )
                    }
                }

                // Pending Badges (A Vencer)
                Card(
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTab == 1) PendingAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (selectedTab == 1) 2.dp else 1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(PendingAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "A Vencer",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PendingAmber)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${pendingInsts.size} parcelas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "$currencySymbol${String.format("%.2f", pendingSum)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                // Paid Badges (Em Dia)
                Card(
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTab == 2) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (selectedTab == 2) 2.dp else 1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Em Dia / Pagas",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${paidInsts.size} parcelas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "$currencySymbol${String.format("%.2f", paidSum)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = EmeraldGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Organized Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🔴 Atrasos (${overdueInsts.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🟡 A Vencer (${pendingInsts.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🟢 Pagas (${paidInsts.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("📄 Empréstimos (${clientLoans.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selection Toolbar for Unpaid Tabs
            if (selectedTab == 0 || selectedTab == 1) {
                val currentTabInsts = if (selectedTab == 0) overdueInsts else pendingInsts
                val allTabIds = currentTabInsts.map { it.id }.toSet()
                val areAllTabSelected = allTabIds.isNotEmpty() && selectedIds.containsAll(allTabIds)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedIds = if (areAllTabSelected) {
                                selectedIds - allTabIds
                            } else {
                                selectedIds + allTabIds
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (areAllTabSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (areAllTabSelected) "Desmarcar Esta Aba" else "Selecionar Todas desta Aba",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text(
                                text = "Limpar Seleção (${selectedIds.size})",
                                style = MaterialTheme.typography.labelMedium.copy(color = OverdueRed, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Tab Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = if (selectedIds.isNotEmpty()) 160.dp else 90.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Overdue / Faltas
                        if (overdueInsts.isEmpty()) {
                            item {
                                EmptyStatusCard(
                                    title = "Nenhum atraso ou falta registrada!",
                                    description = "Este cliente está completamente em dia com o pagamento das suas parcelas.",
                                    icon = Icons.Default.CheckCircle,
                                    color = EmeraldGreen
                                )
                            }
                        } else {
                            items(overdueInsts, key = { it.id }) { inst ->
                                OrganizedInstallmentCard(
                                    installment = inst,
                                    currencySymbol = currencySymbol,
                                    isSelected = selectedIds.contains(inst.id),
                                    onToggleSelect = { toggleSelect(inst.id) },
                                    onMarkPaid = { onMarkPaid(inst.id, context) },
                                    onSendWhatsApp = { sendWhatsAppCollectionMessage(inst) }
                                )
                            }
                        }
                    }
                    1 -> { // Pending
                        if (pendingInsts.isEmpty()) {
                            item {
                                EmptyStatusCard(
                                    title = "Nenhuma parcela a vencer.",
                                    description = "Não há parcelas futuras pendentes para este cliente.",
                                    icon = Icons.Default.Info,
                                    color = DarkBluePrimary
                                )
                            }
                        } else {
                            items(pendingInsts, key = { it.id }) { inst ->
                                OrganizedInstallmentCard(
                                    installment = inst,
                                    currencySymbol = currencySymbol,
                                    isSelected = selectedIds.contains(inst.id),
                                    onToggleSelect = { toggleSelect(inst.id) },
                                    onMarkPaid = { onMarkPaid(inst.id, context) },
                                    onSendWhatsApp = { sendWhatsAppCollectionMessage(inst) }
                                )
                            }
                        }
                    }
                    2 -> { // Paid
                        if (paidInsts.isEmpty()) {
                            item {
                                EmptyStatusCard(
                                    title = "Nenhum pagamento registrado ainda.",
                                    description = "As parcelas quitadas por este cliente aparecerão organizadas nesta aba.",
                                    icon = Icons.Default.Receipt,
                                    color = DarkBluePrimary
                                )
                            }
                        } else {
                            items(paidInsts, key = { it.id }) { inst ->
                                OrganizedInstallmentCard(
                                    installment = inst,
                                    currencySymbol = currencySymbol,
                                    isSelected = false,
                                    onToggleSelect = null,
                                    onMarkPaid = { },
                                    onSendWhatsApp = { }
                                )
                            }
                        }
                    }
                    3 -> { // Loans
                        if (clientLoans.isEmpty()) {
                            item {
                                EmptyStatusCard(
                                    title = "Nenum contrato de empréstimo ativo.",
                                    description = "Clique no botão acima '+ Empréstimo' para gerar um novo contrato.",
                                    icon = Icons.Default.AddCard,
                                    color = DarkBluePrimary
                                )
                            }
                        } else {
                            items(clientLoans, key = { it.id }) { loan ->
                                LoanCardItem(loan = loan, currencySymbol = currencySymbol)
                            }
                        }
                    }
                }
            }
        }

        // Floating Batch Payment Bottom Bar
        if (selectedIds.isNotEmpty()) {
            val selectedList = clientInstallments.filter { selectedIds.contains(it.id) }
            val selectedSum = selectedList.sumOf { it.amount }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = DarkBluePrimary,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedIds.size} parcela(s) selecionada(s)",
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.8f))
                            )
                            Text(
                                text = "Total: $currencySymbol${String.format("%.2f", selectedSum)}",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }

                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar Seleção", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val idsToPay = selectedIds.toList()
                                selectedIds = emptySet()
                                if (onMarkMultiplePaid != null) {
                                    onMarkMultiplePaid(idsToPay, context)
                                } else {
                                    idsToPay.forEach { onMarkPaid(it, context) }
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dar Baixa (${selectedIds.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { sendWhatsAppSelectedMessage() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Cobrar no Whats", style = MaterialTheme.typography.labelMedium, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrganizedInstallmentCard(
    installment: Installment,
    currencySymbol: String,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onMarkPaid: () -> Unit,
    onSendWhatsApp: () -> Unit
) {
    val isOverdue = installment.status == InstallmentStatus.OVERDUE
    val isPaid = installment.status == InstallmentStatus.PAID

    val cardBg = when {
        isSelected -> DarkBluePrimary.copy(alpha = 0.12f)
        isOverdue -> OverdueRed.copy(alpha = 0.06f)
        isPaid -> EmeraldGreen.copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surface
    }

    val cardBorder = when {
        isSelected -> DarkBluePrimary
        isOverdue -> OverdueRed
        isPaid -> EmeraldGreen
        else -> DarkBluePrimary.copy(alpha = 0.3f)
    }

    Card(
        onClick = { if (!isPaid && onToggleSelect != null) onToggleSelect() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorder)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isPaid && onToggleSelect != null) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(checkedColor = DarkBluePrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isOverdue) OverdueRed else if (isPaid) EmeraldGreen else DarkBluePrimary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Parcela ${installment.installmentNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Vencimento: ${installment.dueDate}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Text(
                    text = "$currencySymbol${String.format("%.2f", installment.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isOverdue) OverdueRed else if (isPaid) EmeraldGreen else DarkBluePrimary
                    )
                )
            }

            if (!isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = cardBorder.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMarkPaid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Dar Baixa (1)", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onSendWhatsApp,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Cobrar no Whats", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✓ Pago em ${installment.paymentDate ?: "-"} • Comprovante emitido",
                    style = MaterialTheme.typography.bodySmall.copy(color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
fun EmptyStatusCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun LoanCardItem(loan: Loan, currencySymbol: String) {
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
                Text(
                    text = "Contrato #${loan.id.takeLast(6)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (loan.remainingBalance <= 0) Color(0xFFDCFCE7) else Color(0xFFE0E7FF))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = loan.status.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (loan.remainingBalance <= 0) Color(0xFF15803D) else DarkBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Valor Emprestado", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "$currencySymbol${String.format("%.2f", loan.loanAmount)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Column {
                    Text(text = "Total c/ Juros (${loan.interestRate}%)", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "$currencySymbol${String.format("%.2f", loan.totalAmount)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Column {
                    Text(text = "Saldo Devedor", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "$currencySymbol${String.format("%.2f", loan.remainingBalance)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OverdueRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${loan.totalInstallments} parcelas (${loan.frequency.displayName}) de $currencySymbol${String.format("%.2f", loan.installmentAmount)}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
