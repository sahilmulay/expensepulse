package com.expensepulse.data

enum class Category(
    val displayName: String,
    val iconEmoji: String,
    val defaultKeywords: List<String>
) {
    FOOD_DINING("Food & Dining", "🍔", listOf("zomato", "swiggy", "mcdonalds", "mc donalds", "restaurant", "hotel", "tiffans", "vegis", "bake")),
    CHAI_SNACKS("Chai & Snacks", "☕", listOf("chay", "tea", "bakery", "xerox", "tailors", "shetty")),
    GROCERIES("Groceries", "🛒", listOf("blinkit", "zepto", "instamart", "mart", "supermarket", "kirana", "dudh")),
    FUEL_TRAVEL("Fuel & Travel", "🛵", listOf("petrol", "petroleum", "redbus", "railway", "irctc", "auto", "uber", "ola", "travels")),
    BILLS_RECHARGE("Bills & Recharges", "⚡", listOf("jio", "airtel", "vi", "electricity", "broadband", "recharge", "spotify", "youtube")),
    SHOPPING("Shopping", "🛍️", listOf("flipkart", "amazon", "myntra", "trent", "zudio", "furnishing", "trading")),
    FRIENDS_SPLIT("Friend Split", "🤝", listOf("split", "share", "settle", "vedant", "nihal", "aditya", "kedar", "amruta")),
    CAPITAL_INVESTMENT("Vehicle & Large Outlay", "🏍️", listOf("royal enfield", "bike", "car", "deposit")),
    INTERNAL_TRANSFER("Self Transfer", "🔄", listOf("self transfer")),
    OTHER("Other", "📝", emptyList());

    companion object {
        fun inferCategory(merchantName: String, transactionType: TransactionType): Category {
            if (transactionType == TransactionType.SELF_TRANSFER) {
                return INTERNAL_TRANSFER
            }
            if (transactionType == TransactionType.SETTLEMENT) {
                return FRIENDS_SPLIT
            }
            val lower = merchantName.lowercase()
            for (category in entries) {
                if (category.defaultKeywords.any { lower.contains(it) }) {
                    return category
                }
            }
            return OTHER
        }
    }
}
