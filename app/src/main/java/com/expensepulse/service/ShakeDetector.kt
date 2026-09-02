package com.expensepulse.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val onShakeListener: () -> Unit
) : SensorEventListener {

    // Sensitivity threshold: acceleration in G's
    // 2.7G is calibrated to catch clear intentional shakes while ignoring walking
    private var shakeThresholdGravity = 2.7f
    private var shakeTimestamp: Long = 0
    private var shakeCount = 0

    fun setSensitivity(threshold: Float) {
        this.shakeThresholdGravity = threshold
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // gForce will be close to 1 when there is no movement
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > shakeThresholdGravity) {
            val now = System.currentTimeMillis()
            // Ignore shake events too close to each other (debounce 500ms)
            if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }

            // Reset shake count if too much time passed since last shake
            if (shakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                shakeCount = 0
            }

            shakeTimestamp = now
            shakeCount++

            // Trigger on 2 rapid shakes
            if (shakeCount >= 2) {
                shakeCount = 0
                onShakeListener()
            }
        }
    }

    companion object {
        private const val SHAKE_SLOP_TIME_MS = 350
        private const val SHAKE_COUNT_RESET_TIME_MS = 1500
    }
}
