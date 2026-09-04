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
        fun inferCategory(merchantName: String, transactionType: TransactionType, context: android.content.Context? = null): Category {
            if (transactionType == TransactionType.SELF_TRANSFER) {
                return INTERNAL_TRANSFER
            }
            if (transactionType == TransactionType.SETTLEMENT) {
                return FRIENDS_SPLIT
            }
            // 1. Check Smart Learned Merchant Patterns First
            if (context != null) {
                val learned = MerchantLearner.predict(context, merchantName)
                if (learned != null) return learned
            }
            // 2. Fallback to default keyword heuristics
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

data class CategoryItem(
    val key: String,
    val enumCategory: Category,
    val displayName: String,
    val iconEmoji: String,
    val isEnabled: Boolean = true
)

object CategoryManager {
    private const val PREFS_NAME = "expense_pulse_prefs"
    private const val KEY_CATEGORIES_CONFIG = "custom_categories_config_v1"

    val DEFAULT_ITEMS: List<CategoryItem> = listOf(
        CategoryItem(Category.FOOD_DINING.name, Category.FOOD_DINING, "Food & Dining", "🍔"),
        CategoryItem(Category.CHAI_SNACKS.name, Category.CHAI_SNACKS, "Chai & Snacks", "☕"),
        CategoryItem(Category.GROCERIES.name, Category.GROCERIES, "Groceries", "🛒"),
        CategoryItem(Category.FUEL_TRAVEL.name, Category.FUEL_TRAVEL, "Fuel & Travel", "🛵"),
        CategoryItem(Category.BILLS_RECHARGE.name, Category.BILLS_RECHARGE, "Bills & Recharges", "⚡"),
        CategoryItem(Category.SHOPPING.name, Category.SHOPPING, "Shopping", "🛍️"),
        CategoryItem(Category.FRIENDS_SPLIT.name, Category.FRIENDS_SPLIT, "Friend Split", "🤝"),
        CategoryItem(Category.CAPITAL_INVESTMENT.name, Category.CAPITAL_INVESTMENT, "Vehicle & Outlay", "🏍️"),
        CategoryItem(Category.OTHER.name, Category.OTHER, "Other", "📝")
    )

    fun getCategories(context: android.content.Context): List<CategoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CATEGORIES_CONFIG, null) ?: return DEFAULT_ITEMS

        return try {
            val jsonArray = org.json.JSONArray(jsonStr)
            val list = mutableListOf<CategoryItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val key = obj.getString("key")
                val name = obj.getString("name")
                val emoji = obj.getString("emoji")
                val enabled = obj.optBoolean("enabled", true)
                val enumVal = runCatching { Category.valueOf(key) }.getOrDefault(Category.OTHER)
                list.add(CategoryItem(key, enumVal, name, emoji, enabled))
            }
            if (list.isEmpty()) DEFAULT_ITEMS else list
        } catch (e: Exception) {
            DEFAULT_ITEMS
        }
    }

    fun saveCategories(context: android.content.Context, categories: List<CategoryItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        for (item in categories) {
            val obj = org.json.JSONObject().apply {
                put("key", item.key)
                put("name", item.displayName)
                put("emoji", item.iconEmoji)
                put("enabled", item.isEnabled)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CATEGORIES_CONFIG, jsonArray.toString()).apply()
    }

    fun getCategoryItem(context: android.content.Context, category: Category): CategoryItem {
        val all = getCategories(context)
        return all.find { it.enumCategory == category || it.key == category.name }
            ?: CategoryItem(category.name, category, category.displayName, category.iconEmoji)
    }

    fun resetDefaults(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CATEGORIES_CONFIG).apply()
    }
}
