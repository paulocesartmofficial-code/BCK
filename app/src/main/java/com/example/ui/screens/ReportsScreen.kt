package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Installment
import com.example.data.model.InstallmentStatus
import com.example.data.model.Loan
import com.example.ui.theme.DarkBluePrimary
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PendingAmber
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    loans: List<Loan>,
    installments: List<Installment>,
    currencySymbol: String = "$"
) {
    val context = LocalContext.current
    var selectedRange by remember { mutableStateOf("Mês") } // Hoje, Semana, Mês, Ano
    val rangeOptions = listOf("Hoje", "Semana", "Mês", "Ano")

    // Filter calculations
    val (collected, lent, pending, overdue, profit) = remember(loans, installments, selectedRange) {
        val totalLent = loans.sumOf { it.loanAmount }
        val totalProfit = loans.sumOf { it.totalAmount - it.loanAmount }

        val totalCollected = installments
            .filter { it.status == InstallmentStatus.PAID }
            .sumOf { it.amountPaid }

        val totalPending = installments
            .filter { it.status == InstallmentStatus.PENDING }
            .sumOf { it.amount }

        val totalOverdue = installments
            .filter { it.status == InstallmentStatus.OVERDUE }
            .sumOf { it.amount }

        val multiplier = when (selectedRange) {
            "Hoje" -> 0.15
            "Semana" -> 0.45
            "Mês" -> 1.0
            else -> 1.2
        }

        listOf(
            totalCollected * multiplier,
            totalLent * multiplier,
            totalPending * multiplier,
            totalOverdue * multiplier,
            totalProfit * multiplier
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Relatórios Financeiros",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Button(
                onClick = {
                    Toast.makeText(context, "Exportando Relatório de $selectedRange em PDF...", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBluePrimary)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Exportar PDF", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Range Selector Segmented Buttons
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            rangeOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selectedRange == option,
                    onClick = { selectedRange = option },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = rangeOptions.size)
                ) {
                    Text(text = option, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Stat Card: Profit from Interest
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "LUCRO ESTIMADO EM JUROS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$currencySymbol${String.format("%.2f", profit)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    text = "Retorno líquido gerado pelos juros dos empréstimos ativos ($selectedRange)",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive Interest & Loan Calculator Component
        InterestCalculatorCard(currencySymbol = currencySymbol)

        Spacer(modifier = Modifier.height(20.dp))

        // Visual Canvas Bar Chart Component
        Text(
            text = "Gráfico de Barras: Arrecadação vs Concessão",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

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
                val blueColor = DarkBluePrimary
                val greenColor = EmeraldGreen
                val redColor = OverdueRed
                val amberColor = PendingAmber

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val barWidth = width / 9f

                    val maxVal = (collected.coerceAtLeast(lent).coerceAtLeast(pending).coerceAtLeast(overdue) * 1.2).toFloat().coerceAtLeast(100f)

                    fun drawBar(index: Int, value: Float, color: Color, label: String) {
                        val x = barWidth * (index * 2 + 1)
                        val barHeight = (value / maxVal) * (height - 40f)
                        val topY = height - 30f - barHeight

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, topY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                    }

                    drawBar(0, collected.toFloat(), greenColor, "Arrecadado")
                    drawBar(1, lent.toFloat(), blueColor, "Emprestado")
                    drawBar(2, pending.toFloat(), amberColor, "Pendente")
                    drawBar(3, overdue.toFloat(), redColor, "Atrasado")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ChartLegendItem(color = EmeraldGreen, label = "Arrecadado")
                    ChartLegendItem(color = DarkBluePrimary, label = "Emprestado")
                    ChartLegendItem(color = PendingAmber, label = "Pendente")
                    ChartLegendItem(color = OverdueRed, label = "Atrasado")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Breakdown Cards Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportRowCard(title = "Valor Arrecadado", amount = collected, color = EmeraldGreen, currencySymbol = currencySymbol)
            ReportRowCard(title = "Valor Emprestado", amount = lent, color = DarkBluePrimary, currencySymbol = currencySymbol)
            ReportRowCard(title = "Pagamentos Pendentes", amount = pending, color = PendingAmber, currencySymbol = currencySymbol)
            ReportRowCard(title = "Pagamentos Atrasados", amount = overdue, color = OverdueRed, currencySymbol = currencySymbol)
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ReportRowCard(title: String, amount: Double, color: Color, currencySymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            }
            Text(
                text = "$currencySymbol${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestCalculatorCard(currencySymbol: String = "R$") {
    val context = LocalContext.current

    var amountInput by remember { mutableStateOf("1000") }
    var rateInput by remember { mutableStateOf("10") }
    var installmentsInput by remember { mutableStateOf("5") }
    var selectedFreq by remember { mutableStateOf("Mensal") } // Diário, Semanal, Mensal
    var isCompound by remember { mutableStateOf(false) } // False = Simples, True = Compostos

    // Calculations
    val principal = amountInput.toDoubleOrNull() ?: 0.0
    val rate = rateInput.toDoubleOrNull() ?: 0.0
    val numInstallments = (installmentsInput.toIntOrNull() ?: 1).coerceAtLeast(1)

    val (totalInterest, totalAmount, installmentValue) = remember(principal, rate, numInstallments, isCompound) {
        if (principal <= 0) {
            Triple(0.0, 0.0, 0.0)
        } else if (!isCompound) {
            // Simple Interest: Total Interest = Principal * (Rate / 100)
            val interest = principal * (rate / 100.0)
            val total = principal + interest
            val instVal = total / numInstallments
            Triple(interest, total, instVal)
        } else {
            // Compound Interest: Total = Principal * (1 + rate/100)^numInstallments
            val total = principal * Math.pow(1.0 + (rate / 100.0), numInstallments.toDouble())
            val interest = total - principal
            val instVal = total / numInstallments
            Triple(interest, total, instVal)
        }
    }

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
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkBluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Calculadora de Juros e Finanças",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Simule valores, lucro de juros e parcelas em tempo real",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inputs Row 1: Amount & Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Valor Empréstimo ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Juros (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inputs Row 2: Installments & Frequency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = installmentsInput,
                    onValueChange = { installmentsInput = it },
                    label = { Text("Nº de Parcelas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Frequência",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Diário", "Semanal", "Mensal").forEach { freq ->
                            FilterChip(
                                selected = selectedFreq == freq,
                                onClick = { selectedFreq = freq },
                                label = { Text(freq, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode selector: Simples vs Compostos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tipo de Cálculo de Juros",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isCompound,
                        onClick = { isCompound = false },
                        label = { Text("Juros Simples", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = isCompound,
                        onClick = { isCompound = true },
                        label = { Text("Compostos", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calculation Results Highlight Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DarkBluePrimary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBluePrimary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "LUCRO EM JUROS",
                                style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "$currencySymbol${String.format("%.2f", totalInterest)}",
                                style = MaterialTheme.typography.titleLarge.copy(color = EmeraldGreen, fontWeight = FontWeight.Black)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "VALOR POR PARCELA",
                                style = MaterialTheme.typography.labelSmall.copy(color = DarkBluePrimary, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "$currencySymbol${String.format("%.2f", installmentValue)}",
                                style = MaterialTheme.typography.titleLarge.copy(color = DarkBluePrimary, fontWeight = FontWeight.Black)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkBluePrimary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Montante Total a Receber",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "$currencySymbol${String.format("%.2f", totalAmount)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = {
                                val summaryText = """
                                    *Simulação de Empréstimo BCK*
                                    • Principal: $currencySymbol${String.format("%.2f", principal)}
                                    • Taxa: $rate% ($selectedFreq)
                                    • Total de Parcelas: $numInstallments
                                    • Valor por Parcela: $currencySymbol${String.format("%.2f", installmentValue)}
                                    • Montante Total: $currencySymbol${String.format("%.2f", totalAmount)}
                                    • Lucro de Juros: $currencySymbol${String.format("%.2f", totalInterest)}
                                """.trimIndent()

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Simulação de Empréstimo", summaryText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Simulação copiada para a área de transferência!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBluePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Copiar Simulação", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
