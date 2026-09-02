package com.expensepulse.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class CategorySum(
    val category: Category,
    val totalAmount: Double,
    val count: Int
)

data class FriendSummary(
    val friendName: String,
    val totalGiven: Double,
    val totalReceived: Double,
    val netBalance: Double
)

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp >= :start AND timestamp <= :end 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'EXPENSE' 
        AND isExcludedFromAnalytics = 0 
        AND timestamp >= :start AND timestamp <= :end
    """)
    fun getTotalExpenses(start: Long, end: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE (type = 'INCOME' OR type = 'SETTLEMENT') 
        AND isExcludedFromAnalytics = 0 
        AND timestamp >= :start AND timestamp <= :end
    """)
    fun getTotalIncomes(start: Long, end: Long): Flow<Double?>

    @Query("""
        SELECT category, SUM(amount) as totalAmount, COUNT(id) as count 
        FROM transactions 
        WHERE type = 'EXPENSE' 
        AND isExcludedFromAnalytics = 0 
        AND timestamp >= :start AND timestamp <= :end 
        GROUP BY category 
        ORDER BY totalAmount DESC
    """)
    fun getCategoryBreakdown(start: Long, end: Long): Flow<List<CategorySum>>

    @Query("""
        SELECT * FROM transactions 
        WHERE linkedFriendName = :friendName 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByFriend(friendName: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE upiTransactionId = :upiId LIMIT 1")
    suspend fun findByUpiId(upiId: String): TransactionEntity?

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}
