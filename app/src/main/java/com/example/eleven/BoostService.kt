package com.example.eleven

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class BoostService : Service() {

    private val engine = GlobalBoostEngine()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISABLE -> disableAndStop()
            ACTION_SET_BOOST, null -> {
                val percent = intent?.getIntExtra(EXTRA_PERCENT, readPercent()) ?: readPercent()
                enableOrUpdate(percent)
            }
        }
        return START_STICKY
    }

    private fun enableOrUpdate(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        startForeground(NOTIFICATION_ID, buildNotification(clamped, "STARTING"))

        val mode = if (prefs.getBoolean(KEY_ENGINE_STARTED, false)) {
            engine.setPercent(clamped)
            engine.mode
        } else {
            engine.start(clamped)
        }

        // A service process can be recreated, so if the in-memory engine disappeared, retry once.
        val finalMode = if (mode == GlobalBoostEngine.Mode.UNSUPPORTED) engine.start(clamped) else mode
        val modeLabel = when (finalMode) {
            GlobalBoostEngine.Mode.LOUDNESS_ENHANCER -> "LOUDNESS"
            GlobalBoostEngine.Mode.EQUALIZER_FALLBACK -> "EQ FALLBACK"
            GlobalBoostEngine.Mode.UNSUPPORTED -> "UNSUPPORTED"
        }

        prefs.edit()
            .putInt(KEY_PERCENT, clamped)
            .putBoolean(KEY_ENABLED, finalMode != GlobalBoostEngine.Mode.UNSUPPORTED)
            .putBoolean(KEY_ENGINE_STARTED, finalMode != GlobalBoostEngine.Mode.UNSUPPORTED)
            .putString(KEY_ENGINE_MODE, modeLabel)
            .apply()

        if (finalMode == GlobalBoostEngine.Mode.UNSUPPORTED) {
            disableAndStop(keepUnsupportedStatus = true)
            return
        }

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(clamped, modeLabel))
    }

    private fun disableAndStop(keepUnsupportedStatus: Boolean = false) {
        engine.release()
        val editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, false)
            .putBoolean(KEY_ENGINE_STARTED, false)
        if (!keepUnsupportedStatus) editor.putString(KEY_ENGINE_MODE, "STANDBY")
        editor.apply()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(percent: Int, engineName: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val offIntent = Intent(this, BoostService::class.java).apply { action = ACTION_DISABLE }
        val offPending = PendingIntent.getService(
            this,
            1,
            offIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ELEVEN // $percent%")
            .setContentText("$engineName engine active")
            .setContentIntent(openPending)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "OFF", offPending).build())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun readPercent(): Int =
        getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_PERCENT, 125)

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SET_BOOST = "com.example.eleven.SET_BOOST"
        const val ACTION_DISABLE = "com.example.eleven.DISABLE"
        const val EXTRA_PERCENT = "percent"

        const val PREFS = "eleven"
        const val KEY_PERCENT = "boost_percent"
        const val KEY_ENABLED = "boost_enabled"
        const val KEY_ENGINE_STARTED = "engine_started"
        const val KEY_ENGINE_MODE = "engine_mode"

        private const val CHANNEL_ID = "eleven_booster"
        private const val NOTIFICATION_ID = 7719
    }
}
