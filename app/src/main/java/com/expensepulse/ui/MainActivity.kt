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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.expensepulse.ExpensePulseApplication
import com.expensepulse.data.Category
import com.expensepulse.data.CategoryItem
import com.expensepulse.data.CategoryManager
import com.expensepulse.data.MerchantLearner
import com.expensepulse.data.TransactionEntity
import com.expensepulse.data.TransactionType
import com.expensepulse.export.CsvExporter
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
                    onUpdateShakeSensitivity = { sensitivityG -> updateShakeSensitivity(sensitivityG) },
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

    private fun updateShakeSensitivity(sensitivityG: Float) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_SHAKE_SENSITIVITY, sensitivityG).apply()

        if (prefs.getBoolean(KEY_SHAKE_ENABLED, false)) {
            val serviceIntent = Intent(this, ShakeService::class.java).apply {
                putExtra(ShakeService.EXTRA_SENSITIVITY, sensitivityG)
            }
            startService(serviceIntent)
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
        const val KEY_SHAKE_SENSITIVITY = "shake_sensitivity"
        const val EXTRA_TRIGGER_QUICK_ADD = "extra_trigger_quick_add"
    }
}

// Complete Material 3 Color Scheme & Theme for Android
@Composable
fun ExpensePulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF111317),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE4E7EB),
            onPrimaryContainer = Color(0xFF111317),
            secondary = Color(0xFF4B515D),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFEAECEF),
            onSecondaryContainer = Color(0xFF111317),
            tertiary = Color(0xFF2563EB),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFDBEAFE),
            onTertiaryContainer = Color(0xFF1E40AF),
            error = Color(0xFFDC2626),
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF991B1B),
            background = Color(0xFFF7F8FA),
            onBackground = Color(0xFF111317),
            surface = Color.White,
            onSurface = Color(0xFF111317),
            surfaceVariant = Color(0xFFF1F3F5),
            onSurfaceVariant = Color(0xFF6B7280),
            outline = Color(0xFFD1D5DB),
            outlineVariant = Color(0xFFE5E7EB)
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    onToggleShakeService: (Boolean) -> Unit,
    onUpdateShakeSensitivity: (Float) -> Unit = {},
    onRequestOverlayPermission: () -> Unit,
    onTriggerOverlayPreview: () -> Unit,
    repository: com.expensepulse.data.ExpenseRepository
) {
    var selectedTab by remember { mutableStateOf(0) }
    var categoryVersion by remember { mutableStateOf(0) }
    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for editing transaction
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    if (editingTransaction != null) {
        EditTransactionDialog(
            transaction = editingTransaction!!,
            onDismiss = { editingTransaction = null },
            onSave = { updated ->
                coroutineScope.launch(Dispatchers.IO) {
                    repository.update(updated)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Expense updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = { toDelete ->
                coroutineScope.launch(Dispatchers.IO) {
                    repository.delete(toDelete)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ExpensePulse",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personal Expense Manager",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onTriggerOverlayPreview) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Test Shake Overlay",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // Material 3 Navigation Bar with tonal elevation & active indicator
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("Transactions", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        floatingActionButton = {
            // Material 3 FAB with Squircle Shape
            FloatingActionButton(
                onClick = onTriggerOverlayPreview,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Add")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (selectedTab) {
                0 -> DashboardTab(
                    transactions = transactions,
                    onEditTransaction = { editingTransaction = it },
                    categoryVersion = categoryVersion
                )
                1 -> TransactionsTab(
                    transactions = transactions,
                    onEditTransaction = { editingTransaction = it },
                    categoryVersion = categoryVersion
                )
                2 -> SettingsTab(
                    onToggleShake = onToggleShakeService,
                    onUpdateSensitivity = onUpdateShakeSensitivity,
                    onRequestOverlay = onRequestOverlayPermission,
                    onTestOverlay = onTriggerOverlayPreview,
                    onImportText = { text ->
                        coroutineScope.launch(Dispatchers.IO) {
                            val parsed = GPayStatementParser.parseStatementText(text)
                            repository.insertAll(parsed)
                        }
                    },
                    onClearAllData = {
                        coroutineScope.launch(Dispatchers.IO) {
                            repository.clearAllTransactions()
                        }
                    },
                    categoryVersion = categoryVersion,
                    allTransactions = transactions,
                    onCategoriesUpdated = { categoryVersion++ }
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    transactions: List<TransactionEntity>,
    onEditTransaction: (TransactionEntity) -> Unit,
    categoryVersion: Int = 0
) {
    // Memoize aggregations for smooth, lag-free 60/120fps scrolling
    val totalExpense = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromAnalytics }
            .sumOf { it.amount }
    }

    val sbiExpense = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE && it.bankAccount.contains("State Bank", ignoreCase = true) }
            .sumOf { it.amount }
    }

    val ippbExpense = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE && it.bankAccount.contains("India Post", ignoreCase = true) }
            .sumOf { it.amount }
    }

    val cashExpense = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE && it.bankAccount.contains("Cash", ignoreCase = true) }
            .sumOf { it.amount }
    }

    val categoryGroups = remember(transactions, categoryVersion) {
        transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromAnalytics }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Material 3 Obsidian Total Outflow Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111317)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Outflow", color = Color(0xFF8C919E), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Excludes self-transfers", color = Color(0xFF6B7280), fontSize = 10.sp)
                    }
                    Text(
                        "₹ ${"%,.2f".format(totalExpense)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${transactions.size} transactions logged", color = Color(0xFF8C919E), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Surface(
                            color = Color(0xFF1F232B),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Multi-Account", color = Color(0xFFE5E7EB), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Material 3 Monthly Budget Tracker Card
        item {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE) }
            val budgetCap = remember { mutableStateOf(prefs.getFloat("monthly_budget_cap", 10000f).toDouble()) }
            val remaining = budgetCap.value - totalExpense
            val pct = if (budgetCap.value > 0) (totalExpense / budgetCap.value).coerceIn(0.0, 1.0) else 0.0

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Budget", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (remaining >= 0) "₹ ${"%,.2f".format(remaining)} left" else "Exceeded by ₹ ${"%,.2f".format(-remaining)}",
                                color = if (remaining >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                "Limit: ₹${"%,.0f".format(budgetCap.value)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = pct.toFloat(),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${(pct * 100).toInt()}% spent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text("Spent ₹${"%,.0f".format(totalExpense)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Account Breakdown
        item {
            Text("Accounts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
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
            Text("Spending by Category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))

            if (categoryGroups.isEmpty()) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No expenses logged yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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

        // Recent Activity preview with 1-Tap Edit Option
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Tap to edit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (transactions.isEmpty()) {
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(transactions.take(8), key = { it.id }) { tx ->
                TransactionListItem(
                    transaction = tx,
                    onClick = { onEditTransaction(tx) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun AccountCard(title: String, icon: String, spent: Double, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$icon $title", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text("₹ ${"%,.0f".format(spent)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text("spent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CategoryRow(category: Category, amount: Double, percentage: Double) {
    val context = LocalContext.current
    val categoryItem = remember(category) { CategoryManager.getCategoryItem(context, category) }

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(categoryItem.iconEmoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(categoryItem.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("₹ ${"%,.2f".format(amount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = percentage.toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun TransactionsTab(
    transactions: List<TransactionEntity>,
    onEditTransaction: (TransactionEntity) -> Unit,
    categoryVersion: Int = 0
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Fast memoized filtering
    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.merchantOrPerson.contains(searchQuery, ignoreCase = true) ||
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
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar with Material 3 styling
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search merchant or amount...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Account Filter Chips (Material 3)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All", fontWeight = if (selectedFilter == "ALL") FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedFilter == "SBI",
                onClick = { selectedFilter = "SBI" },
                label = { Text("SBI 7067", fontWeight = if (selectedFilter == "SBI") FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedFilter == "IPPB",
                onClick = { selectedFilter = "IPPB" },
                label = { Text("IPPB 2938", fontWeight = if (selectedFilter == "IPPB") FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedFilter == "CASH",
                onClick = { selectedFilter = "CASH" },
                label = { Text("Cash", fontWeight = if (selectedFilter == "CASH") FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(12.dp)
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
                        Text("No matching transactions", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionListItem(
                        transaction = tx,
                        onClick = { onEditTransaction(tx) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: TransactionEntity,
    onClick: () -> Unit = {}
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isTransfer = transaction.type == TransactionType.SELF_TRANSFER
    val isSettlement = transaction.type == TransactionType.SETTLEMENT

    val amountColor = when {
        isTransfer -> MaterialTheme.colorScheme.tertiary
        isExpense -> MaterialTheme.colorScheme.onSurface
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

    val context = LocalContext.current
    val categoryItem = remember(transaction.category) { CategoryManager.getCategoryItem(context, transaction.category) }
    val displayIcon = if (isCash && transaction.category == Category.OTHER) "💵" else categoryItem.iconEmoji

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(dateText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = bankDisplayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
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
                    Text("Transfer", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                } else if (isSettlement) {
                    Text("Settlement", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 1-Tap Edit & Delete Expense Dialog for Android Native with Material 3 styling
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf(if (transaction.amount % 1.0 == 0.0) transaction.amount.toInt().toString() else transaction.amount.toString()) }
    var merchantText by remember { mutableStateOf(transaction.merchantOrPerson) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var selectedBank by remember { mutableStateOf(transaction.bankAccount) }

    val activeCategoryItems = remember {
        CategoryManager.getCategories(context).filter { it.isEnabled }
    }

    val banks = listOf(
        "State Bank of India 7067",
        "India Post Payment Bank 2938",
        "Cash"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Expense", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount Input
                Column {
                    Text("Amount (₹)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // Note / Merchant Input
                Column {
                    Text("Note / Merchant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = merchantText,
                        onValueChange = {
                            merchantText = it
                            val predicted = MerchantLearner.predict(context, it)
                            if (predicted != null) {
                                selectedCategory = predicted
                            }
                        },
                        placeholder = { Text("e.g. Chai, Groceries, Zomato", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // Category Selection
                Column {
                    Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeCategoryItems.forEach { catItem ->
                            FilterChip(
                                selected = selectedCategory == catItem.enumCategory,
                                onClick = { selectedCategory = catItem.enumCategory },
                                label = { Text("${catItem.iconEmoji} ${catItem.displayName}", fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Bank Account Selection
                Column {
                    Text("Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        banks.forEach { bank ->
                            val label = if (bank.contains("7067")) "SBI 7067" else if (bank.contains("2938")) "IPPB 2938" else "Cash"
                            FilterChip(
                                selected = selectedBank == bank,
                                onClick = { selectedBank = bank },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull() ?: transaction.amount
                    val currentCatItem = CategoryManager.getCategoryItem(context, selectedCategory)
                    val finalNote = merchantText.trim().ifEmpty { "Paid to ${currentCatItem.displayName}" }
                    
                    // Learn user's category association
                    MerchantLearner.learn(context, finalNote, selectedCategory)

                    val updated = transaction.copy(
                        amount = amountVal,
                        merchantOrPerson = finalNote,
                        note = finalNote,
                        category = selectedCategory,
                        bankAccount = selectedBank
                    )
                    onSave(updated)
                    onDismiss()
                },
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = {
                    onDelete(transaction)
                    onDismiss()
                },
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}

// Interactive Category Customization Dialog with Material 3 styling
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryDialog(
    categoryItem: CategoryItem,
    onDismiss: () -> Unit,
    onSave: (CategoryItem) -> Unit
) {
    var nameText by remember { mutableStateOf(categoryItem.displayName) }
    var emojiText by remember { mutableStateOf(categoryItem.iconEmoji) }
    var isEnabled by remember { mutableStateOf(categoryItem.isEnabled) }

    val quickEmojis = listOf("🍔", "☕", "🛒", "🛵", "⚡", "🛍️", "🤝", "🏍️", "🍿", "💊", "📚", "🎮", "💻", "✈️", "👔", "🏠", "🏋️", "🎬", "🎁", "📝")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Customize Category", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Emoji & Quick Picker
                Column {
                    Text("Category Icon / Emoji", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = emojiText,
                            onValueChange = { if (it.length <= 4) emojiText = it },
                            modifier = Modifier.width(68.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickEmojis.forEach { emoji ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { emojiText = emoji }
                                        .border(
                                            1.dp,
                                            if (emojiText == emoji) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(10.dp)
                                        ),
                                    color = if (emojiText == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(emoji, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }

                // Category Name
                Column {
                    Text("Category Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // Active toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Show in Quick Select", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Enable for spending tags & quick entry", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = nameText.trim().ifEmpty { categoryItem.displayName }
                    val finalEmoji = emojiText.trim().ifEmpty { categoryItem.iconEmoji }
                    onSave(categoryItem.copy(displayName = finalName, iconEmoji = finalEmoji, isEnabled = isEnabled))
                    onDismiss()
                },
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save Category", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun SettingsTab(
    onToggleShake: (Boolean) -> Unit,
    onUpdateSensitivity: (Float) -> Unit = {},
    onRequestOverlay: () -> Unit,
    onTestOverlay: () -> Unit,
    onImportText: (String) -> Unit,
    onClearAllData: () -> Unit,
    categoryVersion: Int = 0,
    allTransactions: List<TransactionEntity> = emptyList(),
    onCategoriesUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE) }
    
    // Read persistent state from SharedPreferences so toggle NEVER resets on app close
    var shakeEnabled by remember { mutableStateOf(prefs.getBoolean(MainActivity.KEY_SHAKE_ENABLED, false)) }
    var shakeSensitivity by remember { mutableStateOf(prefs.getFloat(MainActivity.KEY_SHAKE_SENSITIVITY, 2.7f)) }
    var statementInput by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var editingCategoryItem by remember { mutableStateOf<CategoryItem?>(null) }

    val categories = remember(categoryVersion) { CategoryManager.getCategories(context) }

    if (editingCategoryItem != null) {
        EditCategoryDialog(
            categoryItem = editingCategoryItem!!,
            onDismiss = { editingCategoryItem = null },
            onSave = { updatedItem ->
                val updatedList = categories.map { if (it.key == updatedItem.key) updatedItem else it }
                CategoryManager.saveCategories(context, updatedList)
                onCategoriesUpdated()
                Toast.makeText(context, "Category updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("⚙️ Settings & Gestures", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        // Shake Phone to Log Card with Persistent Toggle & Sensitivity Slider (Material 3 OutlinedCard)
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shake Phone to Log", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Runs in background. Shake twice right after making a UPI payment to open the quick-add window.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = shakeEnabled,
                            onCheckedChange = { isChecked ->
                                shakeEnabled = isChecked
                                prefs.edit().putBoolean(MainActivity.KEY_SHAKE_ENABLED, isChecked).apply()
                                onToggleShake(isChecked)
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Custom Shake Sensitivity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Shake Sensitivity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            val sensitivityLabel = when {
                                shakeSensitivity < 2.3f -> "Gentle (Sensitive)"
                                shakeSensitivity > 3.1f -> "Firm (Vigorous)"
                                else -> "Medium (Recommended)"
                            }
                            Text(sensitivityLabel, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = shakeSensitivity,
                            onValueChange = {
                                shakeSensitivity = it
                                prefs.edit().putFloat(MainActivity.KEY_SHAKE_SENSITIVITY, it).apply()
                                onUpdateSensitivity(it)
                            },
                            valueRange = 1.8f..3.6f,
                            steps = 2
                        )
                        Text(
                            "Calibrate how firmly you need to shake your phone to trigger the pop-up.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Overlay Permission
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Floating Overlay Permission", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Required to show the quick-log window on top of GPay or PhonePe without exiting.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onRequestOverlay,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Grant 'Display over other apps'", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Test Overlay
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Test Floating Overlay Now", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Simulate shaking or opening the floating window.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = onTestOverlay,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Launch Floating Quick-Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Quick Settings Shade Tile Tip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", fontSize = 16.sp)
                        Text(
                            "Quick Settings: Swipe down from top of your screen & edit your Quick Settings tiles to add the '⚡ Log Expense' button for 1-tap logging anywhere.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Interactive Category Customization Card (Material 3 OutlinedCard)
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🏷️ Category Customization", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Tap any category to rename, change emoji, or enable/disable", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Clickable list of categories
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { editingCategoryItem = cat },
                                color = if (cat.isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(cat.iconEmoji, fontSize = 16.sp)
                                        Text(
                                            cat.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (cat.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!cat.isEnabled) {
                                            Text("(Disabled)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                CategoryManager.resetDefaults(context)
                                onCategoriesUpdated()
                                Toast.makeText(context, "Reset to default categories", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Reset Default Categories", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Transferred Import Section: Inside Settings (Material 3 OutlinedCard)
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("📄 Import Statement (PDF / Text)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Paste your Google Pay statement text below. It extracts transactions, tags categories, and excludes self-transfers automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = statementInput,
                        onValueChange = { statementInput = it },
                        placeholder = { Text("Paste statement text here (e.g. 01 Aug, 2026 04:15 PM Paid to ... ₹20)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
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
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
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

        // Export Ledger (Excel / CSV) Section (Material 3 OutlinedCard)
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📊 Export Ledger (Excel / CSV)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Export your entire transaction ledger as a .csv spreadsheet file to open in Microsoft Excel, Google Sheets, or share to Google Drive / WhatsApp.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            CsvExporter.exportTransactionsToCsv(context, allTransactions)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export to CSV / Excel Spreadsheet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Clear All Data (Start Fresh) Section (Material 3 OutlinedCard)
        item {
            var showClearDialog by remember { mutableStateOf(false) }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Clear All Data?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        Text(
                            "This will permanently delete all recorded transactions and reset your ledger to fresh zero. This action cannot be undone.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showClearDialog = false
                                onClearAllData()
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Yes, Clear All", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearDialog = false },
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(28.dp)
                )
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🗑️ Reset & Clear Data", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Wipe all testing transactions and start completely fresh from ₹0.00.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Transactions (Fresh Start)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
