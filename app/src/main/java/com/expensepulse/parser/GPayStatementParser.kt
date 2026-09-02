package com.expensepulse.parser

import com.expensepulse.data.Category
import com.expensepulse.data.TransactionEntity
import com.expensepulse.data.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

object GPayStatementParser {

    private val dateFormat = SimpleDateFormat("dd MMM, yyyy hh:mm a", Locale.ENGLISH)

    // Known friends from transaction history for settlement tracking
    private val knownFriends = listOf(
        "Vedant Satish Jadhav",
        "Nihal Jahid Shaikh",
        "Aditya Kerimane",
        "Kedar Patil",
        "Amruta Mote",
        "Ashna Kumbhar",
        "Shreya Dadaso Koli",
        "Swapnil Ravaso Mokashi",
        "Akram Shaikh",
        "Dilip Mulay",
        "Sujata Shital Mulay",
        "Snehal Mulay",
        "Soham Mulay",
        "Piyusha Mulay"
    )

    data class ParsedTransaction(
        val dateStr: String,
        val timeStr: String,
        val actionLine: String,
        val upiId: String,
        val bankLine: String,
        val amountStr: String
    )

    fun parseStatementText(fullText: String): List<TransactionEntity> {
        val lines = fullText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val transactions = mutableListOf<TransactionEntity>()

        var i = 0
        val dateRegex = Regex("""^(\d{1,2}\s+[A-Za-z]{3},\s+\d{4})$""")
        val timeRegex = Regex("""^(\d{1,2}:\d{2}\s+(?:AM|PM|am|pm))$""")
        val amountRegex = Regex("""^₹\s*([0-9,]+(?:\.[0-9]+)?)$""")

        while (i < lines.size) {
            val line = lines[i]
            if (dateRegex.matches(line)) {
                val datePart = line
                var timePart = ""
                var actionLine = ""
                var upiId = ""
                var bankLine = ""
                var amountVal = 0.0

                var j = i + 1
                while (j < lines.size && j < i + 10) {
                    val nextLine = lines[j]

                    if (dateRegex.matches(nextLine)) {
                        break
                    }

                    if (timeRegex.matches(nextLine)) {
                        timePart = nextLine
                    } else if (nextLine.startsWith("Received from ", ignoreCase = true)) {
                        actionLine = nextLine
                    } else if (nextLine.startsWith("Self transfer to ", ignoreCase = true)) {
                        actionLine = nextLine
                    } else if (actionLine.isEmpty() && nextLine.startsWith("Paid to ", ignoreCase = true)) {
                        actionLine = nextLine
                    } else if (nextLine.contains("UPI Transaction ID:", ignoreCase = true)) {
                        upiId = nextLine.substringAfter(":").trim()
                    } else if (nextLine.startsWith("Paid by ", ignoreCase = true) ||
                        nextLine.startsWith("Paid to State Bank", ignoreCase = true) ||
                        nextLine.startsWith("Paid to India Post", ignoreCase = true)) {
                        bankLine = nextLine.replace(Regex("^(Paid by|Paid to)\\s+"), "").trim()
                    } else {
                        val amountMatch = amountRegex.find(nextLine)
                        if (amountMatch != null) {
                            val cleanAmount = amountMatch.groupValues[1].replace(",", "")
                            amountVal = cleanAmount.toDoubleOrNull() ?: 0.0
                            // Usually amount is the last field of a transaction block
                            j++
                            break
                        }
                    }
                    j++
                }

                if (actionLine.isNotEmpty() && amountVal > 0) {
                    val entity = buildTransactionEntity(
                        dateStr = "$datePart $timePart".trim(),
                        actionLine = actionLine,
                        upiId = upiId,
                        bankAccount = if (bankLine.isNotEmpty()) bankLine else "State Bank of India 7067",
                        amount = amountVal
                    )
                    transactions.add(entity)
                    i = j - 1
                }
            }
            i++
        }

        return transactions
    }

    private fun buildTransactionEntity(
        dateStr: String,
        actionLine: String,
        upiId: String,
        bankAccount: String,
        amount: Double
    ): TransactionEntity {
        val timestamp = runCatching {
            dateFormat.parse(dateStr)?.time
        }.getOrNull() ?: System.currentTimeMillis()

        val type: TransactionType
        val merchantOrPerson: String
        var linkedFriend: String? = null

        when {
            actionLine.startsWith("Self transfer", ignoreCase = true) -> {
                type = TransactionType.SELF_TRANSFER
                merchantOrPerson = actionLine.replace(Regex("(?i)Self transfer (to )?"), "").trim()
            }
            actionLine.startsWith("Received from", ignoreCase = true) -> {
                val person = actionLine.replace(Regex("(?i)Received from\\s+"), "").trim()
                merchantOrPerson = person

                val matchedFriend = knownFriends.firstOrNull { it.equals(person, ignoreCase = true) }
                if (matchedFriend != null) {
                    type = TransactionType.SETTLEMENT
                    linkedFriend = matchedFriend
                } else {
                    type = TransactionType.INCOME
                }
            }
            else -> {
                type = TransactionType.EXPENSE
                merchantOrPerson = actionLine.replace(Regex("(?i)Paid to\\s+"), "").trim()
                val matchedFriend = knownFriends.firstOrNull { it.equals(merchantOrPerson, ignoreCase = true) }
                if (matchedFriend != null) {
                    linkedFriend = matchedFriend
                }
            }
        }

        val category = if (linkedFriend != null && type == TransactionType.SETTLEMENT) {
            Category.FRIENDS_SPLIT
        } else {
            Category.inferCategory(merchantOrPerson, type)
        }

        return TransactionEntity(
            amount = amount,
            type = type,
            category = category,
            merchantOrPerson = merchantOrPerson,
            bankAccount = bankAccount,
            upiTransactionId = upiId.takeIf { it.isNotEmpty() },
            note = actionLine,
            timestamp = timestamp,
            isExcludedFromAnalytics = (type == TransactionType.SELF_TRANSFER),
            linkedFriendName = linkedFriend
        )
    }
}
