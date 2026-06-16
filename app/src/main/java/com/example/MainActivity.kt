package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.Translations
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[AppViewModel::class.java]
        setContent {
            val settingsState by viewModel.settings.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = settingsState.darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FarmCostAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmCostAppScreen(viewModel: AppViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe state from ViewModel
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userProfile by viewModel.user.collectAsStateWithLifecycle()
    val farmsList by viewModel.farms.collectAsStateWithLifecycle()
    val cropsList by viewModel.crops.collectAsStateWithLifecycle()
    val expensesList by viewModel.expenses.collectAsStateWithLifecycle()
    val workersList by viewModel.workers.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendance.collectAsStateWithLifecycle()
    val incomeList by viewModel.income.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle()
    val aiAdvice by viewModel.aiAdvice.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val lang = settingsState.preferredLanguage
    val currency = settingsState.currencySymbol

    // Dialog form triggers
    var showAddFarm by remember { mutableStateOf(false) }
    var showAddCrop by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddWorker by remember { mutableStateOf(false) }
    var showAddAttendance by remember { mutableStateOf(false) }
    var showAddIncome by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }
    var showImportBackup by remember { mutableStateOf(false) }

    // Helper translation wrapper
    fun t(key: String): String {
        return Translations.tr(key, lang)
    }

    // Edge to edge padding handling using Scaffold
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == "dashboard",
                    onClick = { viewModel.navigateTo("dashboard") },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(t("dashboard"), fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == "ledger",
                    onClick = { viewModel.navigateTo("ledger") },
                    icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Ledger") },
                    label = { Text(t("expenses"), fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_ledger")
                )
                NavigationBarItem(
                    selected = currentScreen == "farms",
                    onClick = { viewModel.navigateTo("farms") },
                    icon = { Icon(Icons.Filled.Agriculture, contentDescription = "Farms") },
                    label = { Text(t("farms"), fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_farms")
                )
                NavigationBarItem(
                    selected = currentScreen == "reports",
                    onClick = { viewModel.navigateTo("reports") },
                    icon = { Icon(Icons.Filled.Analytics, contentDescription = "Reports") },
                    label = { Text(t("reports"), fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_reports")
                )
                NavigationBarItem(
                    selected = currentScreen == "settings",
                    onClick = { viewModel.navigateTo("settings") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text(t("settings"), fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Eco,
                        contentDescription = "Farm Logo",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = t("app_name"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        val farmerGreeting = userProfile?.fullName?.let {
                            "${t("profile")}: $it (${userProfile?.village})"
                        } ?: "Offline-First Farming"
                        Text(
                            text = farmerGreeting,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (farmsList.isEmpty()) {
                                viewModel.injectDemoData()
                                Toast.makeText(context, "Loaded demo crop accounting ledger!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Ledger fully stored!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (farmsList.isEmpty()) Icons.Filled.AutoAwesome else Icons.Filled.CloudQueue,
                            contentDescription = "Sync Info",
                            tint = if (farmsList.isEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main navigation router
            when (currentScreen) {
                "dashboard" -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        farms = farmsList,
                        crops = cropsList,
                        expenses = expensesList,
                        income = incomeList,
                        currency = currency,
                        t = ::t,
                        onAddExpense = { showAddExpense = true },
                        onAddIncome = { showAddIncome = true },
                        onAddWorker = { showAddWorker = true },
                        onAddFarm = { showAddFarm = true }
                    )
                }
                "ledger" -> {
                    LedgerScreen(
                        viewModel = viewModel,
                        expenses = expensesList,
                        income = incomeList,
                        workers = workersList,
                        attendance = attendanceList,
                        crops = cropsList,
                        farms = farmsList,
                        searchQuery = searchQuery,
                        currency = currency,
                        t = ::t,
                        onAddExpense = { showAddExpense = true },
                        onAddIncome = { showAddIncome = true },
                        onAddWorker = { showAddWorker = true },
                        onMarkAttendance = { showAddAttendance = true }
                    )
                }
                "farms" -> {
                    FarmsAndCropsScreen(
                        viewModel = viewModel,
                        farms = farmsList,
                        crops = cropsList,
                        expenses = expensesList,
                        t = ::t,
                        onAddFarm = { showAddFarm = true },
                        onAddCrop = { showAddCrop = true }
                    )
                }
                "reports" -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        expenses = expensesList,
                        income = incomeList,
                        crops = cropsList,
                        farms = farmsList,
                        aiAdvice = aiAdvice,
                        isAiLoading = isAiLoading,
                        currency = currency,
                        t = ::t
                    )
                }
                "settings" -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        user = userProfile,
                        settings = settingsState,
                        t = ::t,
                        onEditProfile = { showProfileEdit = true },
                        onTriggerRestore = { showImportBackup = true }
                    )
                }
            }
        }
    }

    // ==========================================
    // DATA DIALOG FORM POPUPS
    // ==========================================

    if (showProfileEdit) {
        ProfileEditDialog(
            user = userProfile,
            t = ::t,
            onDismiss = { showProfileEdit = false },
            onSave = { name, mob, vil, tal, dist, st, preferLang ->
                viewModel.saveProfile(name, mob, vil, tal, dist, st, preferLang)
                showProfileEdit = false
            }
        )
    }

    if (showAddFarm) {
        AddFarmDialog(
            t = ::t,
            onDismiss = { showAddFarm = false },
            onSave = { name, area, unit, vil, soil, irr, note ->
                viewModel.addFarm(name, area, unit, vil, soil, irr, note)
                showAddFarm = false
            }
        )
    }

    if (showAddCrop) {
        AddCropDialog(
            farms = farmsList,
            t = ::t,
            onDismiss = { showAddCrop = false },
            onSave = { name, variety, season, farmId, planting, harvest ->
                viewModel.addCrop(name, variety, season, farmId, planting, harvest)
                showAddCrop = false
            }
        )
    }

    if (showAddExpense) {
        val verifiedFarmId = farmsList.firstOrNull()?.id ?: 0
        val verifiedCropId = cropsList.firstOrNull()?.id ?: 0
        if (farmsList.isEmpty() || cropsList.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showAddExpense = false },
                title = { Text(t("crops")) },
                text = { Text("Please add at least one Farm and one Crop first before log expenses!") },
                confirmButton = {
                    Button(onClick = { showAddExpense = false }) {
                        Text("OK")
                    }
                }
            )
        } else {
            AddExpenseDialog(
                farms = farmsList,
                crops = cropsList,
                t = ::t,
                onDismiss = { showAddExpense = false },
                onSave = { date, fId, cId, cat, amt, notes, picPath ->
                    viewModel.addExpense(date, fId, cId, cat, amt, notes, picPath)
                    showAddExpense = false
                }
            )
        }
    }

    if (showAddWorker) {
        AddWorkerDialog(
            t = ::t,
            onDismiss = { showAddWorker = false },
            onSave = { name, mob, type ->
                viewModel.addWorker(name, mob, type)
                showAddWorker = false
            }
        )
    }

    if (showAddAttendance) {
        if (workersList.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showAddAttendance = false },
                title = { Text(t("labor")) },
                text = { Text("Please add at least one Worker first!") },
                confirmButton = {
                    Button(onClick = { showAddAttendance = false }) {
                        Text("OK")
                    }
                }
            )
        } else {
            AddAttendanceDialog(
                workers = workersList,
                t = ::t,
                onDismiss = { showAddAttendance = false },
                onSave = { date, workerId, wage, paid ->
                    viewModel.addAttendance(date, workerId, wage, paid)
                    showAddAttendance = false
                }
            )
        }
    }

    if (showAddIncome) {
        if (cropsList.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showAddIncome = false },
                title = { Text(t("crops")) },
                text = { Text("Please add at least one Crop first!") },
                confirmButton = {
                    Button(onClick = { showAddIncome = false }) {
                        Text("OK")
                    }
                }
            )
        } else {
            AddIncomeDialog(
                crops = cropsList,
                t = ::t,
                onDismiss = { showAddIncome = false },
                onSave = { cropId, qty, unit, rate, buyer, date ->
                    viewModel.addIncome(cropId, qty, unit, rate, buyer, date)
                    showAddIncome = false
                }
            )
        }
    }

    if (showImportBackup) {
        ImportBackupDialog(
            t = ::t,
            onDismiss = { showImportBackup = false },
            onImport = { json ->
                coroutineScope.launch {
                    val ok = viewModel.restoreBackupJson(json)
                    if (ok) {
                        Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid JSON structure. Import failed.", Toast.LENGTH_LONG).show()
                    }
                }
                showImportBackup = false
            }
        )
    }
}

