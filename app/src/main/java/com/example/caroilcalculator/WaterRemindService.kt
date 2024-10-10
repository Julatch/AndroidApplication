package com.example.caroilcalculator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class WaterRemindService: Service() {
    private var remindTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 处理接收到的广播
            handler.removeCallbacks(statusChecker)
            remindTime = intent?.getLongExtra("remind_time", 0) ?: 0
            handler.post(statusChecker)

        }
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ForegroundServiceChannel2"

        private const val CHANNEL_NAME = "Foreground Service Channel2"
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
                .setContentTitle("水分補給リマインダー")
                .setContentText("水分補給リマインダーが設定されました")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        }



        val filter = IntentFilter("com.example.caroilcalculator.ALARM_ACTION")
        registerReceiver(alarmReceiver, filter)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        return START_NOT_STICKY
    }
    private val statusChecker = object : Runnable {
        override fun run() {

            handler.postDelayed(this,remindTime)
            sendAlarmNotification()
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alarm_water"
            val channelName = "Water Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel for Water notifications"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendAlarmNotification() {
        val notificationId = 1
        val notificationBuilder = NotificationCompat.Builder(this, "alarm_water")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("水分補給リマインダー")
            .setContentText("水を飲みましょう。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    override fun onDestroy() {
        handler.removeCallbacks(statusChecker)
        super.onDestroy()
    }
}