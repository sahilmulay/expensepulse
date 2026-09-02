package com.expensepulse.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.expensepulse.ExpensePulseApplication
import com.expensepulse.parser.ShareIntentParser
import com.expensepulse.service.FloatingOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)
        finish()
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            val type = intent.type

            if (type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                processSharedText(sharedText)
            } else if (type?.startsWith("image/") == true) {
                val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                processSharedImage(imageUri)
            } else if (type == "application/pdf") {
                val pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                processSharedPdf(pdfUri)
            }
        }
    }

    private fun processSharedText(text: String) {
        val extracted = ShareIntentParser.parseSharedText(text)
        if (extracted != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                val overlayIntent = Intent(this, FloatingOverlayService::class.java).apply {
                    putExtra(FloatingOverlayService.EXTRA_AMOUNT, extracted.amount.toString())
                    putExtra(FloatingOverlayService.EXTRA_MERCHANT, extracted.recipientOrMerchant)
                }
                startService(overlayIntent)
            } else {
                // Directly save to DB and notify
                val app = applicationContext as ExpensePulseApplication
                val entity = ShareIntentParser.toTransactionEntity(extracted)
                CoroutineScope(Dispatchers.IO).launch {
                    app.repository.insert(entity)
                }
                Toast.makeText(this, "Logged ₹${extracted.amount} to ${extracted.recipientOrMerchant}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Could not detect payment details from shared text", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processSharedImage(uri: Uri?) {
        Toast.makeText(this, "Received receipt image. Opening quick log...", Toast.LENGTH_SHORT).show()
        val overlayIntent = Intent(this, FloatingOverlayService::class.java)
        startService(overlayIntent)
    }

    private fun processSharedPdf(uri: Uri?) {
        if (uri != null) {
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_shared_pdf_uri", uri.toString())
            }
            startActivity(mainIntent)
            Toast.makeText(this, "Opening ExpensePulse statement importer...", Toast.LENGTH_SHORT).show()
        }
    }
}
