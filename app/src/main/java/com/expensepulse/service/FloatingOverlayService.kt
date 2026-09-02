package com.expensepulse.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.expensepulse.ExpensePulseApplication
import com.expensepulse.R
import com.expensepulse.data.Category
import com.expensepulse.data.TransactionEntity
import com.expensepulse.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dynamic Island Style Floating Overlay for Android.
 * ONLY emerges when phone is shaken. Smoothly expands out of the front camera cutout,
 * and seamlessly retreats back into the camera hole when dismissed or saved.
 * Leaves ZERO persistent UI on screen when idle.
 */
class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var islandCard: CardView? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialAmount = intent?.getStringExtra(EXTRA_AMOUNT) ?: ""
        val initialMerchant = intent?.getStringExtra(EXTRA_MERCHANT) ?: ""

        if (overlayView == null) {
            showDynamicIsland(initialAmount, initialMerchant)
        }
        return START_NOT_STICKY
    }

    private fun showDynamicIsland(initialAmount: String, initialMerchant: String) {
        val layoutInflater = LayoutInflater.from(this)
        overlayView = layoutInflater.inflate(R.layout.layout_dynamic_island_overlay, null)

        val paramsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            paramsType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Anchor directly at top center under the front camera punch hole
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 8 // Sits precisely below camera notch
        }

        islandCard = overlayView?.findViewById(R.id.card_dynamic_island)
        val compactLayout = overlayView?.findViewById<View>(R.id.layout_island_compact)
        val expandedLayout = overlayView?.findViewById<View>(R.id.layout_island_expanded)

        val etAmount = overlayView?.findViewById<EditText>(R.id.et_island_amount)
        val etNote = overlayView?.findViewById<EditText>(R.id.et_island_note)
        val rgCategories = overlayView?.findViewById<RadioGroup>(R.id.rg_island_categories)
        val rgAccounts = overlayView?.findViewById<RadioGroup>(R.id.rg_island_accounts)
        val btnCollapse = overlayView?.findViewById<ImageButton>(R.id.btn_island_collapse)
        val btnSave = overlayView?.findViewById<Button>(R.id.btn_island_save)

        // Show expanded form directly since shake occurred
        compactLayout?.visibility = View.GONE
        expandedLayout?.visibility = View.VISIBLE

        if (initialAmount.isNotEmpty()) etAmount?.setText(initialAmount)
        if (initialMerchant.isNotEmpty()) etNote?.setText(initialMerchant)

        btnCollapse?.setOnClickListener {
            dismissSmoothly()
        }

        btnSave?.setOnClickListener {
            val amountStr = etAmount?.text?.toString()?.trim() ?: ""
            val amount = amountStr.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = when (rgCategories?.checkedRadioButtonId) {
                R.id.rb_island_food -> Category.FOOD_DINING
                R.id.rb_island_chai -> Category.CHAI_SNACKS
                R.id.rb_island_groceries -> Category.GROCERIES
                R.id.rb_island_travel -> Category.FUEL_TRAVEL
                R.id.rb_island_split -> Category.FRIENDS_SPLIT
                R.id.rb_island_bills -> Category.BILLS_RECHARGE
                else -> Category.OTHER
            }

            val bankAccount = when (rgAccounts?.checkedRadioButtonId) {
                R.id.rb_island_sbi -> "State Bank of India 7067"
                R.id.rb_island_ippb -> "India Post Payment Bank 2938"
                R.id.rb_island_cash -> "Cash"
                else -> "State Bank of India 7067"
            }

            val noteInput = etNote?.text?.toString()?.trim()
            val merchant = if (!noteInput.isNullOrEmpty()) {
                noteInput
            } else if (bankAccount == "Cash" && (category == Category.OTHER || category == Category.FOOD_DINING && noteInput.isNullOrEmpty() && false)) {
                "Paid via Cash"
            } else if (bankAccount == "Cash" && category == Category.OTHER) {
                "Paid via Cash"
            } else {
                "Paid to ${category.displayName}"
            }

            val transaction = TransactionEntity(
                amount = amount,
                type = if (category == Category.FRIENDS_SPLIT) TransactionType.SETTLEMENT else TransactionType.EXPENSE,
                category = category,
                merchantOrPerson = merchant,
                bankAccount = bankAccount,
                note = merchant,
                timestamp = System.currentTimeMillis()
            )

            val app = applicationContext as ExpensePulseApplication
            serviceScope.launch {
                app.repository.insert(transaction)
            }

            vibrateSuccess()
            showSuccessAndDismiss(amount, category)
        }

        try {
            windowManager?.addView(overlayView, params)

            // Apple Dynamic Island Liquid Physics (560ms Liquid Droplet Stretch & Squash):
            // 1. Droplet stretches vertically down from camera hole first
            // 2. Liquid blooms horizontally and settles with surface tension damping
            islandCard?.apply {
                pivotX = width.toFloat() / 2f
                pivotY = 0f
                scaleX = 0.1f
                scaleY = 0.1f
                alpha = 0f
                translationY = -20f

                // Liquid vertical stretch & blossom
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .translationY(0f)
                    .setDuration(560) // Snappy yet luscious 560ms
                    .setInterpolator(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            // Asymmetric spring: fast initial tear, gentle surface-tension dampening
                            android.view.animation.PathInterpolator(0.2f, 0.9f, 0.3f, 1.0f)
                        } else {
                            android.view.animation.OvershootInterpolator(1.08f)
                        }
                    )
                    .withEndAction {
                        etAmount?.requestFocus()
                    }
                    .start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            removeOverlay()
        }
    }

    private fun showSuccessAndDismiss(amount: Double, category: Category) {
        val compactLayout = overlayView?.findViewById<View>(R.id.layout_island_compact)
        val expandedLayout = overlayView?.findViewById<View>(R.id.layout_island_expanded)
        val tvPillIcon = overlayView?.findViewById<TextView>(R.id.tv_island_pill_icon)
        val tvPillText = overlayView?.findViewById<TextView>(R.id.tv_island_pill_text)
        val tvPillAmount = overlayView?.findViewById<TextView>(R.id.tv_island_pill_amount)

        expandedLayout?.visibility = View.GONE
        compactLayout?.visibility = View.VISIBLE

        tvPillIcon?.text = "✓"
        tvPillText?.text = "Logged to ${category.displayName}"
        tvPillAmount?.visibility = View.VISIBLE
        tvPillAmount?.text = "₹${amount.toInt()}"

        // Hold success pill at camera notch for 1.2s, then retract liquid into camera
        mainHandler.postDelayed({
            dismissSmoothly()
        }, 1200)
    }

    private fun dismissSmoothly() {
        // Liquid retracts: pinches inward and pulls into camera cutout (460ms)
        islandCard?.animate()
            ?.scaleX(0.08f)
            ?.scaleY(0.08f)
            ?.alpha(0f)
            ?.translationY(-20f)
            ?.setDuration(460) // Liquid ink suction 460ms
            ?.setInterpolator(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1.0f)
                } else {
                    android.view.animation.AccelerateInterpolator(1.6f)
                }
            )
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Clear inputs completely so next trigger is fresh
                    overlayView?.findViewById<EditText>(R.id.et_island_amount)?.setText("")
                    overlayView?.findViewById<EditText>(R.id.et_island_note)?.setText("")
                    removeOverlay()
                }
            })
            ?.start()
    }

    private fun vibrateSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        }
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_MERCHANT = "extra_merchant"
    }
}
