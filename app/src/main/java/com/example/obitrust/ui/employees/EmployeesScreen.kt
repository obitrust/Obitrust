package com.example.obitrust.ui.employees

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.obitrust.data.model.EmployeeEntity
import com.example.obitrust.ui.BusinessViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val employees by viewModel.employees.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val currency = profile?.currencySymbol ?: "GH₵"

    var showAddDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeForAttendance by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeForSalary by remember { mutableStateOf<EmployeeEntity?>(null) }

    val totalPayroll = employees.filter { it.status == "ACTIVE" }.sumOf { it.baseSalary }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    employeeToEdit = null
                    showAddDialog = true
                },
                containerColor = GoldPrimary,
                contentColor = NavyDark,
                modifier = Modifier.testTag("add_employee_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Employee")
            }
        },
        modifier = modifier.fillMaxSize().testTag("employees_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
                modifier = Modifier.fillMaxWidth().testTag("payroll_summary_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Monthly Staff Payroll Obligation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$currency ${String.format(Locale.US, "%,.2f", totalPayroll)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                    Badge(containerColor = GoldPrimary.copy(alpha = 0.2f), contentColor = GoldPrimary) {
                        Text("${employees.size} Staff Members", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Staff Directory & Access Roles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(employees) { employee ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("employee_card_${employee.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(employee.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text("Tel: ${employee.phone} • Role: ${employee.role}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Badge(
                                    containerColor = when (employee.role) {
                                        "Admin" -> GoldPrimary
                                        "Accountant" -> EmeraldProfit
                                        "Manager" -> Color(0xFF38BDF8)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = NavyDark
                                ) {
                                    Text(employee.role, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Base Salary: $currency ${String.format(Locale.US, "%,.2f", employee.baseSalary)} / month",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { employeeForAttendance = employee }) {
                                    Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Attendance")
                                }
                                TextButton(onClick = { employeeForSalary = employee }) {
                                    Icon(Icons.Default.Paid, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pay Salary", color = EmeraldProfit)
                                }
                                TextButton(onClick = {
                                    employeeToEdit = employee
                                    showAddDialog = true
                                }) {
                                    Text("Edit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf(employeeToEdit?.name ?: "") }
        var role by remember { mutableStateOf(employeeToEdit?.role ?: "Cashier") }
        var phone by remember { mutableStateOf(employeeToEdit?.phone ?: "") }
        var email by remember { mutableStateOf(employeeToEdit?.email ?: "") }
        var salaryText by remember { mutableStateOf(employeeToEdit?.baseSalary?.toString() ?: "1800") }

        val roles = listOf("Admin", "Manager", "Cashier", "Accountant", "Inventory Officer")

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (employeeToEdit == null) "Add Employee" else "Edit Employee", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Text("System Access Role:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(roles) { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r) })
                        }
                    }

                    OutlinedTextField(value = salaryText, onValueChange = { salaryText = it }, label = { Text("Base Salary ($currency / Month)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (name.isNotBlank() && phone.isNotBlank()) {
                                    viewModel.saveEmployee(
                                        EmployeeEntity(
                                            id = employeeToEdit?.id ?: 0,
                                            name = name.trim(),
                                            role = role,
                                            phone = phone.trim(),
                                            email = email.trim(),
                                            baseSalary = salaryText.toDoubleOrNull() ?: 0.0
                                        )
                                    ) {
                                        showAddDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Employee", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Attendance Dialog
    employeeForAttendance?.let { emp ->
        var selectedStatus by remember { mutableStateOf("PRESENT") }
        Dialog(onDismissRequest = { employeeForAttendance = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Attendance: ${emp.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    listOf("PRESENT" to "Present (Full Day)", "LATE" to "Late Arrival", "HALF_DAY" to "Half Day", "ABSENT" to "Absent").forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(selected = selectedStatus == code, onClick = { selectedStatus = code })
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { employeeForAttendance = null }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                viewModel.logAttendance(emp, selectedStatus, "Daily log") {
                                    employeeForAttendance = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit Log", color = NavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Salary Payment Dialog
    employeeForSalary?.let { emp ->
        var salaryAmtText by remember { mutableStateOf(emp.baseSalary.toString()) }
        var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }

        Dialog(onDismissRequest = { employeeForSalary = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Disburse Salary: ${emp.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Posts automatically as Salaries & Wages Expense in General Ledger", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(value = salaryAmtText, onValueChange = { salaryAmtText = it }, label = { Text("Salary Amount ($currency) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Text("Paying Account:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(accounts) { acc ->
                            FilterChip(selected = selectedAccountId == acc.id, onClick = { selectedAccountId = acc.id }, label = { Text(acc.name) })
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { employeeForSalary = null }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                val amt = salaryAmtText.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    viewModel.recordExpense(
                                        title = "Salary Disbursal: ${emp.name} (${emp.role})",
                                        category = "Salaries",
                                        amount = amt,
                                        paymentAccountId = selectedAccountId,
                                        paymentMethod = "BANK_TRANSFER",
                                        isRecurring = true,
                                        notes = "Monthly payroll disbursement"
                                    ) {
                                        employeeForSalary = null
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm & Disburse", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
