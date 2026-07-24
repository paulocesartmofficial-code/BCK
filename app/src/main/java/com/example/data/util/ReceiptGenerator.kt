package com.example.data.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Receipt
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLEncoder

object ReceiptGenerator {

    /**
     * Renders a high-quality receipt image on a Canvas and saves it as a PNG file.
     */
    fun createReceiptBitmap(context: Context, receipt: Receipt): File {
        val width = 800
        val height = 1150
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header Background (Dark Blue #0F3D91)
        val headerPaint = Paint().apply {
            color = Color.parseColor("#0F3D91")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 200f, headerPaint)

        // BCK Logo / Shield Title
        val logoTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("BCK", 60f, 110f, logoTitlePaint)

        val logoSubPaint = Paint().apply {
            color = Color.parseColor("#22C55E") // Emerald Green
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("GESTÃO DE EMPRÉSTIMOS E COMPROVANTE DIGITAL", 60f, 155f, logoSubPaint)

        // Receipt Card Border
        val cardBorder = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val cardRect = RectF(40f, 230f, width - 40f, height - 120f)
        canvas.drawRoundRect(cardRect, 20f, 20f, cardBorder)

        // Receipt Title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("COMPROVANTE OFICIAL DE PAGAMENTO", 70f, 290f, titlePaint)

        // Receipt Number & Date
        val metaPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText("Comprovante nº: ${receipt.receiptNumber}", 70f, 330f, metaPaint)
        canvas.drawText("Data: ${receipt.date}  |  Hora: ${receipt.time}", 70f, 365f, metaPaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 2f
        }
        canvas.drawLine(70f, 395f, width - 70f, 395f, dividerPaint)

        // Details Grid
        var currentY = 440f
        val labelPaint = TextPaint().apply {
            color = Color.parseColor("#64748B")
            textSize = 26f
            isAntiAlias = true
        }
        val valuePaint = TextPaint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        fun drawRow(label: String, value: String, isGreen: Boolean = false) {
            canvas.drawText(label, 70f, currentY, labelPaint)
            val valP = if (isGreen) {
                Paint(valuePaint).apply { color = Color.parseColor("#22C55E") }
            } else valuePaint
            val valWidth = valP.measureText(value)
            canvas.drawText(value, width - 70f - valWidth, currentY, valP)
            currentY += 55f
        }

        drawRow("Nome do Cliente:", receipt.clientName)
        drawRow("Valor Empréstimo:", "R$ ${String.format("%.2f", receipt.loanAmount)}")
        drawRow("Detalhamento:", if (receipt.installmentDescription.isNotBlank()) receipt.installmentDescription else "Parcela ${receipt.installmentNumber} de ${receipt.totalInstallments}")
        drawRow("Valor Pago:", "R$ ${String.format("%.2f", receipt.amountPaid)}", isGreen = true)
        drawRow("Saldo Devedor:", "R$ ${String.format("%.2f", receipt.remainingBalance)}")
        drawRow("Quantas Falta:", "${receipt.installmentsRemaining} parcela(s) pendente(s)")

        // Status Badge
        currentY += 10f
        val badgeBg = Paint().apply {
            color = Color.parseColor("#DCFCE7") // light emerald background
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val badgeRect = RectF(70f, currentY, 260f, currentY + 60f)
        canvas.drawRoundRect(badgeRect, 30f, 30f, badgeBg)

        val badgeTextPaint = Paint().apply {
            color = Color.parseColor("#15803D")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("STATUS: PAGO", 90f, currentY + 40f, badgeTextPaint)

        // Thank you message
        currentY += 120f
        val thankPaint = Paint().apply {
            color = Color.parseColor("#0F3D91")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        val thankWidth = thankPaint.measureText(receipt.thankYouMessage)
        canvas.drawText(receipt.thankYouMessage, (width - thankWidth) / 2f, currentY, thankPaint)

        // Footer
        val footerBg = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - 80f, width.toFloat(), height.toFloat(), footerBg)

        val footerTextPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
            isAntiAlias = true
        }
        val footerStr = "Aplicativo BCK Gestão de Empréstimos • Gerado Automaticamente"
        val fWidth = footerTextPaint.measureText(footerStr)
        canvas.drawText(footerStr, (width - fWidth) / 2f, height - 35f, footerTextPaint)

        // Save to cache file
        val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(receiptsDir, "Receipt_${receipt.receiptNumber}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    /**
     * Share receipt image via WhatsApp or standard sharing intent.
     */
    fun shareViaWhatsApp(context: Context, receipt: Receipt, clientPhone: String) {
        val file = createReceiptBitmap(context, receipt)
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val desc = if (receipt.installmentDescription.isNotBlank()) receipt.installmentDescription else "Parcela nº ${receipt.installmentNumber}"
        val message = """
            *COMPROVANTE DE PAGAMENTO - BCK*
            
            *Comprovante nº:* ${receipt.receiptNumber}
            *Cliente:* ${receipt.clientName}
            *Data/Hora:* ${receipt.date} ${receipt.time}
            
            • *Detalhamento:* $desc
            • *Valor Total Pago:* R$ ${String.format("%.2f", receipt.amountPaid)}
            • *Saldo Devedor Atual:* R$ ${String.format("%.2f", receipt.remainingBalance)}
            • *Quantas Falta:* ${receipt.installmentsRemaining} parcela(s) pendente(s)
            
            _${receipt.thankYouMessage}_
        """.trimIndent()

        val cleanPhone = clientPhone.replace("[^0-9]".toRegex(), "")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (cleanPhone.isNotEmpty()) {
                putExtra("jid", "$cleanPhone@s.whatsapp.net")
            }
            `package` = "com.whatsapp"
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general intent chooser if WhatsApp is not installed
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Compartilhar Comprovante via...")
            context.startActivity(chooser)
        }
    }

    /**
     * Saves the receipt image to MediaStore / Downloads directory.
     */
    fun saveImageToGallery(context: Context, receipt: Receipt): Boolean {
        return try {
            val file = createReceiptBitmap(context, receipt)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            val filename = "BCK_Comprovante_${receipt.receiptNumber}.png"
            val outputStream: OutputStream?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BCK_Receipts")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val bckFolder = File(imagesDir, "BCK_Receipts").apply { mkdirs() }
                val imageFile = File(bckFolder, filename)
                outputStream = FileOutputStream(imageFile)
            }

            outputStream?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "Comprovante salvo em Imagens/BCK_Receipts!", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Falha ao salvar imagem do comprovante: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
