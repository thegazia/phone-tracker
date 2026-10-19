package com.venice.tracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(this)
        title.text = "Tracker"
        title.textSize = 24f

        status = TextView(this)
        status.text = "Status: zaustavljen"
        status.setPadding(0, 30, 0, 30)

        val startBtn = Button(this)
        startBtn.text = "POKRENI pracenje"
        startBtn.setOnClickListener { askAndStart() }

        val stopBtn = Button(this)
        stopBtn.text = "ZAUSTAVI"
        stopBtn.setOnClickListener {
            stopService(Intent(this, TrackerService::class.java))
            status.text = "Status: zaustavljen"
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(startBtn)
        layout.addView(stopBtn)

        setContentView(layout)
    }

    private fun askAndStart() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)

        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

        if (missing.isEmpty()) startTracker()
        else requestPermissions(missing.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        startTracker()
    }

    private fun startTracker() {
        val i = Intent(this, TrackerService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        status.text = "Status: RADI"
    }
}
