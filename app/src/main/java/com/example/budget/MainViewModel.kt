package com.example.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budget.data.AccountEntity
import com.example.budget.data.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
	private val repository: BudgetRepository = ServiceLocator.repository(application)
	private val settings = ServiceLocator.settings(application)

	private val _uiState = MutableStateFlow(UiState(accounts = repository.getAccounts().map { it.toUi() }, alertsEnabled = settings.isAlertsEnabled()))
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			repository.accountsFlow.collect { list ->
				_uiState.value = _uiState.value.copy(accounts = list.map { it.toUi() })
			}
		}
		viewModelScope.launch {
			settings.alertsEnabled.collect { enabled ->
				_uiState.value = _uiState.value.copy(alertsEnabled = enabled)
			}
		}
	}

	fun setAlertsEnabled(enabled: Boolean) { settings.setAlertsEnabled(enabled) }

	fun setAllTotalMoney(value: Double) { repository.setAllTotalMoney(value) }
	fun setAllWeeklyLimit(value: Double) { repository.setAllWeeklyLimit(value) }
	fun setAllSpentThisWeek(value: Double) { repository.setAllSpentThisWeek(value) }

	fun addAccount() {
		val new = Account(
			id = UUID.randomUUID().toString(),
			name = "Account ${_uiState.value.accounts.size + 1}",
			totalMoney = 0.0,
			weeklyLimit = 0.0,
			spentThisWeek = 0.0
		)
		saveAccount(new)
	}

	fun updateAccount(id: String, updated: Account) { repository.saveAccount(updated.toEntity()) }

	private fun saveAccount(account: Account) { repository.saveAccount(account.toEntity()) }
}

data class UiState(
	val accounts: List<Account> = emptyList(),
	val alertsEnabled: Boolean = true
)

data class Account(
	val id: String,
	val name: String,
	val totalMoney: Double,
	val weeklyLimit: Double,
	val spentThisWeek: Double
)

private fun Account.toEntity() = AccountEntity(id, name, totalMoney, weeklyLimit, spentThisWeek)
private fun AccountEntity.toUi() = Account(id, name, totalMoney, weeklyLimit, spentThisWeek)
