package com.example.budget

import android.content.Context
import com.example.budget.data.BudgetRepository
import com.example.budget.data.SettingsRepository

object ServiceLocator {
	@Volatile private var repo: BudgetRepository? = null
	@Volatile private var settingsRepo: SettingsRepository? = null

	fun repository(context: Context): BudgetRepository =
		repo ?: synchronized(this) {
			repo ?: BudgetRepository(context.applicationContext).also { repo = it }
		}

	fun settings(context: Context): SettingsRepository =
		settingsRepo ?: synchronized(this) {
			settingsRepo ?: SettingsRepository(context.applicationContext).also { settingsRepo = it }
		}
}
