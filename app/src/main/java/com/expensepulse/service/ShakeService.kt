package com.expensepulse.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.expensepulse.ExpensePulseApplication
import com.expensepulse.R
import com.expensepulse.ui.MainActivity

class ShakeService : Service() {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var shakeDetector: ShakeDetector? = null
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Offload accelerometer processing completely to a background thread
        // to prevent any UI thread frame drops / lag
        sensorThread = HandlerThread("ShakeSensorThread").apply { start() }
        sensorHandler = Handler(sensorThread!!.looper)

        shakeDetector = ShakeDetector {
            mainHandler.post {
                handleShakeDetected()
            }
        }

        updateSensitivityFromPrefs()
        registerShakeListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.hasExtra(EXTRA_SENSITIVITY) == true) {
            val threshold = intent.getFloatExtra(EXTRA_SENSITIVITY, 2.7f)
            shakeDetector?.setSensitivity(threshold)
        } else {
            updateSensitivityFromPrefs()
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun updateSensitivityFromPrefs() {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val threshold = prefs.getFloat(MainActivity.KEY_SHAKE_SENSITIVITY, 2.7f)
        shakeDetector?.setSensitivity(threshold)
    }

    private fun registerShakeListener() {
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(
                shakeDetector,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL,
                sensorHandler
            )
        }
    }

    private fun handleShakeDetected() {
        // Haptic feedback to alert the user that shake was recognized
        vibrateShake()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            // Launch floating overlay directly over UPI apps
            val overlayIntent = Intent(this, FloatingOverlayService::class.java)
            startService(overlayIntent)
        } else {
            // Fallback to launching main app with quick add flag
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_TRIGGER_QUICK_ADD, true)
            }
            startActivity(appIntent)
        }
    }

    private fun vibrateShake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, ExpensePulseApplication.SHAKE_CHANNEL_ID)
            .setContentTitle("ExpensePulse Active")
            .setContentText("Shake phone after any UPI payment to quick-log")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(shakeDetector)
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SENSITIVITY = "extra_sensitivity"
    }
}
