// CountdownService.kt
package com.example.yourapp

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.caroilcalculator.MainActivity
import com.example.caroilcalculator.R

class CountdownService : Service() {

//    private val binder = LocalBinder()
    private var remainingTimeMillis: Long = 0
    private var currentState: Int = 0
    private var switchState: Int = 0
    private var timer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val statusChecker = object : Runnable {
        override fun run() {
            if(currentState!=1){
                if (remainingTimeMillis > 0) {
                    remainingTimeMillis -= 1000
                    updateRemainingTime()
                    handler.postDelayed(this, 1000)
                } else {
                    if (currentState == 0) {
                        // 发送广播更新UI
                        val intent = Intent("com.example.yourapp.COUNTDOWN_UPDATE")
                        intent.putExtra("remaining_time", remainingTimeMillis)
                        intent.putExtra("remaining_time", remainingTimeMillis)
                        sendBroadcast(intent)
//                        binding.textCurrentStatus.text = "メリハリ大事、休憩しましょう！"
//                        binding.textCurrentStatus.setTextColor(resources.getColor(R.color.black))
                        handler.postDelayed(this, 1000)
                    } else if (currentState == 2) {
                        // 发送广播更新UI
                        val intent = Intent("com.example.yourapp.COUNTDOWN_UPDATE")
                        intent.putExtra("remaining_time", remainingTimeMillis)
                        sendBroadcast(intent)
//                        binding.textCurrentStatus.text = "休憩終了、仕事に戻りましょう！"
//                        binding.textCurrentStatus.setTextColor(resources.getColor(R.color.black))
                        handler.postDelayed(this, 1000)
                    }
                }
            }else{
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun stopStatusChecker() {

        handler.removeCallbacks(statusChecker)
//        WorkManager.getInstance(requireContext()).cancelAllWork()
    }
    private fun updateRemainingTime() {
        val minutes = (remainingTimeMillis / 1000) / 60
        val seconds = (remainingTimeMillis / 1000) % 60
        // 发送广播更新UI
        val intent = Intent("com.example.yourapp.COUNTDOWN_UPDATE")
        intent.putExtra("remaining_time", remainingTimeMillis)
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
        currentState = intent?.getIntExtra("current_state", 0) ?: 0

        startStatusChecker()
        return START_NOT_STICKY
    }


    private fun showNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "countdown_channel")
            .setContentTitle("倒计时结束")
            .setContentText("倒计时已结束，请查看")
//            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun startStatusChecker() {
        handler.post(statusChecker)
    }

    override fun onDestroy() {
        stopStatusChecker()
        super.onDestroy()
    }
}