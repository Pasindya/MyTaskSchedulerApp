package com.example.labexam02

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle the notification logic here
        // For demonstration, show a toast message
        Toast.makeText(context, "Notification received!", Toast.LENGTH_SHORT).show()
    }
}
