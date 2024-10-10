package com.example.caroilcalculator.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "倒计时结束", Toast.LENGTH_SHORT).show()
        // 这里可以添加通知或其他逻辑
    }
}