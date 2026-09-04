package com.expensepulse.service

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.expensepulse.ui.MainActivity

/**
 * Android Quick Settings Tile for 1-Tap Quick Expense Logging.
 * Sits in the notification pull-down shade.
 */
@RequiresApi(Build.VERSION_CODES.N)
class QuickLogTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "⚡ Log Expense"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        val hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

        if (hasOverlay) {
            // Directly pop up the floating quick-log Dynamic Island window
            val overlayIntent = Intent(this, FloatingOverlayService::class.java)
            startService(overlayIntent)
        } else {
            // Open MainActivity to request permission / show quick log dialog
            val activityIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_TRIGGER_QUICK_ADD, true)
            }

            if (isLocked) {
                unlockAndRun {
                    startActivityAndCollapseCompat(activityIntent)
                }
            } else {
                startActivityAndCollapseCompat(activityIntent)
            }
        }
    }

    private fun startActivityAndCollapseCompat(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ API
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
