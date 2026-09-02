package com.expensepulse.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.expensepulse.ExpensePulseApplication
import com.expensepulse.data.Category
import com.expensepulse.data.TransactionEntity
import com.expensepulse.data.TransactionType
import com.expensepulse.parser.GPayStatementParser
import com.expensepulse.service.FloatingOverlayService
import com.expensepulse.service.ShakeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val repository by lazy { (application as ExpensePulseApplication).repository }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure background shake service is running if user previously enabled it
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SHAKE_ENABLED, false)) {
            startBackgroundShakeService()
        }

        setContent {
            ExpensePulseTheme {
                MainAppScreen(
                    onToggleShakeService = { enable -> toggleShakeService(enable) },
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onTriggerOverlayPreview = { triggerFloatingOverlay() },
                    repository = repository
                )
            }
        }
    }

    private fun toggleShakeService(enable: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHAKE_ENABLED, enable).apply()

        if (enable) {
            startBackgroundShakeService()
            Toast.makeText(this, "Shake to Log is now active in background!", Toast.LENGTH_SHORT).show()
        } else {
            stopBackgroundShakeService()
            Toast.makeText(this, "Shake to Log deactivated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBackgroundShakeService() {
        val serviceIntent = Intent(this, ShakeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopBackgroundShakeService() {
        val serviceIntent = Intent(this, ShakeService::class.java)
        stopService(serviceIntent)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun triggerFloatingOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            Toast.makeText(this, "Please allow 'Display over other apps' first", Toast.LENGTH_LONG).show()
            return
        }
        val overlayIntent = Intent(this, FloatingOverlayService::class.java)
        startService(overlayIntent)
    }

    companion object {
        const val PREFS_NAME = "expense_pulse_prefs"
        const val KEY_SHAKE_ENABLED = "shake_enabled"
        const val EXTRA_TRIGGER_QUICK_ADD = "extra_trigger_quick_add"
    }
}

// Clean Minimalist Monochrome Theme
@Composable
fun ExpensePulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF111317),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEAECEF),
            background = Color(0xFFF5F6F8),
            surface = Color.White,
            onSurface = Color(0xFF111317)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    onToggleShakeService: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onTriggerOverlayPreview: () -> Unit,
    repository: com.expensepulse.data.ExpenseRepository
) {
    var selectedTab by remember { mutableStateOf(0) }
    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ExpensePulse",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF111317)
                        )
                        Text(
                            text = "Personal Expense Manager",
                            fontSize = 11.sp,
                            color = Color(0xFF8C919E)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onTriggerOverlayPreview) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Test Shake Overlay",
                            tint = Color(0xFF111317)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Mobile-Friendly 3-Tab Bottom Navigation Bar (No text wrapping)
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF111317),
                        selectedTextColor = Color(0xFF111317),
                        indicatorColor = Color(0xFFEAECEF),
                        unselectedIconColor = Color(0xFF8C919E),
                        unselectedTextColor = Color(0xFF8C919E)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("Transactions", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF111317),
                        selectedTextColor = Color(0xFF111317),
                        indicatorColor = Color(0xFFEAECEF),
                        unselectedIconColor = Color(0xFF8C919E),
                        unselectedTextColor = Color(0xFF8C919E)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF111317),
                        selectedTextColor = Color(0xFF111317),
                        indicatorColor = Color(0xFFEAECEF),
                        unselectedIconColor = Color(0xFF8C919E),
                        unselectedTextColor = Color(0xFF8C919E)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onTriggerOverlayPreview,
                containerColor = Color(0xFF111317),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Add")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFFF5F6F8))) {
            when (selectedTab) {
                0 -> DashboardTab(transactions = transactions)
                1 -> TransactionsTab(transactions = transactions)
                2 -> SettingsTab(
                    onToggleShake = onToggleShakeService,
                    onRequestOverlay = onRequestOverlayPermission,
                    onTestOverlay = onTriggerOverlayPreview,
                    onImportText = { text ->
                        coroutineScope.launch(Dispatchers.IO) {
                            val parsed = GPayStatementParser.parseStatementText(text)
                            repository.insertAll(parsed)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardTab(transactions: List<TransactionEntity>) {
    val totalExpense = transactions
        .filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromAnalytics }
        .sumOf { it.amount }

    val totalIncome = transactions
        .filter { (it.type == TransactionType.INCOME || it.type == TransactionType.SETTLEMENT) && !it.isExcludedFromAnalytics }
        .sumOf { it.amount }

    val sbiExpense = transactions
        .filter { it.type == TransactionType.EXPENSE && it.bankAccount.contains("State Bank", ignoreCase = true) }
        .sumOf { it.amount }

    val ippbExpense = transactions
        .filter { it.type == TransactionType.EXPENSE && it.bankAccount.contains("India Post", ignoreCase = true) }
        .sumOf { it.amount }

    val cashExpense = transactions
        .filter { it.type == TransactionType.EXPENSE && it.bankAccount.contains("Cash", ignoreCase = true) }
        .sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Minimalist Total Outflow Card (Obsidian Slate)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111317)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Outflow", color = Color(0xFF8C919E), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "₹ ${"%,.2f".format(totalExpense)}",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Received / Inflow", color = Color(0xFF8C919E), fontSize = 11.sp)
                            Text("₹ ${"%,.2f".format(totalIncome)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Transactions", color = Color(0xFF8C919E), fontSize = 11.sp)
                            Text("${transactions.size} items", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // Account Breakdown
        item {
            Text("Accounts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF111317))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountCard(
                    title = "SBI 7067",
                    icon = "🏦",
                    spent = sbiExpense,
                    modifier = Modifier.weight(1f)
                )
                AccountCard(
                    title = "IPPB 2938",
                    icon = "📮",
                    spent = ippbExpense,
                    modifier = Modifier.weight(1f)
                )
                AccountCard(
                    title = "Cash",
                    icon = "💵",
                    spent = cashExpense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Category Breakdown
        item {
            Text("Spending by Category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF111317))
            Spacer(modifier = Modifier.height(8.dp))
            val categoryGroups = transactions
                .filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromAnalytics }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }

            if (categoryGroups.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No expenses logged yet", color = Color(0xFF8C919E), fontSize = 13.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryGroups.take(6).forEach { (cat, amount) ->
                        val percentage = if (totalExpense > 0) (amount / totalExpense) else 0.0
                        CategoryRow(category = cat, amount = amount, percentage = percentage)
                    }
                }
            }
        }

        // Recent Activity preview
        item {
            Text("Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF111317))
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions found", color = Color(0xFF8C919E), fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(transactions.take(8)) { tx ->
                TransactionListItem(transaction = tx)
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun AccountCard(title: String, icon: String, spent: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9EBEF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$icon $title", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111317))
            Spacer(modifier = Modifier.height(4.dp))
            Text("₹ ${"%,.0f".format(spent)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111317))
            Text("spent", fontSize = 10.sp, color = Color(0xFF8C919E))
        }
    }
}

@Composable
fun CategoryRow(category: Category, amount: Double, percentage: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9EBEF))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFF5F6F8)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.iconEmoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF111317))
                    Text("₹ ${"%,.2f".format(amount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF111317))
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = percentage.toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF111317),
                    trackColor = Color(0xFFEAECEF)
                )
            }
        }
    }
}

