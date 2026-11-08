package com.ayub.statusbardate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var enableSwitch: Switch

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        statusText = TextView(this).apply { textSize = 14f }
        root.addView(statusText)

        root.addView(Button(this).apply {
            text = "1. Allow \"Draw over other apps\""
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            }
        })

        enableSwitch = Switch(this).apply {
            text = "2. Show date in status bar"
            textSize = 16f
            isChecked = Prefs.enabled(this@MainActivity) && Settings.canDrawOverlays(this@MainActivity)
            setOnCheckedChangeListener { _, checked ->
                if (checked && !Settings.canDrawOverlays(this@MainActivity)) {
                    isChecked = false
                    statusText.text = "Allow \"Draw over other apps\" first (button above)."
                } else {
                    Prefs.setEnabled(this@MainActivity, checked)
                    if (checked) pushToService() else stopService(Intent(this@MainActivity, OverlayService::class.java))
                }
            }
        }
        root.addView(enableSwitch)

        // Date format
        root.addView(label("Date format"))
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, Prefs.FORMAT_LABELS)
            setSelection(Prefs.format(this@MainActivity))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (pos != Prefs.format(this@MainActivity)) {
                        Prefs.setFormat(this@MainActivity, pos); pushToService()
                    }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })

        // Text size 8..20 sp
        root.addView(seek("Text size (sp)", 12, Prefs.sizeSp(this) - 8, 8) {
            Prefs.setSizeSp(this, it); pushToService()
        })
        // Distance from right edge 0..400 dp
        root.addView(seek("Distance from right edge (dp)", 400, Prefs.xDp(this), 0) {
            Prefs.setXDp(this, it); pushToService()
        })
        // Vertical nudge -20..20 dp
        root.addView(seek("Vertical nudge (dp)", 40, Prefs.yDp(this) + 20, -20) {
            Prefs.setYDp(this, it); pushToService()
        })

        // Color
        root.addView(label("Color"))
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, Prefs.COLOR_NAMES)
            setSelection(Prefs.color(this@MainActivity))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (pos != Prefs.color(this@MainActivity)) {
                        Prefs.setColor(this@MainActivity, pos); pushToService()
                    }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })

        root.addView(CheckBox(this).apply {
            text = "Bold"
            isChecked = Prefs.bold(this@MainActivity)
            setOnCheckedChangeListener { _, c -> Prefs.setBold(this@MainActivity, c); pushToService() }
        })

        root.addView(Button(this).apply {
            text = "3. Battery: set to Unrestricted"
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            }
        })
        root.addView(TextView(this).apply {
            text = "App info > App battery usage > Unrestricted. Without this, Pixel may stop the service after a while."
            textSize = 12f
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val ok = Settings.canDrawOverlays(this)
        statusText.text = if (ok) "Overlay permission: granted" else "Overlay permission: NOT granted"
        if (!ok) enableSwitch.isChecked = false
    }

    private fun label(t: String) = TextView(this).apply {
        text = t
        textSize = 14f
        setPadding(0, dp(14), 0, 0)
    }

    // offset: value = progress + offset
    private fun seek(title: String, max: Int, progress: Int, offset: Int, onChange: (Int) -> Unit): LinearLayout {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tv = label("$title: ${progress + offset}")
        box.addView(tv)
        box.addView(SeekBar(this).apply {
            this.max = max
            this.progress = progress
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    tv.text = "$title: ${p + offset}"
                    if (fromUser) onChange(p + offset)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        return box
    }

    private fun pushToService() {
        if (Prefs.enabled(this) && Settings.canDrawOverlays(this)) {
            ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
        }
    }
}
