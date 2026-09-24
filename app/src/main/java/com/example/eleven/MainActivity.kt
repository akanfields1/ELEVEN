package com.example.eleven

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var audioManager: AudioManager
    private lateinit var boostDial: BoostDialView
    private lateinit var boostSeek: SeekBar
    private lateinit var systemVolumeSeek: SeekBar
    private lateinit var systemVolumeLabel: TextView
    private lateinit var statusBadge: TextView
    private lateinit var engageButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences(BoostService.PREFS, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        boostDial = findViewById(R.id.boostDial)
        boostSeek = findViewById(R.id.boostSeek)
        systemVolumeSeek = findViewById(R.id.systemVolumeSeek)
        systemVolumeLabel = findViewById(R.id.systemVolumeLabel)
        statusBadge = findViewById(R.id.statusBadge)
        engageButton = findViewById(R.id.engageButton)

        setupBoostControls()
        setupSystemVolume()
        setupPresets()
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        syncSystemVolumeFromDevice()
        refreshUi()
    }

    private fun setupBoostControls() {
        val saved = prefs.getInt(BoostService.KEY_PERCENT, 125).coerceIn(100, 200)
        boostSeek.progress = saved - 100
        boostDial.boostPercent = saved

        boostSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = progress + 100
                boostDial.boostPercent = percent
                prefs.edit().putInt(BoostService.KEY_PERCENT, percent).apply()
                if (prefs.getBoolean(BoostService.KEY_ENABLED, false)) {
                    sendBoost(percent)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        engageButton.setOnClickListener {
            if (prefs.getBoolean(BoostService.KEY_ENABLED, false)) {
                stopBoost()
            } else {
                requestNotificationPermissionIfNeeded()
                sendBoost(currentBoostPercent())
            }
            handler.postDelayed({ refreshUi() }, 350)
        }
    }

    private fun setupSystemVolume() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        systemVolumeSeek.max = max
        syncSystemVolumeFromDevice()

        systemVolumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateVolumeLabel(progress, max)
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun setupPresets() {
        findViewById<Button>(R.id.presetMute).setOnClickListener {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            syncSystemVolumeFromDevice()
        }
        findViewById<Button>(R.id.preset100).setOnClickListener { setBoostPreset(100) }
        findViewById<Button>(R.id.preset125).setOnClickListener { setBoostPreset(125) }
        findViewById<Button>(R.id.preset150).setOnClickListener { setBoostPreset(150) }
        findViewById<Button>(R.id.preset175).setOnClickListener { setBoostPreset(175) }
        findViewById<Button>(R.id.presetMax).setOnClickListener { setBoostPreset(200) }
    }

    private fun setBoostPreset(percent: Int) {
        boostSeek.progress = percent - 100
        if (!prefs.getBoolean(BoostService.KEY_ENABLED, false)) {
            boostDial.boostPercent = percent
        }
    }

    private fun sendBoost(percent: Int) {
        val intent = Intent(this, BoostService::class.java).apply {
            action = BoostService.ACTION_SET_BOOST
            putExtra(BoostService.EXTRA_PERCENT, percent)
        }
        startForegroundService(intent)
        prefs.edit().putInt(BoostService.KEY_PERCENT, percent).apply()
    }

    private fun stopBoost() {
        startService(Intent(this, BoostService::class.java).apply {
            action = BoostService.ACTION_DISABLE
        })
        prefs.edit().putBoolean(BoostService.KEY_ENABLED, false).apply()
    }

    private fun refreshUi() {
        val enabled = prefs.getBoolean(BoostService.KEY_ENABLED, false)
        val mode = prefs.getString(BoostService.KEY_ENGINE_MODE, "STANDBY") ?: "STANDBY"
        val percent = prefs.getInt(BoostService.KEY_PERCENT, 125).coerceIn(100, 200)

        boostDial.active = enabled
        boostDial.boostPercent = percent
        statusBadge.text = "ENGINE // $mode"
        engageButton.text = if (enabled) "DISENGAGE BOOST" else "ENGAGE BOOST"

        handler.postDelayed({
            val latestEnabled = prefs.getBoolean(BoostService.KEY_ENABLED, false)
            val latestMode = prefs.getString(BoostService.KEY_ENGINE_MODE, "STANDBY") ?: "STANDBY"
            boostDial.active = latestEnabled
            statusBadge.text = "ENGINE // $latestMode"
            engageButton.text = if (latestEnabled) "DISENGAGE BOOST" else "ENGAGE BOOST"
        }, 500)
    }

    private fun syncSystemVolumeFromDevice() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        systemVolumeSeek.max = max
        systemVolumeSeek.progress = current
        updateVolumeLabel(current, max)
    }

    private fun updateVolumeLabel(current: Int, max: Int) {
        val pct = if (max == 0) 0 else (current * 100f / max).toInt()
        systemVolumeLabel.text = "$pct%"
    }

    private fun currentBoostPercent(): Int = boostSeek.progress + 100

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 440)
        }
    }
}
