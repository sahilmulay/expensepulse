package com.expensepulse.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

        // Handle trigger from ShakeService fallback
        if (intent.getBooleanExtra(EXTRA_TRIGGER_QUICK_ADD, false)) {
            // Can show in-app quick add dialog
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
        val serviceIntent = Intent(this, ShakeService::class.java)
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Shake to Log is now active in background!", Toast.LENGTH_SHORT).show()
        } else {
            stopService(serviceIntent)
            Toast.makeText(this, "Shake to Log deactivated", Toast.LENGTH_SHORT).show()
        }
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
        const val EXTRA_TRIGGER_QUICK_ADD = "extra_trigger_quick_add"
    }
}

@Composable
fun ExpensePulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF006C4C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF89F8C7),
            background = Color(0xFFFBFDFA),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF191C1A)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚡ ExpensePulse",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006C4C)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onTriggerOverlayPreview) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Test Shake Overlay",
                            tint = Color(0xFF006C4C)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFBFDFA))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "History") },
                    label = { Text("Transactions") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Friends") },
                    label = { Text("Splits") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = "Import") },
                    label = { Text("Import") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onTriggerOverlayPreview,
                containerColor = Color(0xFF006C4C),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Add")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                0 -> DashboardTab(transactions = transactions)
                1 -> TransactionsTab(transactions = transactions)
                2 -> FriendsSplitTab(transactions = transactions)
                3 -> ImportStatementTab(onImportText = { text ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val parsed = GPayStatementParser.parseStatementText(text)
                        repository.insertAll(parsed)
                    }
                })
                4 -> SettingsTab(
                    onToggleShake = onToggleShakeService,
                    onRequestOverlay = onRequestOverlayPermission,
                    onTestOverlay = onTriggerOverlayPreview
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Quick Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF006C4C)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Outflow (Aug 2026)", color = Color(0xFF89F8C7), fontSize = 13.sp)
                    Text(
                        "₹ ${"%,.2f".format(totalExpense)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Received / Inflow", color = Color(0xFF89F8C7), fontSize = 12.sp)
                            Text("₹ ${"%,.2f".format(totalIncome)}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                        Column {
                            Text("Total Logged", color = Color(0xFF89F8C7), fontSize = 12.sp)
                            Text("${transactions.size} txns", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Account-wise Breakdown
        item {
            Text("Bank Accounts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF191C1A))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
        }

        // Category Breakdown
        item {
            Text("Spending by Category", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF191C1A))
            Spacer(modifier = Modifier.height(8.dp))
            val categoryGroups = transactions
                .filter { it.type == TransactionType.EXPENSE && !it.isExcludedFromAnalytics }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryGroups.take(6).forEach { (cat, amount) ->
                    val percentage = if (totalExpense > 0) (amount / totalExpense) else 0.0
                    CategoryRow(category = cat, amount = amount, percentage = percentage)
                }
            }
        }

        // Recent Activity preview
        item {
            Text("Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF191C1A))
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(transactions.take(8)) { tx ->
            TransactionListItem(transaction = tx)
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun AccountCard(title: String, icon: String, spent: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F2)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("$icon $title", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF404943))
            Spacer(modifier = Modifier.height(6.dp))
            Text("₹ ${"%,.0f".format(spent)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191C1A))
            Text("spent this month", fontSize = 11.sp, color = Color(0xFF707973))
        }
    }
}

@Composable
fun CategoryRow(category: Category, amount: Double, percentage: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.iconEmoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("₹ ${"%,.2f".format(amount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = percentage.toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF006C4C),
                    trackColor = Color(0xFFE0E6E2)
                )
            }
        }
    }
}

