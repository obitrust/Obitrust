package com.example.obitrust.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.obitrust.data.model.FinancialAccountEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"
    val totalLiquidCash = accounts.sumOf { it.balance }

    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTransferDialog = true },
                containerColor = GoldPrimary,
                contentColor = NavyDark,
                icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                text = { Text("Transfer Money", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("transfer_money_fab")
            )
        },
        modifier = modifier.fillMaxSize().testTag("accounts_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                    modifier = Modifier.fillMaxWidth().testTag("total_treasury_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Total Liquid Business Funds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$currency ${String.format(Locale.US, "%,.2f", totalLiquidCash)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldProfit
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Combined across Cash Drawers, Bank Accounts & Ghana Mobile Money wallets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddAccountDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Account")
                    }
                }
            }

            items(accounts) { account ->
                val (brandColor, brandIcon) = when (account.provider) {
                    "MTN" -> MtnYellow to Icons.Default.PhoneIphone
                    "Telecel" -> TelecelRed to Icons.Default.PhoneIphone
                    "AT" -> AtBlue to Icons.Default.PhoneIphone
                    "GCB", "Ecobank" -> Color(0xFF38BDF8) to Icons.Default.AccountBalance
                    else -> EmeraldProfit to Icons.Default.Payments
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("account_card_${account.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(brandColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = brandIcon, contentDescription = null, tint = brandColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(account.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${account.accountType} ${if (account.accountNumber.isNotEmpty()) "• ${account.accountNumber}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            "$currency ${String.format(Locale.US, "%,.2f", account.balance)}",
                            fontWeight = FontWeight.Bold,
                            color = if (account.balance >= 0) EmeraldProfit else CrimsonExpense,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Transfer History
            if (transfers.isNotEmpty()) {
                item {
                    Text("Inter-Account Transfer History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(transfers) { transfer ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${transfer.fromAccountName} → ${transfer.toAccountName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("$currency ${String.format(Locale.US, "%,.2f", transfer.amount)}", fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fee: $currency ${String.format(Locale.US, "%.2f", transfer.fee)}", style = MaterialTheme.typography.labelSmall, color = CrimsonExpense)
                                Text(sdf.format(Date(transfer.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Inter-Account Transfer Dialog
    if (showTransferDialog && accounts.size >= 2) {
        var fromAccount by remember { mutableStateOf(accounts[0]) }
        var toAccount by remember { mutableStateOf(accounts[1]) }
        var transferAmountText by remember { mutableStateOf("") }
        var feeText by remember { mutableStateOf("0.00") }
        var notes by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showTransferDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("transfer_funds_dialog")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Transfer Funds Between Accounts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Maintains balanced double-entry ledger and logs any MoMo/bank transfer fees", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("Source Account (From):", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                        items(accounts) { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { fromAccount = acc }
                            ) {
                                RadioButton(selected = fromAccount.id == acc.id, onClick = { fromAccount = acc })
                                Text("${acc.name} ($currency ${String.format(Locale.US, "%,.0f", acc.balance)})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Text("Destination Account (To):", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                        items(accounts.filter { it.id != fromAccount.id }) { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { toAccount = acc }
                            ) {
                                RadioButton(selected = toAccount.id == acc.id, onClick = { toAccount = acc })
                                Text("${acc.name} ($currency ${String.format(Locale.US, "%,.0f", acc.balance)})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = transferAmountText,
                        onValueChange = { transferAmountText = it },
                        label = { Text("Transfer Amount ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        label = { Text("Transfer Fee / Charges ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showTransferDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                val amt = transferAmountText.toDoubleOrNull() ?: 0.0
                                val fee = feeText.toDoubleOrNull() ?: 0.0
                                if (amt > 0 && fromAccount.id != toAccount.id) {
                                    viewModel.recordAccountTransfer(fromAccount.id, toAccount.id, amt, fee, notes) {
                                        showTransferDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f).testTag("confirm_transfer_button")
                        ) {
                            Text("Execute Transfer", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accType by remember { mutableStateOf("MOBILE_MONEY") }
        var provider by remember { mutableStateOf("MTN") }
        var accNumber by remember { mutableStateOf("") }
        var balanceText by remember { mutableStateOf("0") }

        Dialog(onDismissRequest = { showAddAccountDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add Payment Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(value = accName, onValueChange = { accName = it }, label = { Text("Account Name (e.g. Shop MoMo 2) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = accNumber, onValueChange = { accNumber = it }, label = { Text("Account / SIM Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = balanceText, onValueChange = { balanceText = it }, label = { Text("Opening Balance ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddAccountDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (accName.isNotBlank()) {
                                    viewModel.saveAccount(
                                        FinancialAccountEntity(
                                            name = accName.trim(),
                                            accountType = accType,
                                            provider = provider,
                                            accountNumber = accNumber.trim(),
                                            balance = balanceText.toDoubleOrNull() ?: 0.0
                                        )
                                    ) {
                                        showAddAccountDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Account", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
