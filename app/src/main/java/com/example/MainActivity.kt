package com.example
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.obitrust.ui.BusinessViewModel
import com.example.obitrust.ui.components.BusinessLogo
import com.example.obitrust.ui.NavigationScreen
import com.example.obitrust.ui.accounting.AccountingScreen
import com.example.obitrust.ui.accounts.AccountsScreen
import com.example.obitrust.ui.ai.AiAccountantScreen
import com.example.obitrust.ui.customers.CustomersScreen
import com.example.obitrust.ui.dashboard.DashboardScreen
import com.example.obitrust.ui.ebooks.EBooksScreen
import com.example.obitrust.ui.employees.EmployeesScreen
import com.example.obitrust.ui.expenses.ExpensesScreen
import com.example.obitrust.ui.inventory.InventoryScreen
import com.example.obitrust.ui.purchases.PurchasesScreen
import com.example.obitrust.ui.reports.ReportsScreen
import com.example.obitrust.ui.sales.SalesScreen
import com.example.obitrust.ui.settings.SettingsScreen
import com.example.obitrust.ui.suppliers.SuppliersScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BusinessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ObiTrustTheme(darkTheme = true) {
                ObiTrustApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObiTrustApp(viewModel: BusinessViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // BackHandler: return to Dashboard when on secondary screens
    if (currentScreen != NavigationScreen.DASHBOARD) {
        BackHandler {
            viewModel.setScreen(NavigationScreen.DASHBOARD)
        }
    }

    if (!isUnlocked && profile?.isPinRequired == true) {
        PinLockScreen(
            correctPin = profile?.pinCode ?: "1234",
            onSuccess = { viewModel.unlockApp(it) }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = NavySurface,
                    drawerContentColor = Color.White,
                    modifier = Modifier.width(300.dp).testTag("navigation_drawer")
                ) {
                    DrawerHeader(profile = profile)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = NavyBorder)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(getNavigationItems()) { item ->
                            val isSelected = currentScreen == item.screen
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) NavyDark else GoldPrimary
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NavyDark else Color.White
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    viewModel.setScreen(item.screen)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = GoldPrimary,
                                    unselectedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("nav_item_${item.screen.name.lowercase()}")
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = NavyBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Role: ${profile?.activeUserRole ?: "Admin"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v1.0 • Ghana", style = MaterialTheme.typography.labelSmall, color = GoldPrimary)
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = currentScreen.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = profile?.businessName ?: "OBITRUST BUSINESS MANAGER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPrimary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("drawer_menu_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = GoldPrimary)
                            }
                        },
                        actions = {
                            if (cart.isNotEmpty() && currentScreen != NavigationScreen.SALES) {
                                IconButton(
                                    onClick = { viewModel.setScreen(NavigationScreen.SALES) },
                                    modifier = Modifier.testTag("top_bar_cart_button")
                                ) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = EmeraldProfit, contentColor = Color.White) {
                                            Text("${cart.sumOf { it.quantity }}")
                                        }
                                    }) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = GoldPrimary)
                                    }
                                }
                            }
                            IconButton(
                                onClick = { viewModel.setScreen(NavigationScreen.AI_ACCOUNTANT) },
                                modifier = Modifier.testTag("top_bar_ai_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Accountant", tint = GoldPrimary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = NavyDark,
                            titleContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = NavyDark,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        val bottomScreens = listOf(
                            Triple(NavigationScreen.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
                            Triple(NavigationScreen.SALES, "Sales POS", Icons.Default.PointOfSale),
                            Triple(NavigationScreen.INVENTORY, "Inventory", Icons.Default.Inventory2),
                            Triple(NavigationScreen.ACCOUNTING, "Ledger", Icons.Default.AccountBalance),
                            Triple(NavigationScreen.AI_ACCOUNTANT, "AI Advisor", Icons.Default.AutoAwesome)
                        )

                        bottomScreens.forEach { (screen, label, icon) ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                selected = isSelected,
                                onClick = { viewModel.setScreen(screen) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = GoldPrimary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("bottom_nav_${screen.name.lowercase()}")
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize().testTag("app_scaffold")
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(NavyDark)
                ) {
                    when (currentScreen) {
                        NavigationScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        NavigationScreen.SALES -> SalesScreen(viewModel = viewModel)
                        NavigationScreen.INVENTORY -> InventoryScreen(viewModel = viewModel)
                        NavigationScreen.PURCHASES -> PurchasesScreen(viewModel = viewModel)
                        NavigationScreen.EXPENSES -> ExpensesScreen(viewModel = viewModel)
                        NavigationScreen.ACCOUNTING -> AccountingScreen(viewModel = viewModel)
                        NavigationScreen.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                        NavigationScreen.SUPPLIERS -> SuppliersScreen(viewModel = viewModel)
                        NavigationScreen.EMPLOYEES -> EmployeesScreen(viewModel = viewModel)
                        NavigationScreen.ACCOUNTS -> AccountsScreen(viewModel = viewModel)
                        NavigationScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
                        NavigationScreen.EBOOKS -> EBooksScreen(viewModel = viewModel)
                        NavigationScreen.AI_ACCOUNTANT -> AiAccountantScreen(viewModel = viewModel)
                        NavigationScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(profile: com.example.obitrust.data.model.BusinessProfileEntity?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        BusinessLogo(
            logoUri = profile?.logoUri,
            selectedPreset = profile?.selectedLogoPreset ?: "SHIELD",
            size = 56.dp,
            modifier = Modifier.testTag("drawer_business_logo")
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "OBITRUST",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
            letterSpacing = 1.sp
        )
        Text(
            text = "BUSINESS MANAGER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = profile?.businessName ?: "OBITRUST BUSINESS MANAGER",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class NavDrawerItem(
    val screen: NavigationScreen,
    val title: String,
    val icon: ImageVector
)

private fun getNavigationItems(): List<NavDrawerItem> {
    return listOf(
        NavDrawerItem(NavigationScreen.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
        NavDrawerItem(NavigationScreen.SALES, "Sales & POS", Icons.Default.PointOfSale),
        NavDrawerItem(NavigationScreen.INVENTORY, "Inventory & Stock", Icons.Default.Inventory2),
        NavDrawerItem(NavigationScreen.PURCHASES, "Stock Purchases", Icons.Default.AddBusiness),
        NavDrawerItem(NavigationScreen.EXPENSES, "Expense Tracker", Icons.Default.MoneyOff),
        NavDrawerItem(NavigationScreen.ACCOUNTING, "Bookkeeping & Ledger", Icons.Default.AccountBalance),
        NavDrawerItem(NavigationScreen.CUSTOMERS, "Customers & Debts", Icons.Default.People),
        NavDrawerItem(NavigationScreen.SUPPLIERS, "Suppliers & Payables", Icons.Default.LocalShipping),
        NavDrawerItem(NavigationScreen.EMPLOYEES, "Staff & Payroll", Icons.Default.Badge),
        NavDrawerItem(NavigationScreen.ACCOUNTS, "Accounts & MoMo", Icons.Default.Payments),
        NavDrawerItem(NavigationScreen.REPORTS, "Reports & Analytics", Icons.Default.BarChart),
        NavDrawerItem(NavigationScreen.EBOOKS, "E-Book Academy", Icons.Default.MenuBook),
        NavDrawerItem(NavigationScreen.AI_ACCOUNTANT, "AI Accountant", Icons.Default.AutoAwesome),
        NavDrawerItem(NavigationScreen.SETTINGS, "Settings & Taxes", Icons.Default.Settings)
    )
}

@Composable
private fun PinLockScreen(
    correctPin: String,
    onSuccess: (String) -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("OBITRUST BUSINESS MANAGER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldPrimary)
            Text("Enter your 4-digit security PIN to access company records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // PIN indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (idx < enteredPin.length) GoldPrimary else NavyCard)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage!!, color = CrimsonExpense, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numeric Keypad (1 to 9, Clear, 0, Backspace)
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "⌫")
            )

            keypad.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    row.forEach { key ->
                        Button(
                            onClick = {
                                errorMessage = null
                                when (key) {
                                    "C" -> enteredPin = ""
                                    "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                            if (enteredPin.length == 4) {
                                                if (enteredPin == correctPin) {
                                                    onSuccess(enteredPin)
                                                } else {
                                                    errorMessage = "Incorrect PIN. Default demo PIN is 1234."
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyCard),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Text(text = key, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
