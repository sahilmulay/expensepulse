package com.expensepulse.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("expense_pulse_prefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("shake_enabled", false)) {
                    val serviceIntent = Intent(ctx, ShakeService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ctx.startForegroundService(serviceIntent)
                    } else {
                        ctx.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
