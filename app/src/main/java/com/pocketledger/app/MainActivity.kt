package com.pocketledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private var DarkModeActive = false
private val Forest get() = Color(0xFF176B4D)
private val Paper get() = if (DarkModeActive) Color(0xFF111A16) else Color(0xFFF5F5EF)
private val Ink get() = if (DarkModeActive) Color(0xFFE7EEE9) else Color(0xFF182821)
private val Muted get() = if (DarkModeActive) Color(0xFFACB9B0) else Color(0xFF728078)
private val Mint get() = if (DarkModeActive) Color(0xFF294638) else Color(0xFFDCEFE4)
private val CardColor get() = if (DarkModeActive) Color(0xFF1C2922) else Color.White
private val FieldColor get() = if (DarkModeActive) Color(0xFF25332B) else Color.White
private val categories = listOf("Travel", "Food & dining", "Groceries", "Housing", "Utilities", "Transportation", "Healthcare", "Shopping", "Entertainment", "Subscriptions", "Other")
private val categoryIcons = mapOf("Travel" to "✈", "Food & dining" to "☕", "Groceries" to "🛒", "Housing" to "⌂", "Utilities" to "ϟ", "Transportation" to "🚌", "Healthcare" to "+", "Shopping" to "◇", "Entertainment" to "♫", "Subscriptions" to "▣", "Income" to "↗", "Other" to "•")

data class LedgerTransaction(
    val id: Long, val name: String, val amount: Double, val date: String,
    val category: String, val payment: String, val note: String,
    val type: String, val need: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(245, 245, 239)
        window.navigationBarColor = android.graphics.Color.rgb(245, 245, 239)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { PocketLedgerApp() }
    }
}

@Composable
private fun PocketLedgerApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("ledger", 0) }
    var transactions by remember { mutableStateOf(loadTransactions(prefs.getString("items", null))) }
    var screen by remember { mutableStateOf("Home") }
    var editing by remember { mutableStateOf<LedgerTransaction?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var initialCategory by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf(prefs.getString("profile_name", "") ?: "") }
    var monthlyBudget by remember { mutableStateOf(prefs.getFloat("monthly_budget", 0f).toDouble()) }
    var setupComplete by remember { mutableStateOf(prefs.getBoolean("setup_complete", false)) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    fun persist(list: List<LedgerTransaction>) { transactions = list; prefs.edit().putString("items", encodeTransactions(list)).apply() }
    val spent = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
    val income = transactions.filter { it.type == "Income" }.sumOf { it.amount }
    DarkModeActive = darkMode
    SideEffect {
        (context as? ComponentActivity)?.window?.let { window ->
            val bar = if (darkMode) android.graphics.Color.rgb(17, 26, 22) else android.graphics.Color.rgb(245, 245, 239)
            window.statusBarColor = bar
            window.navigationBarColor = bar
            window.decorView.systemUiVisibility = if (darkMode) 0 else android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    MaterialTheme(colorScheme = if (darkMode) darkColorScheme(primary = Color(0xFF83C9A2), background = Paper, surface = CardColor, onBackground = Ink, onSurface = Ink) else lightColorScheme(primary = Forest, background = Paper, surface = CardColor, onBackground = Ink, onSurface = Ink)) {
        Scaffold(containerColor = Paper, bottomBar = { if (setupComplete) BottomBar(screen) { screen = it } }, floatingActionButton = {
            if (setupComplete && (screen == "Home" || screen == "Activity")) FloatingActionButton(onClick = { initialCategory = null; editing = null; showForm = true }, containerColor = Forest, contentColor = Color.White) { Text("＋", fontSize = 25.sp) }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!setupComplete) FirstRunSetup(onComplete = { name, budget -> userName = name; monthlyBudget = budget; setupComplete = true; prefs.edit().putString("profile_name", name).putFloat("monthly_budget", budget.toFloat()).putBoolean("setup_complete", true).apply() })
                else when (screen) {
                    "Home" -> HomeScreen(transactions, spent, income, userName, monthlyBudget, onProfile = { screen = "Profile" }, onAll = { screen = "Activity" }, onEdit = { editing = it; showForm = true }, onBudgets = { screen = "Budgets" }, onCategory = { selectedCategory = it }, onAddCategory = { initialCategory = it; editing = null; showForm = true })
                    "Activity" -> ActivityScreen(transactions, search, { search = it }, { initialCategory = null; editing = it; showForm = true })
                    "Budgets" -> BudgetsScreen(monthlyBudget, spent, income, transactions, onCategory = { selectedCategory = it }, onAddCategory = { initialCategory = it; editing = null; showForm = true })
                    "Profile" -> ProfileScreen(userName, monthlyBudget, darkMode, onSave = { name, budget -> userName = name; monthlyBudget = budget; prefs.edit().putString("profile_name", name).putFloat("monthly_budget", budget.toFloat()).apply(); toast = "Profile saved" }, onDarkMode = { darkMode = it; prefs.edit().putBoolean("dark_mode", it).apply() }, onBack = { screen = "Home" })
                    else -> ReportsScreen(transactions, spent, income)
                }
                if (selectedCategory != null) CategoryHistoryDialog(selectedCategory!!, transactions, onDismiss = { selectedCategory = null }, onAdd = { category -> selectedCategory = null; initialCategory = category; editing = null; showForm = true }, onEdit = { item -> selectedCategory = null; initialCategory = null; editing = item; showForm = true })
                if (showForm) TransactionDialog(editing, initialCategory, onDismiss = { showForm = false; initialCategory = null }, onSave = { item ->
                    val next = if (editing == null) transactions + item else transactions.map { if (it.id == editing!!.id) item.copy(id = editing!!.id) else it }
                    persist(next); showForm = false; initialCategory = null; toast = if (editing == null) "Transaction saved" else "Transaction updated"
                }, onDelete = { item -> persist(transactions.filterNot { it.id == item.id }); showForm = false; initialCategory = null; toast = "Transaction deleted" })
                if (toast.isNotEmpty()) { LaunchedEffect(toast) { kotlinx.coroutines.delay(1800); toast = "" }; Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), shape = RoundedCornerShape(14.dp), color = Ink, shadowElevation = 8.dp) { Text(toast, Modifier.padding(horizontal = 18.dp, vertical = 13.dp), color = Color.White) } }
            }
        }
    }
}

