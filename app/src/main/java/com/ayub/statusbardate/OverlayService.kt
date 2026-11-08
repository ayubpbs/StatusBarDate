package com.ayub.statusbardate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var tv: TextView? = null
    private lateinit var params: WindowManager.LayoutParams

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        startAsForeground()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        tv = TextView(this).apply { gravity = Gravity.CENTER_VERTICAL }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            statusBarHeight(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END }

        apply()
        wm.addView(tv, params)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(this, timeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // Called every time the settings screen changes something.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tv != null) apply()
        return START_STICKY
    }

    private fun apply() {
        val view = tv ?: return
        val idx = Prefs.format(this)
        val locale = if (idx == 4) Locale("bn", "BD") else Locale.ENGLISH
        view.text = SimpleDateFormat(Prefs.FORMAT_PATTERNS[idx], locale).format(Date())
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, Prefs.sizeSp(this).toFloat())
        view.setTextColor(Prefs.COLOR_VALUES[Prefs.color(this)])
        view.setTypeface(null, if (Prefs.bold(this)) Typeface.BOLD else Typeface.NORMAL)

        params.x = dp(Prefs.xDp(this))
        params.y = dp(Prefs.yDp(this))
        params.height = statusBarHeight()
        if (view.isAttachedToWindow) wm.updateViewLayout(view, params)
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun startAsForeground() {
        val channelId = "date_overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Date overlay", NotificationManager.IMPORTANCE_MIN)
        )
        val n: Notification = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_date)
            .setContentTitle("Status bar date is running")
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    override fun onDestroy() {
        try { unregisterReceiver(timeReceiver) } catch (_: Exception) {}
        tv?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        tv = null
        super.onDestroy()
    }
}