// ==========================================
// SCREEN 1: DASHBOARD
// ==========================================

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    farms: List<Farm>,
    crops: List<Crop>,
    expenses: List<Expense>,
    income: List<Income>,
    currency: String,
    t: (String) -> String,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddWorker: () -> Unit,
    onAddFarm: () -> Unit
) {
    val totalExpense = expenses.sumOf { it.amount }
    val totalIncome = income.sumOf { it.amount }
    val netProfit = totalIncome - totalExpense

    val context = LocalContext.current

    // Calculate today's and monthly expenses
    val calendar = Calendar.getInstance()
    val todayStart = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val thisMonthStart = calendar.apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    val todayExpense = expenses.filter { it.date >= todayStart }.sumOf { it.amount }
    val monthlyExpense = expenses.filter { it.date >= thisMonthStart }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🌾 " + t("app_name") + " - " + t("profile"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simultaneously tracking dynamic farm expenses and seasonal crop yields live.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Metrics Grid Cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardMetricCard(
                    modifier = Modifier.weight(1f),
                    title = t("farms"),
                    value = "${farms.size}",
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.Agriculture
                )
                DashboardMetricCard(
                    modifier = Modifier.weight(1f),
                    title = t("active_crops"),
                    value = "${crops.size}",
                    color = MaterialTheme.colorScheme.secondary,
                    icon = Icons.Filled.Grass
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📊 Seasonal Balances", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = t("total_expenses"), fontSize = 12.sp, color = Color.Gray)
                            Text(text = "$currency ${"%,.2f".format(totalExpense)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = t("total_income"), fontSize = 12.sp, color = Color.Gray)
                            Text(text = "$currency ${"%,.2f".format(totalIncome)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = t("net_profit"), fontWeight = FontWeight.Bold)
                        val profitColor = if (netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        Text(
                            text = "$currency ${"%,.2f".format(netProfit)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = profitColor
                        )
                    }
                }
            }
        }

        // Action Center Rows
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardMetricCard(
                    modifier = Modifier.weight(1f),
                    title = t("today_expenses"),
                    value = "$currency ${"%,.0f".format(todayExpense)}",
                    color = MaterialTheme.colorScheme.outline,
                    icon = Icons.Filled.Today
                )
                DashboardMetricCard(
                    modifier = Modifier.weight(1f),
                    title = t("monthly_expenses"),
                    value = "$currency ${"%,.0f".format(monthlyExpense)}",
                    color = Color(0xFF795548),
                    icon = Icons.Filled.DateRange
                )
            }
        }

        // Quick Actions Grid (Minimal typing entry helpers)
        item {
            Column {
                Text(
                    text = t("quick_actions") + " " + "⚡",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        label = t("add_expense"),
                        color = Color(0xFFC62828),
                        icon = Icons.Filled.CallMade,
                        onClick = onAddExpense,
                        testTag = "btn_add_expense_quick"
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        label = t("add_sale"),
                        color = Color(0xFF2E7D32),
                        icon = Icons.Filled.CallReceived,
                        onClick = onAddIncome,
                        testTag = "btn_add_sale_quick"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        label = t("add_worker"),
                        color = Color(0xFF3F51B5),
                        icon = Icons.Filled.People,
                        onClick = onAddWorker,
                        testTag = "btn_add_worker_quick"
                    )
                    QuickActionButton(
                        modifier = Modifier.weight(1f),
                        label = t("add_farm"),
                        color = Color(0xFFF57C00),
                        icon = Icons.Filled.Landscape,
                        onClick = onAddFarm,
                        testTag = "btn_add_farm_quick"
                    )
                }
            }
        }

        // Custom Visual Vector Chart (Tractor / Yield Trend simulation)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📈 Seasonal Cashflow Trends",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Drawn on dynamic ledger assets completely offline",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Compose Graphics Canvas for a complete custom bar graph
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        // Draw Axes
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(40f, height - 20f),
                            end = Offset(width - 20f, height - 20f),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(40f, 10f),
                            end = Offset(40f, height - 20f),
                            strokeWidth = 3f
                        )

                        // If records are empty, show a visual text or mock curves
                        if (expenses.isEmpty() && income.isEmpty()) {
                            // Draw template grid lines
                            for (i in 1..4) {
                                val y = (height - 20f) * i / 5f
                                drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(40f, y), Offset(width, y), strokeWidth = 1f)
                            }
                        } else {
                            // Draw dynamic bar comparison (Expenses vs Income)
                            val maxVal = maxOf(totalExpense, totalIncome, 1000.0).toFloat()
                            val expenseRatio = (totalExpense.toFloat() / maxVal) * (height - 40f)
                            val incomeRatio = (totalIncome.toFloat() / maxVal) * (height - 40f)

                            // Expense Bar
                            drawRect(
                                color = Color(0xFFC62828),
                                size = Size(60f, expenseRatio),
                                topLeft = Offset(width / 3f - 30f, height - 20f - expenseRatio)
                            )

                            // Income Bar
                            drawRect(
                                color = Color(0xFF2E7D32),
                                size = Size(60f, incomeRatio),
                                topLeft = Offset(2 * width / 3f - 30f, height - 20f - incomeRatio)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFC62828)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Expenses ($currency)", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF2E7D32)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Outlay ($currency)", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier
            .height(54.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ==========================================
// SCREEN 2: MY LEDGER WITH SEARCH & TABS
// ==========================================

@Composable
fun LedgerScreen(
    viewModel: AppViewModel,
    expenses: List<Expense>,
    income: List<Income>,
    workers: List<Worker>,
    attendance: List<Attendance>,
    crops: List<Crop>,
    farms: List<Farm>,
    searchQuery: String,
    currency: String,
    t: (String) -> String,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddWorker: () -> Unit,
    onMarkAttendance: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Expenses, 1 = Income/Sales, 2 = Labor/Attendance

    // Localize on screen UI helpers
    fun farmName(id: Int) = farms.find { it.id == id }?.name ?: "Unknown Farm"
    fun cropName(id: Int) = crops.find { it.id == id }?.name ?: "Unknown Crop"
    fun workerName(id: Int) = workers.find { it.id == id }?.name ?: "Unknown Worker"

    // Unified global search filter
    val filteredExpenses = expenses.filter {
        searchQuery.isEmpty() ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.notes.contains(searchQuery, ignoreCase = true) ||
                farmName(it.farmId).contains(searchQuery, ignoreCase = true) ||
                cropName(it.cropId).contains(searchQuery, ignoreCase = true)
    }

    val filteredIncome = income.filter {
        searchQuery.isEmpty() ||
                it.buyerName.contains(searchQuery, ignoreCase = true) ||
                it.unit.contains(searchQuery, ignoreCase = true) ||
                cropName(it.cropId).contains(searchQuery, ignoreCase = true)
    }

    val filteredWorkers = workers.filter {
        searchQuery.isEmpty() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.workType.contains(searchQuery, ignoreCase = true) ||
                it.mobileNumber.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Global Search bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("ledger_search_field"),
            placeholder = { Text(t("search_hint")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Ledger Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(t("expenses"), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.MoneyOff, contentDescription = "Expense") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(t("income"), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.MonetizationOn, contentDescription = "Income") }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text(t("labor"), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Group, contentDescription = "Labor") }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                0 -> {
                    // Expenses Log List
                    if (filteredExpenses.isEmpty()) {
                        EmptyStatePrompt(t = t)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredExpenses) { item ->
                                var confirmDelete by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(categoryColor(item.category))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = item.category, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            }
                                            Text(
                                                text = "- $currency ${"%,.2f".format(item.amount)}",
                                                color = Color(0xFFC62828),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "🏠 Farm: ${farmName(item.farmId)} | 🌱 Crop: ${cropName(item.cropId)}", fontSize = 12.sp, color = Color.Gray)
                                        if (item.notes.isNotEmpty()) {
                                            Text(text = "📝: ${item.notes}", fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                        if (!item.receiptPhotoPath.isNullOrEmpty()) {
                                            Text(text = "📷 Photo/Doc Code: ${item.receiptPhotoPath}", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = formatDate(item.date), fontSize = 11.sp, color = Color.LightGray)
                                            if (confirmDelete) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    TextButton(onClick = { viewModel.deleteExpense(item.id) }) {
                                                        Text("Yes", color = Color.Red, fontWeight = FontWeight.Bold)
                                                    }
                                                    TextButton(onClick = { confirmDelete = false }) {
                                                        Text("No")
                                                    }
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { confirmDelete = true },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = onAddExpense,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .testTag("fab_add_expense_screen"),
                        containerColor = Color(0xFFC62828),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Expense")
                    }
                }
                1 -> {
                    // Income Log list
                    if (filteredIncome.isEmpty()) {
                        EmptyStatePrompt(t = t)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredIncome) { item ->
                                var confirmDelete by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Crop sold: " + cropName(item.cropId), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                text = "+ $currency ${"%,.2f".format(item.amount)}",
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "📦 Quantity: ${item.quantity} ${item.unit} | 💵 Rate: $currency ${item.rate}/${item.unit}", fontSize = 12.sp, color = Color.DarkGray)
                                        Text(text = "💼 Buyer: ${item.buyerName}", fontSize = 12.sp, color = Color.Gray)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = formatDate(item.saleDate), fontSize = 11.sp, color = Color.LightGray)
                                            if (confirmDelete) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    TextButton(onClick = { viewModel.deleteIncome(item.id) }) {
                                                        Text("Yes", color = Color.Red, fontWeight = FontWeight.Bold)
                                                    }
                                                    TextButton(onClick = { confirmDelete = false }) {
                                                        Text("No")
                                                    }
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { confirmDelete = true },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = onAddIncome,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .testTag("fab_add_sale_screen"),
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Income")
                    }
                }
                2 -> {
                    // Labor & Wages tab lists
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Action row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAddWorker,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = "Add worker")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t("add_worker"), fontSize = 12.sp)
                            }
                            Button(
                                onClick = onMarkAttendance,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Clock wage")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log attendance", fontSize = 11.sp)
                            }
                        }

                        // Combine Workers & payroll list
                        if (filteredWorkers.isEmpty()) {
                            EmptyStatePrompt(t = t)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text("Workers & Attendance Log Book", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                items(filteredWorkers) { worker ->
                                    var confirmDelete by remember { mutableStateOf(false) }
                                    // Summarize worker's total wages paid vs unpaid from the attendance
                                    val workerAttendances = attendance.filter { it.workerId == worker.id }
                                    val totalEarned = workerAttendances.sumOf { it.wage }
                                    val totalPaid = workerAttendances.filter { it.paidStatus }.sumOf { it.wage }
                                    val unpaidBalance = totalEarned - totalPaid

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(text = "👨‍🌾 ${worker.name}", fontWeight = FontWeight.Bold)
                                                    Text(text = "📞 ${worker.mobileNumber} | Work: ${worker.workType}", fontSize = 12.sp, color = Color.Gray)
                                                }
                                                if (confirmDelete) {
                                                    IconButton(onClick = { viewModel.deleteWorker(worker.id) }) {
                                                        Icon(Icons.Filled.Check, contentDescription = "Confirm", tint = Color.Red)
                                                    }
                                                } else {
                                                    IconButton(onClick = { confirmDelete = true }) {
                                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = "Total Earned: $currency ${"%,.0f".format(totalEarned)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "Unpaid: $currency ${"%,.0f".format(unpaidBalance)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (unpaidBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun categoryColor(cat: String): Color {
    return when (cat) {
        "Seeds" -> Color(0xFF4CAF50)
        "Fertilizers" -> Color(0xFFFF9800)
        "Pesticides" -> Color(0xFFE91E63)
        "Labor" -> Color(0xFF3F51B5)
        "Machinery" -> Color(0xFF9C27B0)
        "Diesel", "Petrol" -> Color(0xFF607D8B)
        "Irrigation" -> Color(0xFF03A9F4)
        "Harvesting" -> Color(0xFF795548)
        else -> Color.Gray
    }
}

// ==========================================
// SCREEN 3: FARMS AND CROPS SCREEN
// ==========================================

@Composable
fun FarmsAndCropsScreen(
    viewModel: AppViewModel,
    farms: List<Farm>,
    crops: List<Crop>,
    expenses: List<Expense>,
    t: (String) -> String,
    onAddFarm: () -> Unit,
    onAddCrop: () -> Unit
) {
    var sectionTab by remember { mutableStateOf(0) } // 0 = Farms, 1 = Crops

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = sectionTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(
                selected = sectionTab == 0,
                onClick = { sectionTab = 0 },
                text = { Text(t("add_farm"), fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Landscape, contentDescription = "Farms") }
            )
            Tab(
                selected = sectionTab == 1,
                onClick = { sectionTab = 1 },
                text = { Text(t("crops"), fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Grass, contentDescription = "Crops") }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (sectionTab) {
                0 -> {
                    // Farms List Screen
                    if (farms.isEmpty()) {
                        EmptyStatePrompt(t = t)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(farms) { farm ->
                                var showDeleteConfirm by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "🏡 " + farm.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(text = "${farm.area} ${farm.areaUnit}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "📍 Village: ${farm.village}", fontSize = 13.sp)
                                        Text(text = "Soil: ${farm.soilType} | Irrigation: ${farm.irrigationType}", fontSize = 12.sp, color = Color.Gray)
                                        if (farm.notes.isNotEmpty()) {
                                            Text(text = "Notes: ${farm.notes}", fontSize = 12.sp, color = Color.DarkGray)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (showDeleteConfirm) {
                                                TextButton(onClick = { viewModel.deleteFarm(farm.id) }) {
                                                    Text("Confirm Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(onClick = { showDeleteConfirm = false }) {
                                                    Text("Cancel")
                                                }
                                            } else {
                                                IconButton(onClick = { showDeleteConfirm = true }) {
                                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = onAddFarm,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .testTag("fab_add_farm_screen"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Farm")
                    }
                }
                1 -> {
                    // Crops List Screen
                    if (crops.isEmpty()) {
                        EmptyStatePrompt(t = t)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(crops) { crop ->
                                val cropExpenses = expenses.filter { it.cropId == crop.id }.sumOf { it.amount }
                                val assocFarm = farms.find { it.id == crop.farmId }?.name ?: "Main Valley"
                                var showDeleteConfirm by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "🌱 " + crop.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                            Text(text = "Season: ${crop.season}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                                        }

                                        Text(text = "Variety: ${crop.variety} | Farm: $assocFarm", fontSize = 13.sp, color = Color.Gray)
                                        Text(text = "Planted: ${formatDate(crop.plantingDate)}", fontSize = 12.sp)

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Red.copy(alpha = 0.08f))
                                                .padding(6.dp)
                                        ) {
                                            Text(text = "Cumulative Crop Outlay Cost: $cropExpenses", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFC62828))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (showDeleteConfirm) {
                                                TextButton(onClick = { viewModel.deleteCrop(crop.id) }) {
                                                    Text("Confirm Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(onClick = { showDeleteConfirm = false }) {
                                                    Text("Cancel")
                                                }
                                            } else {
                                                IconButton(onClick = { showDeleteConfirm = true }) {
                                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = onAddCrop,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .testTag("fab_add_crop_screen"),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Crop")
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: REPORTS & GEMINI AI ADVISOR
// ==========================================

@Composable
fun ReportsScreen(
    viewModel: AppViewModel,
    expenses: List<Expense>,
    income: List<Income>,
    crops: List<Crop>,
    farms: List<Farm>,
    aiAdvice: String,
    isAiLoading: Boolean,
    currency: String,
    t: (String) -> String
) {
    val totalExp = expenses.sumOf { it.amount }
    val totalInc = income.sumOf { it.amount }
    val profit = totalInc - totalExp

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "📈 " + t("reports"), fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
        }

        // Gemini AI Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.testTag("gemini_advisor_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("ai_advisor"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "FarmCost AI runs a secure Gemini client matching your localized fertilizer logs, crops varieties, and ledger sheets to calculate structural crop risk.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isAiLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = t("ai_analyzing"), fontSize = 12.sp, color = Color.Gray)
                        }
                    } else if (aiAdvice.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                                .heightIn(max = 240.dp)
                        ) {
                            Text(text = aiAdvice, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.askGeminiAdvisor() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_ask_ai_advisor")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Query")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t("ask_ai"))
                    }
                }
            }
        }

        // Financial Canvas Breakdown Drawing
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📊 Ledger Slices Share (Canvas Chart)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (expenses.isEmpty()) {
                        Text(
                            text = t("empty_state"),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    } else {
                        // Custom Canvas pie slice representation of categories
                        val seedSum = expenses.filter { it.category == "Seeds" }.sumOf { it.amount }
                        val fertilizerSum = expenses.filter { it.category == "Fertilizers" }.sumOf { it.amount }
                        val chemicalSum = expenses.filter { it.category == "Pesticides" }.sumOf { it.amount }
                        val laborSum = expenses.filter { it.category == "Labor" }.sumOf { it.amount }
                        val miscSum = totalExp - (seedSum + fertilizerSum + chemicalSum + laborSum)

                        Canvas(
                            modifier = Modifier
                                .size(160.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            val seedsAngle = if (totalExp > 0) (seedSum / totalExp * 360f).toFloat() else 0f
                            val fertAngle = if (totalExp > 0) (fertilizerSum / totalExp * 360f).toFloat() else 0f
                            val chemAngle = if (totalExp > 0) (chemicalSum / totalExp * 360f).toFloat() else 0f
                            val laborAngle = if (totalExp > 0) (laborSum / totalExp * 360f).toFloat() else 0f
                            val miscAngle = 360f - (seedsAngle + fertAngle + chemAngle + laborAngle)

                            var curAngle = 0f

                            drawArc(Color(0xFF4CAF50), curAngle, seedsAngle, true)
                            curAngle += seedsAngle

                            drawArc(Color(0xFFFF9800), curAngle, fertAngle, true)
                            curAngle += fertAngle

                            drawArc(Color(0xFFE91E63), curAngle, chemAngle, true)
                            curAngle += chemAngle

                            drawArc(Color(0xFF3F51B5), curAngle, laborAngle, true)
                            curAngle += laborAngle

                            drawArc(Color.Gray, curAngle, miscAngle, true)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Labels Grid
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CategoryTag(name = "Seeds", color = Color(0xFF4CAF50), value = "$currency ${formatAmount(seedSum)}")
                                CategoryTag(name = "Fertilizers", color = Color(0xFFFF9800), value = "$currency ${formatAmount(fertilizerSum)}")
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CategoryTag(name = "Pesticides", color = Color(0xFFE91E63), value = "$currency ${formatAmount(chemicalSum)}")
                                CategoryTag(name = "Labor", color = Color(0xFF3F51B5), value = "$currency ${formatAmount(laborSum)}")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatAmount(v: Double): String = "%,.0f".format(v)

@Composable
fun CategoryTag(name: String, color: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$name: $value", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// SCREEN 5: SETTINGS & PROFILE CONTROLS
// ==========================================

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    user: User?,
    settings: AppSettings,
    t: (String) -> String,
    onEditProfile: () -> Unit,
    onTriggerRestore: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "⚙️ " + t("settings"), fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
        }

        // Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Farmer Profile Identification", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (user != null) {
                        Text(text = "Full Name: ${user.fullName}", fontSize = 14.sp)
                        Text(text = "Mobile: ${user.mobileNumber}", fontSize = 14.sp)
                        Text(text = "Village / Taluka: ${user.village} / ${user.taluka}", fontSize = 14.sp)
                        Text(text = "District / State: ${user.district} / ${user.state}", fontSize = 14.sp)
                    } else {
                        Text(text = "No profile created. Standard defaults applied.", color = Color.Gray, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onEditProfile,
                        modifier = Modifier.testTag("btn_edit_profile")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t("edit_profile"))
                    }
                }
            }
        }

        // Configuration Toggles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = t("language"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("en" to "English", "hi" to "हिंदी", "mr" to "मराठी").forEach { (code, label) ->
                            val isSel = settings.preferredLanguage == code
                            Button(
                                onClick = { viewModel.updateLanguage(code) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_lang_$code")
                            ) {
                                Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = t("currency"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("₹", "$", "€").forEach { symbol ->
                            val isSel = settings.currencySymbol == symbol
                            Button(
                                onClick = { viewModel.updateCurrency(symbol) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_cur_$symbol")
                            ) {
                                Text(text = symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = t("dark_mode"), fontWeight = FontWeight.Bold)
                        Switch(
                            checked = settings.darkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            modifier = Modifier.testTag("switch_dark_mode")
                        )
                    }
                }
            }
        }

        // Custom Secure API Key input
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                var localApiKey by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    localApiKey = viewModel.userProposedApiKey.value
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Custom Gemini API Key Override", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "Leave empty to use AI Studio system credentials", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = localApiKey,
                        onValueChange = {
                            localApiKey = it
                            viewModel.setCustomApiKey(it)
                        },
                        placeholder = { Text("API Key...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors()
                    )
                }
            }
        }

        // Backup and Restoration Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = t("backup") + " & " + t("restore"), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val json = viewModel.getBackupJson()
                                    // Copy to clipboard
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("farmcost_json", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, t("backup") + " copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Export", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onTriggerRestore,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.ContentPasteGo, contentDescription = "Import")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Restore", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatePrompt(t: (String) -> String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.WaterDrop,
            contentDescription = "Empty",
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = t("empty_state"),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

// Simple Date formatter
fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

// ==========================================
// POPS INPUT DIALOGS COMPOSABLES
// ==========================================

@Composable
fun ProfileEditDialog(
    user: User?,
    t: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(user?.fullName ?: "") }
    var mob by remember { mutableStateOf(user?.mobileNumber ?: "") }
    var vil by remember { mutableStateOf(user?.village ?: "") }
    var tal by remember { mutableStateOf(user?.taluka ?: "") }
    var dst by remember { mutableStateOf(user?.district ?: "") }
    var st by remember { mutableStateOf(user?.state ?: "") }
    var langCode by remember { mutableStateOf(user?.preferredLanguage ?: "en") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = t("edit_profile"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))

                TextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth().testTag("edit_profile_name"))
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = mob, onValueChange = { mob = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = vil, onValueChange = { vil = it }, label = { Text("Village Town") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = tal, onValueChange = { tal = it }, label = { Text(t("taluka")) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = dst, onValueChange = { dst = it }, label = { Text(t("district")) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = st, onValueChange = { st = it }, label = { Text(t("state")) }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, mob, vil, tal, dst, st, langCode) },
                        modifier = Modifier.testTag("btn_save_profile_dialog")
                    ) { Text(t("save")) }
                }
            }
        }
    }
}

@Composable
fun AddFarmDialog(
    t: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Acre") }
    var vil by remember { mutableStateOf("") }
    var soil by remember { mutableStateOf("Black Cotton Soil") }
    var irr by remember { mutableStateOf("Drip Irrigation") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = t("add_farm"), fontWeight = FontWeight.Bold, fontSize = 18.sp)

                TextField(value = name, onValueChange = { name = it }, label = { Text(t("farm_name")) }, modifier = Modifier.fillMaxWidth().testTag("add_farm_name"))
                TextField(value = area, onValueChange = { area = it }, label = { Text(t("area")) }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { unit = "Acre" }, colors = ButtonDefaults.buttonColors(containerColor = if (unit == "Acre") MaterialTheme.colorScheme.primary else Color.LightGray)) { Text("Acre") }
                    Button(onClick = { unit = "Hectare" }, colors = ButtonDefaults.buttonColors(containerColor = if (unit == "Hectare") MaterialTheme.colorScheme.primary else Color.LightGray)) { Text("Hectare") }
                }

                TextField(value = vil, onValueChange = { vil = it }, label = { Text("Village Location") }, modifier = Modifier.fillMaxWidth())
                TextField(value = soil, onValueChange = { soil = it }, label = { Text("Soil Type (e.g. Clay, Sandy)") }, modifier = Modifier.fillMaxWidth())
                TextField(value = irr, onValueChange = { irr = it }, label = { Text("Irrigation (e.g. Well, Canal)") }, modifier = Modifier.fillMaxWidth())
                TextField(value = note, onValueChange = { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, area.toDoubleOrNull() ?: 0.0, unit, vil, soil, irr, note) },
                        modifier = Modifier.testTag("btn_save_farm_dialog")
                    ) { Text(t("save")) }
                }
            }
        }
    }
}

@Composable
fun AddCropDialog(
    farms: List<Farm>,
    t: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Long, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var season by remember { mutableStateOf("Kharif") }
    var selectedFarmId by remember { mutableStateOf(farms.firstOrNull()?.id ?: 0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = t("add_crop"), fontWeight = FontWeight.Bold, fontSize = 18.sp)

                TextField(value = name, onValueChange = { name = it }, label = { Text(t("crop_name")) }, modifier = Modifier.fillMaxWidth().testTag("add_crop_name"))
                TextField(value = variety, onValueChange = { variety = it }, label = { Text(t("variety")) }, modifier = Modifier.fillMaxWidth())

                Text(text = "Season Options:")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Kharif", "Rabi", "Zaid").forEach { s ->
                        Button(
                            onClick = { season = s },
                            colors = ButtonDefaults.buttonColors(containerColor = if (season == s) MaterialTheme.colorScheme.primary else Color.LightGray),
                            modifier = Modifier.weight(1f)
                        ) { Text(s, fontSize = 10.sp) }
                    }
                }

                Text(text = "Allocate to Farm:")
                farms.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFarmId = f.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedFarmId == f.id, onClick = { selectedFarmId = f.id })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(f.name)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, variety, season, selectedFarmId, System.currentTimeMillis(), System.currentTimeMillis() + 100L * 24 * 60 * 60 * 1000) },
                        modifier = Modifier.testTag("btn_save_crop_dialog")
                    ) { Text(t("save")) }
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    farms: List<Farm>,
    crops: List<Crop>,
    t: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (Long, Int, Int, String, Double, String, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Seeds") }
    var notes by remember { mutableStateOf("") }
    var receiptCode by remember { mutableStateOf("") }
    var selectedFarmId by remember { mutableStateOf(farms.firstOrNull()?.id ?: 0) }
    var selectedCropId by remember { mutableStateOf(crops.firstOrNull()?.id ?: 0) }

    val categories = listOf("Seeds", "Fertilizers", "Pesticides", "Labor", "Diesel", "Petrol", "Irrigation", "Electricity", "Miscellaneous")

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = t("add_expense"), fontWeight = FontWeight.Bold, fontSize = 18.sp)

                TextField(value = amount, onValueChange = { amount = it }, label = { Text(t("amount")) }, modifier = Modifier.fillMaxWidth().testTag("add_expense_amount"))

                Text(text = t("category"))
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).horizontalScroll(rememberScrollState())) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxHeight()) {
                        categories.forEach { cat ->
                            val isSel = category == cat
                            Button(
                                onClick = { category = cat },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.LightGray)
                            ) { Text(cat, fontSize = 11.sp) }
                        }
                    }
                }

                Text(text = "Link to Farm & Crop:")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Farm:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        farms.forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedFarmId = f.id }) {
                                RadioButton(selected = selectedFarmId == f.id, onClick = { selectedFarmId = f.id })
                                Text(f.name, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Crop:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        crops.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedCropId = c.id }) {
                                RadioButton(selected = selectedCropId == c.id, onClick = { selectedCropId = c.id })
                                Text(c.name, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }

                TextField(value = notes, onValueChange = { notes = it }, label = { Text(t("notes")) }, modifier = Modifier.fillMaxWidth())
                TextField(value = receiptCode, onValueChange = { receiptCode = it }, label = { Text("Offline Receipt Reference Code (PNG/JPG)") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(System.currentTimeMillis(), selectedFarmId, selectedCropId, category, amount.toDoubleOrNull() ?: 0.0, notes, if (receiptCode.isEmpty()) null else receiptCode) },
                        modifier = Modifier.testTag("btn_save_expense_dialog")
                    ) { Text(t("save")) }
                }
            }
        }
    }
}

