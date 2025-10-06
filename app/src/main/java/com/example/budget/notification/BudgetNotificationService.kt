package com.example.budget.notification

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.budget.BudgetApp
import com.example.budget.R

class BudgetNotificationService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
	private val prefs by lazy { applicationContext.getSharedPreferences("budget_prefs", MODE_PRIVATE) }
	private val settings by lazy { applicationContext.getSharedPreferences("budget_settings", MODE_PRIVATE) }

	override fun onCreate() {
		super.onCreate()
		prefs.registerOnSharedPreferenceChangeListener(this)
		settings.registerOnSharedPreferenceChangeListener(this)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		startForeground(NOTIFICATION_ID, buildSummaryNotification())
		maybeShowAlert()
		return START_STICKY
	}

	override fun onDestroy() {
		prefs.unregisterOnSharedPreferenceChangeListener(this)
		settings.unregisterOnSharedPreferenceChangeListener(this)
		super.onDestroy()
	}

	private fun buildSummaryNotification(): Notification {
		val (spent, limit) = calculateTotals()
		val left = (limit - spent)
		val content = "Budget: ${format(spent)} / ${format(limit)} (${format(left.coerceAtLeast(0.0))} Left)"
		return NotificationCompat.Builder(this, BudgetApp.NOTIFICATION_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_stat_name)
			.setContentTitle("Budget Manager")
			.setContentText(content)
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.build()
	}

	private fun maybeShowAlert() {
		val alertsEnabled = settings.getBoolean("alerts_enabled", true)
		if (!alertsEnabled) return
		val (spent, limit) = calculateTotals()
		if (limit <= 0.0) return
		val ratio = spent / limit
		val message = when {
			ratio >= 1.0 -> "Over budget"
			ratio >= 0.9 -> "Close to budget"
			else -> null
		}
		if (message != null) {
			val alert = NotificationCompat.Builder(this, BudgetApp.ALERT_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_stat_name)
				.setContentTitle("Budget Alert")
				.setContentText(message)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.build()
			// Use a different ID so it appears alongside the foreground notification
			startForeground(NOTIFICATION_ID, buildSummaryNotification())
			getSystemService(android.app.NotificationManager::class.java)
				.notify(ALERT_NOTIFICATION_ID, alert)
		}
	}

	private fun calculateTotals(): Pair<Double, Double> {
		val ids = prefs.getStringSet("account_ids", emptySet()) ?: emptySet()
		var totalSpent = 0.0
		var totalLimit = 0.0
		for (id in ids) {
			val spent = (prefs.getString("spent_$id", "0") ?: "0").toDoubleOrNull() ?: 0.0
			val limit = (prefs.getString("limit_$id", "0") ?: "0").toDoubleOrNull() ?: 0.0
			totalSpent += spent
			totalLimit += limit
		}
		return totalSpent to totalLimit
	}

	private fun format(value: Double): String {
		return if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.2f", value)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		startForeground(NOTIFICATION_ID, buildSummaryNotification())
		maybeShowAlert()
	}

	override fun onBind(intent: Intent?): IBinder? = null

	companion object {
		const val NOTIFICATION_ID = 1001
		const val ALERT_NOTIFICATION_ID = 1002
	}
}
