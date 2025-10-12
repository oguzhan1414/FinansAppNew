package eu.tutorials.notapp.Ekranlar.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import eu.tutorials.notapp.model.Budget
import eu.tutorials.notapp.model.Transaction
import eu.tutorials.notapp.model.TransactionCategory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// Kategori listeleri
val incomeCategories = listOf(
    TransactionCategory("Maaş", "income", "work"),
    TransactionCategory("Freelance", "income", "computer"),
    TransactionCategory("Yatırım", "income", "trending_up"),
    TransactionCategory("Burs", "income", "school"),
    TransactionCategory("Diğer Gelir", "income", "payments")
)
val expenseCategories = listOf(
    TransactionCategory("Market", "expense", "shopping_cart"),
    TransactionCategory("Fatura", "expense", "receipt"),
    TransactionCategory("Kira", "expense", "home"),
    TransactionCategory("Ulaşım", "expense", "directions_car"),
    TransactionCategory("Yemek", "expense", "restaurant"),
    TransactionCategory("Eğlence", "expense", "movie"),
    TransactionCategory("Sağlık", "expense", "local_hospital"),
    TransactionCategory("Giyim", "expense", "checkroom"),
    TransactionCategory("Diğer Gider", "expense", "payments")
)

@Composable
fun dashboardPage(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var firstName by remember { mutableStateOf("") }
    var totalBalance by remember { mutableStateOf(0.0) }
    var transactions by remember { mutableStateOf<List<Transaction>?>(null) }
    var budgets by remember { mutableStateOf<List<Budget>?>(null) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    // ANLIK GÜNCELLEME İÇİN VERİ YENİLEME FONKSİYONU
    fun refreshData() {
        fetchTransactions { transactionList ->
            transactions = transactionList
            calculateTotalBalance(transactionList) { balance -> totalBalance = balance }
        }
        fetchBudgets { budgetList -> budgets = budgetList }
    }

    LaunchedEffect(Unit) {
        fetchUserData { name, _ -> firstName = name }
        refreshData() // İlk açılışta verileri çek
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTransactionDialog = false },
            onTransactionAdded = { newTransaction ->
                addTransactionToFirebase(newTransaction) {
                    refreshData() // İşlem eklenince tüm verileri yenile
                }
            },
            customBudgets = budgets ?: emptyList()
        )
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onBudgetAdded = { newBudget ->
                addBudgetToFirebase(newBudget) {
                    refreshData() // Bütçe eklenince tüm verileri yenile
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        PremiumHeader(
            firstName = firstName,
            totalBalance = totalBalance,
            onAddTransactionClick = { showAddTransactionDialog = true }
        )

        val areTransactionsVisible = transactions != null
        val areBudgetsVisible = budgets != null

        AnimatedVisibility(visible = areTransactionsVisible, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }) {
            RecentTransactionsSection(transactions ?: emptyList())
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(visible = areBudgetsVisible, enter = fadeIn(tween(500, 200)) + slideInVertically(tween(500, 200)) { it / 2 }) {
            BudgetSummarySection(
                budgets = budgets ?: emptyList(),
                transactions = transactions ?: emptyList(),
                onAddBudgetClick = { showAddBudgetDialog = true },
                navController = navController
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(visible = areTransactionsVisible, enter = fadeIn(tween(500, 400)) + slideInVertically(tween(500, 400)) { it / 2 }) {
            SummarySection(
                transactions = transactions ?: emptyList(),
                onViewReports = { navController.navigate("reports") }
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- TASARIM BİLEŞENLERİ ---

@Composable
private fun PremiumHeader(
    firstName: String,
    totalBalance: Double,
    onAddTransactionClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Merhaba,", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = firstName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                ExtendedFloatingActionButton(
                    onClick = onAddTransactionClick,
                    icon = { Icon(Icons.Default.Add, "Ekle") },
                    text = { Text("Ekle") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                            start = Offset.Zero, end = Offset.Infinite
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Toplam Varlık", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = formatCurrency(totalBalance), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(transactions: List<Transaction>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Son Hareketler", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
        ) {
            if (transactions.isNotEmpty()) {
                Column {
                    transactions.take(3).forEachIndexed { index, transaction ->
                        VibrantTransactionItem(transaction)
                        if (index < transactions.take(3).lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(start = 72.dp, end = 16.dp))
                        }
                    }
                }
            } else {
                Text(text = "Henüz işlem kaydı bulunmuyor.", modifier = Modifier.padding(32.dp).fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun VibrantTransactionItem(transaction: Transaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(getCategoryColor(transaction.category).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = getCategoryIcon(transaction.category), contentDescription = transaction.category, tint = getCategoryColor(transaction.category), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = transaction.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = formatTimestampToDate(transaction.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = formatCurrency(transaction.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (transaction.amount < 0) MaterialTheme.colorScheme.error else Color(0xFF00C853))
    }
}

@Composable
fun BudgetSummarySection(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    onAddBudgetClick: () -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Bütçe Takibi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 8.dp))
            IconButton(onClick = onAddBudgetClick) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Bütçe Ekle", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (budgets.size > 3) {
                TextButton(onClick = { navController.navigate("budgets") }) { Text("Tümü") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
        ) {
            if (budgets.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    budgets.take(3).forEach { budget ->
                        val updatedBudget = updateBudgetWithExpenses(budget, transactions)
                        CompactBudgetItem(budget = updatedBudget)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Henüz bir bütçen yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onAddBudgetClick, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("İlk Bütçeni Oluştur")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBudgetItem(budget: Budget) {
    val progress = (if (budget.allocated > 0) budget.spent / budget.allocated else 0.0).toFloat()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = budget.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "${formatCurrency(budget.spent)} / ${formatCurrency(budget.allocated)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = if (progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun SummarySection(
    transactions: List<Transaction>,
    onViewReports: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Aylık Özet", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onViewReports) { Text("Detaylar") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "expense" }.sumOf { abs(it.amount) }

            ModernReportCard(title = "Toplam Gelir", amount = totalIncome, isPositive = true, modifier = Modifier.weight(1f))
            ModernReportCard(title = "Toplam Gider", amount = totalExpense, isPositive = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModernReportCard(
    title: String,
    amount: Double,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = if (isPositive) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                Icon(imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = formatCurrency(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}


// --- DIALOGLAR, YARDIMCI VE FIREBASE FONKSİYONLARI ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onTransactionAdded: (Transaction) -> Unit,
    customBudgets: List<Budget>
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }

    val filteredCategories = when (transactionType) {
        "income" -> incomeCategories
        else -> {
            val customBudgetCategories = customBudgets.map { budget -> TransactionCategory(budget.category, "expense", "account_balance_wallet") }
            (expenseCategories + customBudgetCategories).distinctBy { it.name }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni İşlem Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("İşlem Açıklaması") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.matches(Regex("^\\d*(\\.\\d*)?$"))) { amount = it } },
                    label = { Text("Tutar (TL)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("₺") }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = transactionType == "income",
                        onClick = { transactionType = "income"; selectedCategory = "" },
                        label = { Text("Gelir") },
                        leadingIcon = { Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = transactionType == "expense",
                        onClick = { transactionType = "expense"; selectedCategory = "" },
                        label = { Text("Gider") },
                        leadingIcon = { Icon(Icons.Default.TrendingDown, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = isCategoryMenuExpanded,
                    onExpandedChange = { isCategoryMenuExpanded = !isCategoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.ifEmpty { "Kategori Seçin" },
                        onValueChange = {},
                        label = { Text("Kategori") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded) }
                    )
                    ExposedDropdownMenu(expanded = isCategoryMenuExpanded, onDismissRequest = { isCategoryMenuExpanded = false }) {
                        filteredCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { selectedCategory = category.name; isCategoryMenuExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && amount.isNotEmpty() && selectedCategory.isNotEmpty()) {
                        val transactionAmount = if (transactionType == "expense") -abs(amount.toDouble()) else abs(amount.toDouble())
                        onTransactionAdded(Transaction(title = title, amount = transactionAmount, category = selectedCategory, type = transactionType))
                        onDismiss()
                    }
                },
                enabled = title.isNotEmpty() && amount.isNotEmpty() && selectedCategory.isNotEmpty()
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
fun AddBudgetDialog(onDismiss: () -> Unit, onBudgetAdded: (Budget) -> Unit) {
    var customCategory by remember { mutableStateOf("") }
    var allocatedAmount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Bütçe Oluştur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = customCategory, onValueChange = { customCategory = it }, label = { Text("Kategori Adı") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = allocatedAmount,
                    onValueChange = { if (it.matches(Regex("^\\d*(\\.\\d*)?$"))) { allocatedAmount = it } },
                    label = { Text("Bütçe Tutarı") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("₺") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customCategory.isNotEmpty() && allocatedAmount.isNotEmpty()) {
                        onBudgetAdded(Budget(id = UUID.randomUUID().toString(), category = customCategory.trim(), allocated = allocatedAmount.toDouble(), spent = 0.0))
                        onDismiss()
                    }
                },
                enabled = customCategory.isNotEmpty() && allocatedAmount.isNotEmpty()
            ) { Text("Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    val expenseCat = expenseCategories.find { it.name == category }
    if (expenseCat != null) {
        return when (expenseCat.icon) {
            "shopping_cart" -> Icons.Default.ShoppingCart
            "receipt" -> Icons.Default.Receipt
            "home" -> Icons.Default.Home
            "directions_car" -> Icons.Default.DirectionsCar
            "restaurant" -> Icons.Default.Restaurant
            "movie" -> Icons.Default.Movie
            "local_hospital" -> Icons.Default.LocalHospital
            "checkroom" -> Icons.Default.Checkroom
            else -> Icons.Default.Payments
        }
    }
    val incomeCat = incomeCategories.find { it.name == category }
    if (incomeCat != null) {
        return when (incomeCat.icon) {
            "work" -> Icons.Default.Work
            "computer" -> Icons.Default.Computer
            "trending_up" -> Icons.Default.TrendingUp
            "school" -> Icons.Default.School
            else -> Icons.Default.Payments
        }
    }
    return Icons.Default.AccountBalanceWallet
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "Market", "Maaş" -> Color(0xFF4CAF50)
        "Fatura" -> Color(0xFF2196F3)
        "Eğlence" -> Color(0xFF9C27B0)
        "Yemek" -> Color(0xFFFF9800)
        "Ulaşım" -> Color(0xFF795548)
        "Kira" -> Color(0xFF607D8B)
        "Sağlık" -> Color(0xFFF44336)
        "Giyim" -> Color(0xFFE91E63)
        "Freelance" -> Color(0xFF009688)
        "Yatırım" -> Color(0xFF4CAF50)
        "Burs" -> Color(0xFF3F51B5)
        else -> Color(0xFF757575)
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    return format.format(amount)
}

private fun fetchUserData(onResult: (String, String) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    Firebase.firestore.collection("users").document(userId).get().addOnSuccessListener { document ->
        val firstName = document.getString("firstName") ?: ""; onResult(firstName, document.getString("lastName") ?: "")
    }
}

private fun fetchTransactions(onResult: (List<Transaction>) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    Firebase.firestore.collection("users").document(userId)
        .collection("transactions")
        .orderBy("timestamp", Query.Direction.DESCENDING).limit(20).get()
        .addOnSuccessListener { result ->
            val transactions = result.documents.mapNotNull { doc ->
                val transaction = doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                if (transaction?.timestamp == null) {
                    doc.getString("date")?.let { oldDateString ->
                        try {
                            val date = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(oldDateString)
                            if (date != null) transaction?.copy(timestamp = Timestamp(date)) else transaction
                        } catch (e: Exception) { transaction }
                    } ?: transaction
                } else { transaction }
            }
            onResult(transactions)
        }
}

private fun formatTimestampToDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "Tarih yok"
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("tr"))
    return sdf.format(timestamp.toDate())
}

private fun fetchBudgets(onResult: (List<Budget>) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    Firebase.firestore.collection("users").document(userId)
        .collection("budgets")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { result ->
            val budgets = result.toObjects(Budget::class.java).mapIndexed { index, budget ->
                budget.copy(id = result.documents[index].id)
            }
            onResult(budgets)
        }
}

private fun addTransactionToFirebase(transaction: Transaction, onSuccess: () -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val transactionWithTimestamp = transaction.copy(timestamp = Timestamp.now())
    Firebase.firestore.collection("users").document(userId)
        .collection("transactions")
        .add(transactionWithTimestamp)
        .addOnSuccessListener { onSuccess() }
}

private fun addBudgetToFirebase(budget: Budget, onSuccess: () -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val budgetWithTimestamp = budget.copy(timestamp = Timestamp.now())
    Firebase.firestore.collection("users").document(userId)
        .collection("budgets")
        .add(budgetWithTimestamp)
        .addOnSuccessListener { onSuccess() }
}

private fun calculateTotalBalance(transactions: List<Transaction>, onResult: (Double) -> Unit) {
    onResult(transactions.sumOf { it.amount })
}

private fun updateBudgetWithExpenses(budget: Budget, transactions: List<Transaction>): Budget {
    val categoryExpenses = transactions.filter { it.type == "expense" && it.category == budget.category }.sumOf { abs(it.amount) }
    return budget.copy(spent = categoryExpenses)
}