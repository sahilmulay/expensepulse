package com.expensepulse.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>> =
        dao.getTransactionsByDateRange(start, end)

    fun getTotalExpenses(start: Long, end: Long): Flow<Double?> =
        dao.getTotalExpenses(start, end)

    fun getTotalIncomes(start: Long, end: Long): Flow<Double?> =
        dao.getTotalIncomes(start, end)

    fun getCategoryBreakdown(start: Long, end: Long): Flow<List<CategorySum>> =
        dao.getCategoryBreakdown(start, end)

    fun getTransactionsByFriend(friendName: String): Flow<List<TransactionEntity>> =
        dao.getTransactionsByFriend(friendName)

    suspend fun insert(transaction: TransactionEntity): Long = dao.insert(transaction)

    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long> = dao.insertAll(transactions)

    suspend fun update(transaction: TransactionEntity) = dao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)

    suspend fun findByUpiId(upiId: String): TransactionEntity? = dao.findByUpiId(upiId)
}
