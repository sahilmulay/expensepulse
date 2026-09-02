package com.expensepulse.data

enum class TransactionType {
    EXPENSE,        // Outflow: Paid to merchant/vendor
    INCOME,         // Inflow: Salary, external credit
    SETTLEMENT,     // Inflow from friends (offsets prior bills)
    SELF_TRANSFER   // Transfer between user's own accounts (SBI <-> IPPB)
}
