// CountdownService.kt
package com.example.caroilcalculator

import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class CountdownService : Service() {

//    private val binder = LocalBinder()
    private var remainingTimeMillis: Long = 0
    private var currentState: Int = 0
    private var switchState: Int = 0
    private var timer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val statusChecker = object : Runnable {
        override fun run() {
            if(remainingTimeMillis >= 0){
                updateRemainingTime()
                handler.postDelayed(this,1000)
            }else{
                createNotificationChannel()
                sendAlarmNotification()

                val intent = Intent("com.example.yourapp.COUNTDOWN_UPDATE")
                intent.putExtra("countdown_status", 0)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                sendBroadcast(intent)

                stopStatusChecker()
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ForegroundServiceChannel"

        private const val CHANNEL_NAME = "Foreground Service Channel"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("アラーム")
                .setContentText("アラームが設定されました")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        }
    }
    private fun stopStatusChecker() {
        handler.removeCallbacks(statusChecker)
    }
    private fun updateRemainingTime() {
        remainingTimeMillis -= 1000
        // 发送广播更新UI
        val intent = Intent("com.example.yourapp.COUNTDOWN_UPDATE")
        intent.putExtra("remaining_time", remainingTimeMillis)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        sendBroadcast(intent)
//        binding.countdown.text = String.format("%02d:%02d", minutes, seconds)
    }

    inner class LocalBinder : Binder() {
//        fun getService(): CountdownService = this@CountdownService
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        remainingTimeMillis = intent?.getLongExtra("countdown_time", 0) ?: 0
        startStatusChecker()
        return START_NOT_STICKY
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alarm_channel"
            val channelName = "Alarm Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel for Alarm notifications"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendAlarmNotification() {
        val notificationId = 1
        val notificationBuilder = NotificationCompat.Builder(this, "alarm_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("アラーム終了")
            .setContentText("アラームが終了しています。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }


    private fun startStatusChecker() {
        handler.post(statusChecker)
    }

    override fun onDestroy() {
        stopStatusChecker()
        super.onDestroy()
    }
}