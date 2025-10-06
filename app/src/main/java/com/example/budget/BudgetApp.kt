package com.example.budget

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BudgetApp : Application() {
	override fun onCreate() {
		super.onCreate()
		createNotificationChannels()
	}

	private fun createNotificationChannels() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val low = NotificationChannel(
				NOTIFICATION_CHANNEL_ID,
				"Budget Updates",
				NotificationManager.IMPORTANCE_LOW
			)
			low.description = "Always-on budget summary"

			val high = NotificationChannel(
				ALERT_CHANNEL_ID,
				"Budget Alerts",
				NotificationManager.IMPORTANCE_HIGH
			)
			high.description = "Alerts when close to or over budget"

			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(low)
			manager.createNotificationChannel(high)
		}
	}

	companion object {
		const val NOTIFICATION_CHANNEL_ID = "budget_channel"
		const val ALERT_CHANNEL_ID = "budget_alert_channel"
	}
}
