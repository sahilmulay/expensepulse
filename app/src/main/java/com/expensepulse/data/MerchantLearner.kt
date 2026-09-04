package com.expensepulse.data

import android.content.Context
import org.json.JSONObject

/**
 * Smart Merchant-to-Category Machine Learning Helper.
 * Automatically remembers user categorization patterns for specific merchants
 * and predicts the most relevant category as the user types or logs transactions.
 */
object MerchantLearner {

    private const val PREFS_NAME = "expense_pulse_prefs"
    private const val KEY_MERCHANT_MAP = "learned_merchant_categories_v1"

    /**
     * Records a merchant -> category mapping when an expense is saved or edited.
     */
    fun learn(context: Context, merchantOrNote: String, category: Category) {
        val cleanKey = normalizeMerchantName(merchantOrNote)
        if (cleanKey.isBlank() || cleanKey.length < 2) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_MERCHANT_MAP, null)
        val jsonObject = if (currentJson != null) {
            try { JSONObject(currentJson) } catch (e: Exception) { JSONObject() }
        } else {
            JSONObject()
        }

        jsonObject.put(cleanKey, category.name)
        prefs.edit().putString(KEY_MERCHANT_MAP, jsonObject.toString()).apply()
    }

    /**
     * Predicts the user's preferred category for a given merchant/note input.
     * Returns null if no learned pattern matches.
     */
    fun predict(context: Context, merchantOrNote: String): Category? {
        val cleanKey = normalizeMerchantName(merchantOrNote)
        if (cleanKey.isBlank()) return null

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_MERCHANT_MAP, null) ?: return null

        return try {
            val jsonObject = JSONObject(currentJson)

            // 1. Direct key match
            if (jsonObject.has(cleanKey)) {
                val catName = jsonObject.getString(cleanKey)
                return runCatching { Category.valueOf(catName) }.getOrNull()
            }

            // 2. Substring matching for multi-word or variations
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (cleanKey.contains(key) || key.contains(cleanKey)) {
                    val catName = jsonObject.getString(key)
                    return runCatching { Category.valueOf(catName) }.getOrNull()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cleans up merchant strings (removes UPI prefixes, dates, and common clutter).
     */
    private fun normalizeMerchantName(raw: String): String {
        return raw.lowercase()
            .replace("paid to", "")
            .replace("payment to", "")
            .replace("transfer to", "")
            .replace("sent to", "")
            .replace("via upi", "")
            .replace("upi", "")
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .trim()
    }
}
