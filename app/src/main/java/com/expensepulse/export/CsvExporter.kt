package com.expensepulse.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.expensepulse.data.CategoryManager
import com.expensepulse.data.TransactionEntity
import com.expensepulse.data.TransactionType
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun exportTransactionsToCsv(context: Context, transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "No transactions found to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "ExpensePulse_Ledger_$timeStamp.csv")

            val writer = FileWriter(file)
            // CSV Header
            writer.append("ID,Date,Time,Merchant / Note,Amount (INR),Category,Account,Type\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

            for (tx in transactions.sortedByDescending { it.timestamp }) {
                val date = dateFormat.format(Date(tx.timestamp))
                val time = timeFormat.format(Date(tx.timestamp))
                val catItem = CategoryManager.getCategoryItem(context, tx.category)
                val cleanMerchant = tx.merchantOrPerson.replace("\"", "\"\"").replace("\n", " ")
                val typeStr = when (tx.type) {
                    TransactionType.EXPENSE -> "Expense"
                    TransactionType.INCOME -> "Income"
                    TransactionType.SELF_TRANSFER -> "Self Transfer"
                    TransactionType.SETTLEMENT -> "Friend Settlement"
                }

                writer.append("${tx.id},")
                writer.append("$date,")
                writer.append("$time,")
                writer.append("\"$cleanMerchant\",")
                writer.append("${"%.2f".format(tx.amount)},")
                writer.append("\"${catItem.displayName}\",")
                writer.append("\"${tx.bankAccount}\",")
                writer.append("$typeStr\n")
            }

            writer.flush()
            writer.close()

            // Generate content URI with FileProvider
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "ExpensePulse Ledger Export ($timeStamp)")
                putExtra(Intent.EXTRA_TEXT, "Here is your exported ExpensePulse transaction ledger.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(shareIntent, "Share / Open Expense Ledger CSV").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
