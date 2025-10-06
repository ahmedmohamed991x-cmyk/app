package com.example.budget.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AccountEntity(
	val id: String,
	val name: String,
	val totalMoney: Double,
	val weeklyLimit: Double,
	val spentThisWeek: Double
)

class BudgetRepository(private val context: Context) {
	private val prefs: SharedPreferences =
		context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	private val _accountsFlow = MutableStateFlow(loadAccounts())
	val accountsFlow: StateFlow<List<AccountEntity>> = _accountsFlow

	fun getAccounts(): List<AccountEntity> = _accountsFlow.value

	fun saveAccount(account: AccountEntity) {
		val ids = getIds().toMutableSet()
		ids.add(account.id)
		prefs.edit()
			.putStringSet(KEY_IDS, ids)
			.putString(keyName(account.id), account.name)
			.putString(keyTotal(account.id), account.totalMoney.toString())
			.putString(keyLimit(account.id), account.weeklyLimit.toString())
			.putString(keySpent(account.id), account.spentThisWeek.toString())
			.apply()
		_accountsFlow.value = loadAccounts()
	}

	fun saveAccounts(accounts: List<AccountEntity>) {
		val ids = accounts.map { it.id }.toSet()
		val editor = prefs.edit().putStringSet(KEY_IDS, ids)
		for (a in accounts) {
			editor
				.putString(keyName(a.id), a.name)
				.putString(keyTotal(a.id), a.totalMoney.toString())
				.putString(keyLimit(a.id), a.weeklyLimit.toString())
				.putString(keySpent(a.id), a.spentThisWeek.toString())
		}
		editor.apply()
		_accountsFlow.value = loadAccounts()
	}

	fun deleteAccount(id: String) {
		val ids = getIds().toMutableSet()
		if (ids.remove(id)) {
			prefs.edit()
				.putStringSet(KEY_IDS, ids)
				.remove(keyName(id))
				.remove(keyTotal(id))
				.remove(keyLimit(id))
				.remove(keySpent(id))
				.apply()
			_accountsFlow.value = loadAccounts()
		}
	}

	fun setAllTotalMoney(value: Double) {
		saveAccounts(getAccounts().map { it.copy(totalMoney = value) })
	}

	fun setAllWeeklyLimit(value: Double) {
		saveAccounts(getAccounts().map { it.copy(weeklyLimit = value) })
	}

	fun setAllSpentThisWeek(value: Double) {
		saveAccounts(getAccounts().map { it.copy(spentThisWeek = value) })
	}

	private fun loadAccounts(): List<AccountEntity> {
		return getIds().map { id ->
			AccountEntity(
				id = id,
				name = prefs.getString(keyName(id), "Account") ?: "Account",
				totalMoney = (prefs.getString(keyTotal(id), "0") ?: "0").toDoubleOrNull() ?: 0.0,
				weeklyLimit = (prefs.getString(keyLimit(id), "0") ?: "0").toDoubleOrNull() ?: 0.0,
				spentThisWeek = (prefs.getString(keySpent(id), "0") ?: "0").toDoubleOrNull() ?: 0.0
			)
		}
	}

	private fun getIds(): List<String> = (prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()).toList()

	companion object {
		private const val PREFS_NAME = "budget_prefs"
		private const val KEY_IDS = "account_ids"
		private fun keyName(id: String) = "name_$id"
		private fun keyTotal(id: String) = "total_$id"
		private fun keyLimit(id: String) = "limit_$id"
		private fun keySpent(id: String) = "spent_$id"
	}
}