@Composable
fun TransactionsTab(transactions: List<TransactionEntity>) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTransactions = transactions.filter { tx ->
        val matchesQuery = tx.merchantOrPerson.contains(searchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(searchQuery)
        val matchesAccount = when (selectedFilter) {
            "ALL" -> true
            "SBI" -> tx.bankAccount.contains("7067")
            "IPPB" -> tx.bankAccount.contains("2938")
            "CASH" -> tx.bankAccount.contains("Cash", ignoreCase = true)
            else -> true
        }
        matchesQuery && matchesAccount
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search merchant or amount...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8C919E)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF111317),
                unfocusedBorderColor = Color(0xFFE3E6EB)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Account Filter Pills (Smooth row)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedFilter == "SBI",
                onClick = { selectedFilter = "SBI" },
                label = { Text("SBI 7067") }
            )
            FilterChip(
                selected = selectedFilter == "IPPB",
                onClick = { selectedFilter = "IPPB" },
                label = { Text("IPPB 2938") }
            )
            FilterChip(
                selected = selectedFilter == "CASH",
                onClick = { selectedFilter = "CASH" },
                label = { Text("Cash") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No matching transactions", color = Color(0xFF8C919E), fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredTransactions) { tx ->
                    TransactionListItem(transaction = tx)
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TransactionListItem(transaction: TransactionEntity) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isTransfer = transaction.type == TransactionType.SELF_TRANSFER
    val isSettlement = transaction.type == TransactionType.SETTLEMENT

    val amountColor = when {
        isTransfer -> Color(0xFF2563EB)
        isExpense -> Color(0xFF111317)
        else -> Color(0xFF059669)
    }

    val amountPrefix = when {
        isTransfer -> "⇄ ₹"
        isExpense -> "- ₹"
        else -> "+ ₹"
    }

    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
    val dateText = sdf.format(Date(transaction.timestamp))

    // Fix: Correctly display Cash account rather than hardcoded "Other"
    val isCash = transaction.bankAccount.contains("Cash", ignoreCase = true)
    val bankDisplayName = when {
        transaction.bankAccount.contains("7067") -> "SBI 7067"
        transaction.bankAccount.contains("2938") -> "IPPB 2938"
        isCash -> "Cash"
        transaction.bankAccount.isNotBlank() -> transaction.bankAccount
        else -> "Cash"
    }

    // Fix: Show Cash icon if paid via cash
    val displayIcon = if (isCash && transaction.category == Category.OTHER) "💵" else transaction.category.iconEmoji

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9EBEF))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF5F6F8)),
                contentAlignment = Alignment.Center
            ) {
                Text(displayIcon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantOrPerson,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF111317)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(dateText, fontSize = 11.sp, color = Color(0xFF8C919E))
                    Text("•", fontSize = 11.sp, color = Color(0xFF8C919E))
                    Text(
                        text = bankDisplayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF454854)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${"%,.2f".format(transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = amountColor
                )
                if (isTransfer) {
                    Text("Transfer", fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                } else if (isSettlement) {
                    Text("Settlement", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    onToggleShake: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onTestOverlay: () -> Unit,
    onImportText: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE) }
    
    // Fix: Read persistent state from SharedPreferences so toggle NEVER resets on app close
    var shakeEnabled by remember { mutableStateOf(prefs.getBoolean(MainActivity.KEY_SHAKE_ENABLED, false)) }
    var statementInput by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("⚙️ Settings & Gestures", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111317))
        }

        // Shake Phone to Log Card with Persistent Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9EBEF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shake Phone to Log", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF111317))
                            Text(
                                "Runs in background. Shake twice right after making a UPI payment to open the quick-add window.",
                                fontSize = 12.sp,
                                color = Color(0xFF8C919E)
                            )
                        }
                        Switch(
                            checked = shakeEnabled,
                            onCheckedChange = { isChecked ->
                                shakeEnabled = isChecked
                                prefs.edit().putBoolean(MainActivity.KEY_SHAKE_ENABLED, isChecked).apply()
                                onToggleShake(isChecked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF111317),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFD6D9E0)
                            )
                        )
                    }

                    Divider(color = Color(0xFFF0F2F5))

                    // Overlay Permission
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Floating Overlay Permission", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF111317))
                        Text(
                            "Required to show the quick-log window on top of GPay or PhonePe without exiting.",
                            fontSize = 12.sp,
                            color = Color(0xFF8C919E)
                        )
                        OutlinedButton(
                            onClick = onRequestOverlay,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF111317))
                        ) {
                            Text("Grant 'Display over other apps'", color = Color(0xFF111317), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Divider(color = Color(0xFFF0F2F5))

                    // Test Overlay
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Test Floating Overlay Now", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF111317))
                        Text(
                            "Simulate shaking or opening the floating window.",
                            fontSize = 12.sp,
                            color = Color(0xFF8C919E)
                        )
                        Button(
                            onClick = onTestOverlay,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111317)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Launch Floating Quick-Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Transferred Import Section: Inside Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9EBEF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("📄 Import Statement (PDF / Text)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF111317))
                    Text(
                        "Paste your Google Pay statement text below. It extracts transactions, tags categories, and excludes self-transfers automatically.",
                        fontSize = 12.sp,
                        color = Color(0xFF8C919E)
                    )

                    OutlinedTextField(
                        value = statementInput,
                        onValueChange = { statementInput = it },
                        placeholder = { Text("Paste statement text here (e.g. 01 Aug, 2026 04:15 PM Paid to ... ₹20)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF111317),
                            unfocusedBorderColor = Color(0xFFE3E6EB)
                        )
                    )

                    Button(
                        onClick = {
                            if (statementInput.isNotBlank()) {
                                onImportText(statementInput)
                                importStatus = "Statement imported and deduplicated successfully!"
                                statementInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111317)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process & Import Transactions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    importStatus?.let { status ->
                        Text(status, color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
