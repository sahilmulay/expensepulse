package com.expensepulse.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["upiTransactionId"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val merchantOrPerson: String,
    val bankAccount: String,
    val upiTransactionId: String? = null,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isExcludedFromAnalytics: Boolean = (type == TransactionType.SELF_TRANSFER),
    val linkedFriendName: String? = null
)