@Composable private fun BottomBar(selected: String, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = CardColor) {
        listOf("Home" to "⌂", "Activity" to "☷", "Budgets" to "◉", "Reports" to "▥").forEach { (name, icon) ->
            NavigationBarItem(selected == name, onClick = { onSelect(name) }, icon = { Text(icon, fontSize = 20.sp) }, label = { Text(name, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = Mint, unselectedIconColor = Muted, unselectedTextColor = Muted))
        }
    }
}

@Composable private fun HomeScreen(transactions: List<LedgerTransaction>, spent: Double, income: Double, userName: String, budget: Double, onProfile: () -> Unit, onAll: () -> Unit, onEdit: (LedgerTransaction) -> Unit, onBudgets: () -> Unit, onCategory: (String) -> Unit, onAddCategory: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Image(painterResource(R.drawable.ic_pocket_logo), contentDescription = "Pocket Ledger logo", modifier = Modifier.size(43.dp)); Column(Modifier.padding(start = 10.dp)) { Text("Pocket Ledger", fontSize = 14.sp, color = Forest, fontWeight = FontWeight.Bold); Text("Hi, $userName", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Your money, in focus", color = Muted) } }; Box(Modifier.size(42.dp).clip(CircleShape).background(Mint).clickable { onProfile() }, contentAlignment = Alignment.Center) { Text(userName.take(1).uppercase(), color = Forest, fontWeight = FontWeight.Bold) } } }
        item { SummaryCard(spent, income, budget) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { MetricCard("Daily average", cash(spent / 30), if (budget > spent) "Within your plan" else "Budget reached", Modifier.weight(1f)); MetricCard("Remaining budget", cash((budget-spent).coerceAtLeast(0.0)), if (budget > 0) "of ${cash(budget)}" else "Set a budget in Profile", Modifier.weight(1f)) } }
        item { SectionTitle("Spending by category", "See all") { onBudgets() } }
        val categoryTotals = transactions.filter { it.type == "Expense" }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
        val visibleCategories = categories
        items(visibleCategories) { name -> CategorySpending(name, categoryTotals[name] ?: 0.0, categoryTotals[name]?.let { transactions.count { tx -> tx.type == "Expense" && tx.category == name } } ?: 0, { onCategory(name) }, { onAddCategory(name) }) }
        if (transactions.isEmpty()) item { Text("Your ledger is ready. Add your first transaction to see your spending here.", color = Muted, modifier = Modifier.padding(vertical = 8.dp)) }
        item { SectionTitle("Recent activity", "See all") { onAll() } }
        items(transactions.sortedByDescending { it.date }.take(5)) { tx -> TransactionRow(tx, Modifier.clickable { onEdit(tx) }) }
        item { Text("Suggestions are based on your recorded transactions. They are not financial advice.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }
    }
}

@Composable private fun SummaryCard(spent: Double, income: Double, budget: Double) {
    val pct = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Forest).padding(22.dp)) {
        Text("THIS MONTH · SPENT", color = Color(0xFFD4E8DC), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(cash(spent), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Monthly budget", color = Color.White); Text(if (budget > 0) cash(budget) else "Not set", color = Color.White) }
        LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(7.dp).clip(CircleShape), color = Color(0xFFB6E3C4), trackColor = Color(0x5578A990))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (budget > 0) "${cash((budget-spent).coerceAtLeast(0.0))} left" else "Set a monthly budget to track progress", color = Color.White); if (budget > 0) Text("${(pct*100).toInt()}% used", color = Color.White) }
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(38.dp)) { Column { Text("Income", color = Color(0xFFC9E0D1), fontSize = 12.sp); Text(cash(income), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }; Column { Text("Saved so far", color = Color(0xFFC9E0D1), fontSize = 12.sp); Text(cash(income-spent), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) } }
    }
}

