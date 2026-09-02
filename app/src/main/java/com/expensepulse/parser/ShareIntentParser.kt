package com.expensepulse.parser

import com.expensepulse.data.Category
import com.expensepulse.data.TransactionEntity
import com.expensepulse.data.TransactionType

object ShareIntentParser {

    data class ExtractedPayment(
        val amount: Double,
        val recipientOrMerchant: String,
        val upiRefId: String?,
        val inferredCategory: Category
    )

    fun parseSharedText(text: String): ExtractedPayment? {
        if (text.isBlank()) return null

        // Examples of UPI share text:
        // "Paid ₹433.46 to Zomato using UPI. Ref: 621835978214"
        // "Sent Rs. 250 to Amruta Mote. Transaction ID: 621461611965"
        // "₹1,511 paid to BLINKIT"

        val amountPattern = Regex("""(?:₹|Rs\.?|INR)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val amountMatch = amountPattern.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        // Extract merchant or recipient
        val merchantPattern = Regex("""(?:paid to|sent to|transfer to|to)\s+([A-Za-z0-9\s&]+?)(?:\s+(?:using|via|ref|txn|transaction|on|from)|$|\.)""", RegexOption.IGNORE_CASE)
        val merchantMatch = merchantPattern.find(text)
        val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: "Shared UPI Payment"

        // Extract UPI Ref / Txn ID
        val refPattern = Regex("""(?:UPI Ref(?: ID)?|Txn(?: ID)?|Transaction ID|Ref:?)\s*[:#]?\s*([0-9]{10,16})""", RegexOption.IGNORE_CASE)
        val refMatch = refPattern.find(text)
        val upiRef = refMatch?.groupValues?.get(1)?.trim()

        val category = Category.inferCategory(merchant, TransactionType.EXPENSE)

        return ExtractedPayment(
            amount = amount,
            recipientOrMerchant = merchant,
            upiRefId = upiRef,
            inferredCategory = category
        )
    }

    fun toTransactionEntity(payment: ExtractedPayment, bankAccount: String = "State Bank of India 7067"): TransactionEntity {
        return TransactionEntity(
            amount = payment.amount,
            type = TransactionType.EXPENSE,
            category = payment.inferredCategory,
            merchantOrPerson = payment.recipientOrMerchant,
            bankAccount = bankAccount,
            upiTransactionId = payment.upiRefId,
            note = "Shared from UPI app",
            timestamp = System.currentTimeMillis()
        )
    }
}