@Composable
fun TransactionsTab(transactions: List<TransactionEntity>) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = transactions.filter {
        it.merchantOrPerson.contains(searchQuery, ignoreCase = true) ||
        it.category.displayName.contains(searchQuery, ignoreCase = true) ||
        it.amount.toString().contains(searchQuery)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search merchant, amount, category...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("${filtered.size} Transactions", fontSize = 13.sp, color = Color(0xFF707973))
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { tx ->
                TransactionListItem(transaction = tx)
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
        isTransfer -> Color(0xFF006699)
        isExpense -> Color(0xFFBA1A1A)
        else -> Color(0xFF006C4C)
    }

    val amountPrefix = when {
        isTransfer -> "⇄ "
        isExpense -> "- ₹"
        else -> "+ ₹"
    }

    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
    val dateText = sdf.format(Date(transaction.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFF1F5F2)),
                contentAlignment = Alignment.Center
            ) {
                Text(transaction.category.iconEmoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantOrPerson,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF191C1A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dateText, fontSize = 11.sp, color = Color(0xFF707973))
                    Text("•", fontSize = 11.sp, color = Color(0xFF707973))
                    Text(
                        if (transaction.bankAccount.contains("7067")) "SBI 7067" else if (transaction.bankAccount.contains("2938")) "IPPB 2938" else "Other",
                        fontSize = 11.sp,
                        color = Color(0xFF404943)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${"%,.2f".format(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )
                if (isTransfer) {
                    Text("Transfer", fontSize = 10.sp, color = Color(0xFF006699), fontWeight = FontWeight.Bold)
                } else if (isSettlement) {
                    Text("Settlement", fontSize = 10.sp, color = Color(0xFF006C4C), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FriendsSplitTab(transactions: List<TransactionEntity>) {
    // Group transactions by friend
    val friendTransactions = transactions.filter { it.linkedFriendName != null }
    val friendGroups = friendTransactions.groupBy { it.linkedFriendName!! }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("🤝 Friends & Split Settlement", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Track recurring shared bills and repayments with roommates & friends",
                fontSize = 12.sp,
                color = Color(0xFF707973)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(friendGroups.toList()) { (friend, txList) ->
            val paidToFriend = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val receivedFromFriend = txList.filter { it.type == TransactionType.INCOME || it.type == TransactionType.SETTLEMENT }.sumOf { it.amount }
            val balance = receivedFromFriend - paidToFriend

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(friend, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(
                            text = if (balance >= 0) "Recv: +₹${"%,.0f".format(balance)}" else "Sent: -₹${"%,.0f".format(-balance)}",
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) Color(0xFF006C4C) else Color(0xFFBA1A1A),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sent: ₹${"%,.0f".format(paidToFriend)}", fontSize = 12.sp, color = Color(0xFF707973))
                        Text("Received: ₹${"%,.0f".format(receivedFromFriend)}", fontSize = 12.sp, color = Color(0xFF707973))
                        Text("${txList.size} txns", fontSize = 12.sp, color = Color(0xFF404943))
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ImportStatementTab(onImportText: (String) -> Unit) {
    var statementInput by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("📄 Import Google Pay Statement", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Paste your Google Pay PDF statement text here. The parser automatically extracts transactions, excludes self-transfers, and tags categories.",
            fontSize = 13.sp,
            color = Color(0xFF707973)
        )

        OutlinedTextField(
            value = statementInput,
            onValueChange = { statementInput = it },
            placeholder = { Text("Paste statement text here (e.g. 01 Aug, 2026 04:15 PM Paid to ... ₹20)") },
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(12.dp)
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C4C)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Process & Import Transactions")
        }

        importStatus?.let { status ->
            Text(status, color = Color(0xFF006C4C), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun SettingsTab(
    onToggleShake: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onTestOverlay: () -> Unit
) {
    var shakeEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("⚙️ Settings & Gestures", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shake Phone to Log", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Runs in background. Shake twice right after making a UPI payment to open the quick-add window.",
                            fontSize = 12.sp,
                            color = Color(0xFF707973)
                        )
                    }
                    Switch(
                        checked = shakeEnabled,
                        onCheckedChange = {
                            shakeEnabled = it
                            onToggleShake(it)
                        }
                    )
                }

                Divider()

                Column {
                    Text("Floating Overlay Permission", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Required to show the quick-log window on top of GPay or PhonePe without exiting.",
                        fontSize = 12.sp,
                        color = Color(0xFF707973)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onRequestOverlay) {
                        Text("Grant 'Display over other apps'")
                    }
                }

                Divider()

                Column {
                    Text("Test Floating Overlay Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Simulate shaking or opening the floating window.", fontSize = 12.sp, color = Color(0xFF707973))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onTestOverlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C4C))
                    ) {
                        Text("Launch Floating Quick-Add")
                    }
                }
            }
        }
    }
}