@Composable private fun MetricCard(title: String, amount: String, caption: String, modifier: Modifier = Modifier) { Column(modifier.clip(RoundedCornerShape(18.dp)).background(CardColor).padding(16.dp)) { Text("◈", color = Forest, fontSize = 20.sp); Text(title, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)); Text(amount, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(caption, color = Forest, fontSize = 11.sp) } }

@Composable private fun SectionTitle(title: String, action: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(action, color = Forest, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onClick() }.padding(6.dp)) } }

@Composable private fun CategorySpending(name: String, spent: Double, count: Int, onClick: () -> Unit, onAdd: () -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardColor).clickable { onClick() }.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(categoryIcons[name] ?: "•", fontSize = 20.sp, modifier = Modifier.width(36.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(if (count == 0) "No expenses yet" else "$count ${if (count == 1) "expense" else "expenses"}", color = Muted, fontSize = 12.sp) }; Text(cash(spent), fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Spacer(Modifier.width(10.dp)); FilledIconButton(onClick = onAdd, modifier = Modifier.size(34.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Mint, contentColor = Forest)) { Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp) } } } }

@Composable private fun TransactionRow(tx: LedgerTransaction, modifier: Modifier = Modifier) { Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardColor).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Mint), contentAlignment = Alignment.Center) { Text(categoryIcons[tx.category] ?: "•", fontSize = 19.sp) }; Column(Modifier.weight(1f).padding(start = 11.dp)) { Text(tx.name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("${tx.category} · ${tx.date} · ${tx.payment}", color = Muted, fontSize = 11.sp); if (tx.type == "Expense" && tx.need == "Needs review") Text("Needs review", color = Color(0xFFCA8A4F), fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Text((if (tx.type == "Income") "+" else "−") + cash(tx.amount), color = if (tx.type == "Income") Forest else Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) } }

@Composable private fun ActivityScreen(items: List<LedgerTransaction>, search: String, onSearch: (String) -> Unit, onEdit: (LedgerTransaction) -> Unit) { Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { Text("Transactions", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp)); Text("Your income and spending, all in one place.", color = Muted, modifier = Modifier.padding(bottom = 15.dp)); OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), placeholder = { Text("Search name, category or note") }, singleLine = true, shape = RoundedCornerShape(14.dp)); LazyColumn(contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(items.filter { (it.name + it.category + it.note).contains(search, true) }.sortedByDescending { it.date }) { TransactionRow(it, Modifier.clickable { onEdit(it) }) } } } }

