package com.example.budget
import androidx.compose.ui.text.input.KeyboardOptions
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.budget.notification.BudgetNotificationService
import com.example.budget.ui.theme.BudgetTheme

class MainActivity : ComponentActivity() {
	private val viewModel: MainViewModel by viewModels()

	private val requestPermissionLauncher =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
			}
		}

		ContextCompat.startForegroundService(this, Intent(this, BudgetNotificationService::class.java))

		setContent {
			BudgetTheme {
				Surface(modifier = Modifier.fillMaxSize()) {
					DashboardScreen(viewModel = viewModel)
				}
			}
		}
	}
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
	val uiState by viewModel.uiState.collectAsState()

	var dialogState by remember { mutableStateOf<EditDialogState?>(null) }

	Column(
		modifier = Modifier.padding(16.dp).fillMaxSize(),
		horizontalAlignment = Alignment.Start
	) {
		Text(text = "Budget Manager", style = MaterialTheme.typography.headlineSmall)
		Spacer(Modifier.height(12.dp))
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			Button(onClick = { dialogState = EditDialogState.GlobalTotal }) { Text("Set Total Money") }
			Button(onClick = { dialogState = EditDialogState.GlobalLimit }) { Text("Set Weekly Limit") }
		}
		Spacer(Modifier.height(8.dp))
		Button(onClick = { dialogState = EditDialogState.GlobalSpent }) { Text("Set Spent This Week") }

		Spacer(Modifier.height(8.dp))
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text("Alerts")
			Spacer(Modifier.width(8.dp))
			Switch(checked = uiState.alertsEnabled, onCheckedChange = { viewModel.setAlertsEnabled(it) })
		}

		Spacer(Modifier.height(16.dp))
		AccountsList(
			uiState = uiState,
			onAdd = { viewModel.addAccount() },
			onUpdate = { id, a -> viewModel.updateAccount(id, a) },
			onEdit = { account -> dialogState = EditDialogState.Account(account) }
		)
	}

	when (val s = dialogState) {
		is EditDialogState.Account -> AccountEditDialog(
			initial = s.account,
			onDismiss = { dialogState = null },
			onSave = { viewModel.updateAccount(it.id, it); dialogState = null }
		)
		EditDialogState.GlobalTotal -> NumberPromptDialog(
			title = "Set Total Money (all)",
			onDismiss = { dialogState = null },
			onConfirm = { v -> viewModel.setAllTotalMoney(v); dialogState = null }
		)
		EditDialogState.GlobalLimit -> NumberPromptDialog(
			title = "Set Weekly Limit (all)",
			onDismiss = { dialogState = null },
			onConfirm = { v -> viewModel.setAllWeeklyLimit(v); dialogState = null }
		)
		EditDialogState.GlobalSpent -> NumberPromptDialog(
			title = "Set Spent This Week (all)",
			onDismiss = { dialogState = null },
			onConfirm = { v -> viewModel.setAllSpentThisWeek(v); dialogState = null }
		)
		null -> {}
	}
}

@Composable
fun AccountsList(
	uiState: UiState,
	onAdd: () -> Unit,
	onUpdate: (String, Account) -> Unit,
	onEdit: (Account) -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Text("Accounts", style = MaterialTheme.typography.titleMedium)
		uiState.accounts.forEach { account ->
			Card(modifier = Modifier.clickable { onEdit(account) }) {
				Column(Modifier.padding(12.dp)) {
					Text(account.name)
					Text("Budget: ${account.spentThisWeek} / ${account.weeklyLimit} (${account.weeklyLimit - account.spentThisWeek} Left)")
				}
			}
		}
		OutlinedButton(onClick = onAdd) { Text("Add Account") }
	}
}

private sealed interface EditDialogState {
	data class Account(val account: com.example.budget.Account) : EditDialogState
	object GlobalTotal : EditDialogState
	object GlobalLimit : EditDialogState
	object GlobalSpent : EditDialogState
}

@Composable
private fun AccountEditDialog(initial: Account, onDismiss: () -> Unit, onSave: (Account) -> Unit) {
	var name by remember { mutableStateOf(initial.name) }
	var total by remember { mutableStateOf(initial.totalMoney.toString()) }
	var limit by remember { mutableStateOf(initial.weeklyLimit.toString()) }
	var spent by remember { mutableStateOf(initial.spentThisWeek.toString()) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Edit Account") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
				OutlinedNumberField(value = total, onValueChange = { total = it }, label = "Total Money")
				OutlinedNumberField(value = limit, onValueChange = { limit = it }, label = "Weekly Limit")
				OutlinedNumberField(value = spent, onValueChange = { spent = it }, label = "Spent This Week")
			}
		},
		confirmButton = {
			TextButton(onClick = {
				val updated = initial.copy(
					name = name,
					totalMoney = total.toDoubleOrNull() ?: 0.0,
					weeklyLimit = limit.toDoubleOrNull() ?: 0.0,
					spentThisWeek = spent.toDoubleOrNull() ?: 0.0
				)
				onSave(updated)
			}) { Text("Save") }
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
	)
}

@Composable
private fun NumberPromptDialog(title: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
	var value by remember { mutableStateOf("") }
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			OutlinedNumberField(value = value, onValueChange = { value = it }, label = title)
		},
		confirmButton = {
			TextButton(onClick = {
				onConfirm(value.toDoubleOrNull() ?: 0.0)
			}) { Text("Apply") }
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
	)
}

@Composable
private fun OutlinedNumberField(value: String, onValueChange: (String) -> Unit, label: String) {
	OutlinedTextField(
		value = value,
		onValueChange = { input ->
			val filtered = input.filter { it.isDigit() || it == '.' }
			onValueChange(filtered)
		},
		label = { Text(label) },
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
	)
}