@Composable
fun AddWorkerDialog(
    t: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mob by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Harvesting Duties") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = t("add_worker"), fontWeight = FontWeight.Bold, fontSize = 18.sp)

                TextField(value = name, onValueChange = { name = it }, label = { Text(t("worker_name")) }, modifier = Modifier.fillMaxWidth().testTag("add_worker_name"))
                TextField(value = mob, onValueChange = { mob = it }, label = { Text(t("mobile")) }, modifier = Modifier.fillMaxWidth())
                TextField(value = type, onValueChange = { type = it }, label = { Text(t("work_type")) }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, mob, type) },
                        modifier = Modifier.testTag("btn_save_worker_dialog")
                    ) { Text(t("save")) }
                }
            }
        }
    }
}

@Composable
fun AddAttendanceDialog(
    workers: List<Worker>,
    t: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (Long, Int, Double, Boolean) -> Unit
) {
    var wage by remember { mutableStateOf("300") }
    var isPaid by remember { mutableStateOf(true) }
    var selectedWorkerId by remember { mutableStateOf(workers.firstOrNull()?.id ?: 0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = t("mark_attendance"), fontWeight = FontWeight.Bold, fontSize = 18.sp)

                TextField(value = wage, onValueChange = { wage = it }, label = { Text(t("wage")) }, modifier = Modifier.fillMaxWidth().testTag("add_attendance_wage"))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isPaid = !isPaid }) {
                    Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t("paid_status"))
                }

                Text(text = "Select Worker Name:")
                workers.forEach { w ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedWorkerId = w.id }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedWorkerId == w.id, onClick = { selectedWorkerId = w.id })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(w.name)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(System.currentTimeMillis(), selectedWorkerId, wage.toDoubleOrNull() ?: 1.0, isPaid) },
                        modifier = Modifier.testTag("btn_save_attendance_dialog")
                    ) { Text(t("save")) }
                }
            }
        }
    }
}