@Composable private fun BudgetsScreen(budget: Double, spent: Double, income: Double, transactions: List<LedgerTransaction>, onCategory: (String) -> Unit, onAddCategory: (String) -> Unit) { val totals = transactions.filter { it.type == "Expense" }.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }; LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) { item { Text("Your budgets", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Your monthly plan and recorded spending.", color = Muted, modifier = Modifier.padding(bottom = 8.dp)) }; item { SummaryCard(spent, income, budget) }; item { Text("Category spending", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 5.dp)) }; items(categories) { name -> CategorySpending(name, totals[name] ?: 0.0, transactions.count { it.type == "Expense" && it.category == name }, { onCategory(name) }, { onAddCategory(name) }) } } }

@Composable private fun ReportsScreen(items: List<LedgerTransaction>, spent: Double, income: Double) { val expenses = items.filter { it.type == "Expense" }; val highest = expenses.groupBy { it.category }.maxByOrNull { e -> e.value.sumOf { it.amount } }; Column(Modifier.fillMaxSize().padding(20.dp)) { Text("Monthly report", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Based on transactions you have recorded", color = Muted, modifier = Modifier.padding(bottom = 16.dp)); Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardColor).padding(20.dp)) { Text("Spending at a glance", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("${cash(spent)} spent across ${expenses.map { it.category }.distinct().size} categories.", modifier = Modifier.padding(top = 8.dp)); Spacer(Modifier.height(18.dp)); Text("Income recorded", fontWeight = FontWeight.Bold); Text(cash(income), color = Forest); Spacer(Modifier.height(18.dp)); Text("Your biggest category", fontWeight = FontWeight.Bold); Text(highest?.key ?: "No expenses recorded"); Text(highest?.value?.let { cash(it.sumOf { t -> t.amount }) } ?: "Add an expense to see your category patterns", color = Muted); Spacer(Modifier.height(18.dp)); Text("Spending patterns", fontWeight = FontWeight.Bold); Text(if (expenses.isEmpty()) "Patterns will appear as you record transactions." else "Keep recording transactions to build a useful history.", color = Muted); Spacer(Modifier.height(18.dp)); Text("This report describes your recorded activity and is not financial advice.", color = Muted, fontSize = 12.sp) } } }

@Composable private fun FirstRunSetup(onComplete: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Paper).padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ic_pocket_logo), contentDescription = "Pocket Ledger logo", modifier = Modifier.size(88.dp))
        Text("Let’s set up your ledger", fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
        Text("A couple of details help make your dashboard yours.", color = Muted, modifier = Modifier.padding(top = 7.dp, bottom = 22.dp))
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("What should we call you?") }, singleLine = true, shape = RoundedCornerShape(13.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(budget, { budget = it }, Modifier.fillMaxWidth(), label = { Text("Monthly spending budget (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(13.dp))
        Text("You can change both later from your profile.", color = Muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(top = 8.dp))
        val parsed = budget.toDoubleOrNull()
        Button(onClick = { if (name.isNotBlank() && parsed != null && parsed > 0) onComplete(name.trim(), parsed) }, enabled = name.isNotBlank() && parsed != null && parsed > 0, modifier = Modifier.fillMaxWidth().padding(top = 22.dp).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Get started") }
    }
}

@Composable private fun ProfileScreen(name: String, budget: Double, darkMode: Boolean, onSave: (String, Double) -> Unit, onDarkMode: (Boolean) -> Unit, onBack: () -> Unit) {
    var editedName by remember(name) { mutableStateOf(name) }
    var editedBudget by remember(budget) { mutableStateOf(if (budget > 0) budget.toLong().toString() else "") }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("‹  Profile", color = Forest, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(vertical = 8.dp))
        Text("Your details", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Text("Update the personal details used in your app.", color = Muted, modifier = Modifier.padding(bottom = 18.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardColor).padding(16.dp)) {
            OutlinedTextField(editedName, { editedName = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(editedBudget, { editedBudget = it }, Modifier.fillMaxWidth(), label = { Text("Monthly spending budget (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp))
            Button(onClick = { editedBudget.toDoubleOrNull()?.takeIf { it > 0 }?.let { value -> if (editedName.isNotBlank()) onSave(editedName.trim(), value) } }, enabled = editedName.isNotBlank() && (editedBudget.toDoubleOrNull() ?: 0.0) > 0, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(12.dp)) { Text("Save changes") }
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(16.dp)).background(CardColor).clickable { onDarkMode(!darkMode) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Dark mode", fontWeight = FontWeight.Bold); Text("Use a darker color theme", color = Muted, fontSize = 12.sp) }
            Switch(checked = darkMode, onCheckedChange = onDarkMode)
        }
    }
}

@Composable private fun TransactionDialog(existing: LedgerTransaction?, initialCategory: String?, onDismiss: () -> Unit, onSave: (LedgerTransaction) -> Unit, onDelete: (LedgerTransaction) -> Unit) {
    var type by remember(existing, initialCategory) { mutableStateOf(existing?.type ?: "Expense") }; var amount by remember(existing, initialCategory) { mutableStateOf(existing?.amount?.toString() ?: "") }; var name by remember(existing, initialCategory) { mutableStateOf(existing?.name ?: "") }; var date by remember(existing, initialCategory) { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }; var category by remember(existing, initialCategory) { mutableStateOf(existing?.category ?: initialCategory ?: "Groceries") }; var payment by remember(existing, initialCategory) { mutableStateOf(existing?.payment ?: "Cash") }; var note by remember(existing, initialCategory) { mutableStateOf(existing?.note ?: "") }; var need by remember(existing, initialCategory) { mutableStateOf(existing?.need ?: "Needs review") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Add transaction" else "Edit transaction", fontWeight = FontWeight.Bold) }, text = {
        LazyColumn(Modifier.heightIn(max = 570.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Mint).padding(4.dp)) { listOf("Expense", "Income").forEach { Text(it, Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (it == type) CardColor else Color.Transparent).clickable { type = it; if (it == "Income") category = "Income" else if (category == "Income") category = "Groceries" }.padding(11.dp), color = if (it == type) Forest else Muted, fontWeight = FontWeight.SemiBold) } } }
            item { FormField("Amount (৳)", amount, { amount = it }, KeyboardType.Decimal) }; item { FormField("Name / merchant", name, { name = it }) }; item { FormField("Date (YYYY-MM-DD)", date, { date = it }) }
            item { DropdownField("Category", category, if (type == "Income") listOf("Income", "Salary", "Freelance", "Gift", "Other") else categories, { category = it }) }
            item { DropdownField("Payment method", payment, listOf("Cash", "Debit card", "Credit card", "Mobile wallet", "Bank transfer"), { payment = it }) }
            item { FormField("Note (optional)", note, { note = it }) }
            if (type == "Expense") item { DropdownField("Expense label", need, listOf("Needs review", "Mandatory", "Non-mandatory"), { need = it }) }
            item { Text("If a label is uncertain, leave it as Needs review.", color = Muted, fontSize = 12.sp) }
        }
    }, confirmButton = { TextButton(onClick = { val value = amount.toDoubleOrNull(); if (value != null && value > 0 && name.isNotBlank() && date.isNotBlank()) onSave(LedgerTransaction(existing?.id ?: System.currentTimeMillis(), name.trim(), value, date.trim(), category, payment, note.trim(), type, need)) }) { Text("Save") } }, dismissButton = { Row { if (existing != null) TextButton(onClick = { onDelete(existing) }) { Text("Delete", color = Color(0xFFBC5148)) }; TextButton(onClick = onDismiss) { Text("Cancel") } } })
}

@Composable private fun CategoryHistoryDialog(category: String, transactions: List<LedgerTransaction>, onDismiss: () -> Unit, onAdd: (String) -> Unit, onEdit: (LedgerTransaction) -> Unit) {
    val history = transactions.filter { it.type == "Expense" && it.category == category }.sortedByDescending { it.date }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Column { Text(category, fontWeight = FontWeight.Bold); Text("Expense history", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Normal) } },
        text = {
            if (history.isEmpty()) Column(Modifier.padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No expenses in this category yet", color = Muted)
            } else LazyColumn(Modifier.heightIn(max = 390.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(history) { transaction -> TransactionRow(transaction, Modifier.clickable { onEdit(transaction) }) }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(category) }) { Text("＋ Add expense", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable private fun FormField(label: String, value: String, onChange: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text) { OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(12.dp)) }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun DropdownField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) { var expanded by remember { mutableStateOf(false) }; ExposedDropdownMenuBox(expanded, { expanded = it }) { OutlinedTextField(value, {}, Modifier.fillMaxWidth().menuAnchor(), label = { Text(label) }, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, shape = RoundedCornerShape(12.dp)); ExposedDropdownMenu(expanded, { expanded = false }) { options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); expanded = false }) } } } }

private fun cash(amount: Double): String = "৳ " + NumberFormat.getNumberInstance(Locale("en", "BD")).format(amount.toLong())
private fun encodeTransactions(items: List<LedgerTransaction>): String = JSONArray().apply { items.forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("amount", it.amount); put("date", it.date); put("category", it.category); put("payment", it.payment); put("note", it.note); put("type", it.type); put("need", it.need) }) } }.toString()
private fun loadTransactions(raw: String?): List<LedgerTransaction> {
    if (raw != null) return try { val array = JSONArray(raw); (0 until array.length()).map { val o = array.getJSONObject(it); LedgerTransaction(o.optLong("id"), o.optString("name"), o.optDouble("amount"), o.optString("date"), o.optString("category"), o.optString("payment"), o.optString("note"), o.optString("type"), o.optString("need", "Needs review")) } } catch (_: Exception) { emptyList() }
    return emptyList()
}
