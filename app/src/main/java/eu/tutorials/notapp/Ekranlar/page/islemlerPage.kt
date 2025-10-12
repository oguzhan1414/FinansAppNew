package eu.tutorials.notapp.Ekranlar.page

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import eu.tutorials.notapp.model.Transaction
import eu.tutorials.notapp.model.TransactionCategory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun islemlerPage(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var transactions by remember { mutableStateOf<List<Transaction>?>(null) }
    var filterState by remember { mutableStateOf("Tümü") }
    val isLoading = transactions == null

    val filteredTransactions = remember(transactions, filterState) {
        when (filterState) {
            "Gelir" -> transactions?.filter { it.type == "income" }
            "Gider" -> transactions?.filter { it.type == "expense" }
            else -> transactions
        }
    }

    LaunchedEffect(Unit) {
        fetchTransactionsForPage { transactionList ->
            transactions = transactionList.sortedByDescending { it.timestamp }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            LoadingState()
        } else {
            TimelineScreen(
                navController = navController,
                transactions = filteredTransactions ?: emptyList(),
                currentFilter = filterState,
                onFilterChange = { filterState = it }
            )
        }
    }
}

// --- TASARIM BİLEŞENLERİ ---

@OptIn(ExperimentalFoundationApi::class) // ANİMASYON İÇİN GEREKLİ
@Composable
private fun TimelineScreen(
    navController: NavController,
    transactions: List<Transaction>,
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    val groupedTransactions = groupTransactionsByDate(transactions)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "header") {
            Column(modifier = Modifier.animateItemPlacement()) { // ANİMASYON EKLENDİ
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "İşlem Geçmişi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                FilterChips(
                    selectedFilter = currentFilter,
                    onFilterSelected = onFilterChange,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
            item(key = "emptyState") {
                EmptyTransactionsState(filter = currentFilter, modifier = Modifier.animateItemPlacement()) // ANİMASYON EKLENDİ
            }
        } else {
            groupedTransactions.forEach { (date, dailyTransactions) ->
                item(key = date) {
                    TimelineNode(isDateNode = true, modifier = Modifier.animateItemPlacement()) { // ANİMASYON EKLENDİ
                        DateHeader(date)
                    }
                }
                items(dailyTransactions, key = { it.id }) { transaction ->
                    TimelineNode(modifier = Modifier.animateItemPlacement()) { // ANİMASYON EKLENDİ
                        TimelineTransactionCard(transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Tümü", "Gelir", "Gider").forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                leadingIcon = {
                    val icon = when (filter) {
                        "Gelir" -> Icons.Default.TrendingUp
                        "Gider" -> Icons.Default.TrendingDown
                        else -> Icons.Default.Checklist
                    }
                    Icon(icon, null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}

@Composable
private fun TimelineNode(
    modifier: Modifier = Modifier, // DÜZELTİLDİ: modifier parametresi eklendi
    isDateNode: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.height(if (isDateNode) 16.dp else 0.dp).width(2.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
            Box(modifier = Modifier.size(if (isDateNode) 16.dp else 8.dp).clip(CircleShape).background(if (isDateNode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
            Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            content()
            Spacer(modifier = Modifier.height(if (isDateNode) 8.dp else 16.dp))
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    Text(text = date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp))
}

@Composable
private fun TimelineTransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.clip(RoundedCornerShape(16.dp))) {
            // DÜZELTİLDİ: Renkli kategori vurgusu
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(getCategoryColor(transaction.category))
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                    Row {
                        Text(text = transaction.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = " • ${formatTimestampToTime(transaction.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = formatCurrencyForTransactions(transaction.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.amount < 0) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                )
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyTransactionsState(filter: String, modifier: Modifier = Modifier) { // DÜZELTİLDİ: modifier parametresi eklendi
    val message = when(filter) {
        "Gelir" -> "Kayıtlı gelir bulunmuyor."
        "Gider" -> "Kayıtlı gider bulunmuyor."
        else -> "Henüz işlem bulunmuyor."
    }
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- YARDIMCI VE FIREBASE FONKSİYONLARI ---

private val incomeCategoriesForHelpers = listOf(
    TransactionCategory("Maaş", "income", "work"),
    TransactionCategory("Freelance", "income", "computer"),
    TransactionCategory("Yatırım", "income", "trending_up"),
    TransactionCategory("Burs", "income", "school"),
    TransactionCategory("Diğer Gelir", "income", "payments")
)
private val expenseCategoriesForHelpers = listOf(
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

private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    val expenseCat = expenseCategoriesForHelpers.find { it.name == category }
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
    val incomeCat = incomeCategoriesForHelpers.find { it.name == category }
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

private fun groupTransactionsByDate(transactions: List<Transaction>): Map<String, List<Transaction>> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return transactions.groupBy { transaction ->
        val transactionCalendar = Calendar.getInstance()
        transaction.timestamp?.toDate()?.let { transactionCalendar.time = it }
        when {
            isSameDay(transactionCalendar, today) -> "Bugün"
            isSameDay(transactionCalendar, yesterday) -> "Dün"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR")).format(transactionCalendar.time)
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatTimestampToTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    return try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate())
    } catch (e: Exception) { "" }
}

private fun formatCurrencyForTransactions(amount: Double): String {
    return java.text.NumberFormat.getCurrencyInstance(Locale("tr", "TR")).format(amount)
}

private fun fetchTransactionsForPage(onResult: (List<Transaction>) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(emptyList())
    Firebase.firestore.collection("users").document(userId)
        .collection("transactions")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
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
        .addOnFailureListener { exception ->
            println("❌ Firebase hatası: ${exception.message}")
            onResult(emptyList())
        }
}