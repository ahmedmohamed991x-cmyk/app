package com.example.budget.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {
	private val prefs: SharedPreferences =
		context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	private val _alertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ALERTS_ENABLED, true))
	val alertsEnabled: StateFlow<Boolean> = _alertsEnabled

	fun setAlertsEnabled(enabled: Boolean) {
		prefs.edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
		_alertsEnabled.value = enabled
	}

	fun isAlertsEnabled(): Boolean = _alertsEnabled.value

	companion object {
		private const val PREFS_NAME = "budget_settings"
		private const val KEY_ALERTS_ENABLED = "alerts_enabled"
	}
}
