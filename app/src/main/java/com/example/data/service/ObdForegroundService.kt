package com.example.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ObdForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "OBD2_MONITORING_CHANNEL"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_SERVICE = "com.example.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_EXIT_APP = "com.example.action.EXIT_APP"

        fun startService(context: Context) {
            val intent = Intent(context, ObdForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ObdForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SERVICE || action == ACTION_EXIT_APP) {
            if (action == ACTION_EXIT_APP) {
                // Broadcast exit intent to activities
                val exitIntent = Intent(ACTION_EXIT_APP).apply {
                    setPackage(packageName)
                }
                sendBroadcast(exitIntent)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OBD2 Мониторинг в фоновом режиме",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Сохраняет подключение к ELM327 и отслеживает датчики в фоновом режиме"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exitIntent = Intent(this, ObdForegroundService::class.java).apply {
            action = ACTION_EXIT_APP
        }
        val exitPendingIntent = PendingIntent.getService(
            this,
            1,
            exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OBD2 ИИ-Автомеханик")
            .setContentText("Фоновый мониторинг параметров автомобиля и алармов активен")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Выход из программы",
                exitPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