@Composable
fun AddIncomeDialog(
    crops: List<Crop>,
    t: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (Int, Double, String, Double, String, Long) -> Unit
) {
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Quintal") } // kg, Ton, Quintal
    var rate by remember { mutableStateOf("") }
    var buyer by remember { mutableStateOf("") }
    var selectedCropId by remember { mutableStateOf(crops.firstOrNull()?.id ?: 0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = t("add_sale"), fontWeight = FontWeight.Bold, fontSize = 18.sp)

                TextField(value = qty, onValueChange = { qty = it }, label = { Text(t("quantity")) }, modifier = Modifier.fillMaxWidth().testTag("add_income_qty"))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Quintal", "Ton", "kg").forEach { un ->
                        Button(
                            onClick = { unit = un },
                            colors = ButtonDefaults.buttonColors(containerColor = if (unit == un) MaterialTheme.colorScheme.primary else Color.LightGray),
                            modifier = Modifier.weight(1f)
                        ) { Text(un, fontSize = 11.sp) }
                    }
                }

                TextField(value = rate, onValueChange = { rate = it }, label = { Text(t("rate")) }, modifier = Modifier.fillMaxWidth())
                TextField(value = buyer, onValueChange = { buyer = it }, label = { Text(t("buyer")) }, modifier = Modifier.fillMaxWidth())

                Text(text = "Allocate to Crop Yield:")
                crops.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCropId = c.id }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCropId == c.id, onClick = { selectedCropId = c.id })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(c.name)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(selectedCropId, qty.toDoubleOrNull() ?: 1.0, unit, rate.toDoubleOrNull() ?: 1.0, buyer, System.currentTimeMillis()) },
                        modifier = Modifier.testTag("btn_save_income_dialog")
                    ) { Text(t("save")) }
                }
            }
        }
    }
}

@Composable
fun ImportBackupDialog(
    t: (String) -> String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = t("restore"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Paste the JSON Backup content below:", fontSize = 12.sp, color = Color.Gray)

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(t("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onImport(rawText) }) { Text("Restore") }
                }
            }
        }
    }
}
