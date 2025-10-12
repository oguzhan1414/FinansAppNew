package eu.tutorials.notapp.Ekranlar.page

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import eu.tutorials.notapp.model.Transaction
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import java.text.NumberFormat

private val PositiveGreen = Color(0xFF00C853)
private val WarningOrange = Color(0xFFFF9800)
private val InfoBlue = Color(0xFF2196F3)

@Composable
fun raporPage(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var transactions by remember { mutableStateOf<List<Transaction>?>(null) }
    var selectedTimeFrame by remember { mutableStateOf("Aylık") }
    var selectedChartType by remember { mutableStateOf("Gelir-Gider") }
    val isLoading = transactions == null

    LaunchedEffect(selectedTimeFrame) {
        fetchAllReportTransactions { transactionList ->
            transactions = transactionList
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ModernReportsHeader(navController = navController)

        if (isLoading) {
            LoadingState()
        } else {
            EnhancedReportsContent(
                transactions = transactions ?: emptyList(),
                selectedTimeFrame = selectedTimeFrame,
                selectedChartType = selectedChartType,
                onTimeFrameSelected = { selectedTimeFrame = it },
                onChartTypeSelected = { selectedChartType = it }
            )
        }
    }
}

@Composable
private fun ModernReportsHeader(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "📊 Finansal Analiz",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EnhancedReportsContent(
    transactions: List<Transaction>,
    selectedTimeFrame: String,
    selectedChartType: String,
    onTimeFrameSelected: (String) -> Unit,
    onChartTypeSelected: (String) -> Unit
) {
    val filteredTransactions = filterTransactionsByTimeFrame(transactions, selectedTimeFrame)
    val totalIncome = filteredTransactions.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpenses = filteredTransactions.filter { it.type == "expense" }.sumOf { abs(it.amount) }
    val netBalance = totalIncome - totalExpenses

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TimeFrameSelector(
                selectedTimeFrame = selectedTimeFrame,
                onTimeFrameSelected = onTimeFrameSelected
            )
        }

        item {
            FinancialOverviewCard(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netBalance = netBalance,
                transactionCount = filteredTransactions.size
            )
        }

        item {
            QuickStatsRow(
                transactions = filteredTransactions,
                timeFrame = selectedTimeFrame
            )
        }

        item {
            ChartTypeSelector(
                selectedChartType = selectedChartType,
                onChartTypeSelected = onChartTypeSelected
            )
        }

        item {
            if (filteredTransactions.isNotEmpty()) {
                when (selectedChartType) {
                    "Gelir-Gider" -> BarChartPlaceholder(
                        transactions = filteredTransactions,
                        timeFrame = selectedTimeFrame
                    )
                    "Kategoriler" -> PieChartPlaceholder(
                        transactions = filteredTransactions,
                        timeFrame = selectedTimeFrame
                    )
                }
            } else {
                EmptyChartState()
            }
        }

        item {
            CategoryBreakdown(
                transactions = filteredTransactions,
                timeFrame = selectedTimeFrame
            )
        }
    }
}

@Composable
private fun TimeFrameSelector(
    selectedTimeFrame: String,
    onTimeFrameSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFrames = listOf("Haftalık", "Aylık", "Yıllık")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            timeFrames.forEach { timeFrame ->
                FilterChip(
                    selected = selectedTimeFrame == timeFrame,
                    onClick = { onTimeFrameSelected(timeFrame) },
                    label = { Text(timeFrame) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FinancialOverviewCard(
    totalIncome: Double,
    totalExpenses: Double,
    netBalance: Double,
    transactionCount: Int
) {
    val balanceStatus = when {
        netBalance > 0 -> "Sağlıklı"
        netBalance == 0.0 -> "Dengeli"
        else -> "Dikkat"
    }

    val balanceColor = when {
        netBalance > 0 -> PositiveGreen
        netBalance == 0.0 -> InfoBlue
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Başlık ve Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Finansal Özet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(balanceColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = balanceStatus,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Net Bakiye
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Net Bakiye",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(netBalance),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gelir-Gider Satırları
            IncomeExpenseRow(
                income = totalIncome,
                expense = totalExpenses,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // İşlem Sayısı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = "Toplam işlem",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$transactionCount işlem",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IncomeExpenseRow(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // Gelir
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(PositiveGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gelir",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(income),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = PositiveGreen
            )
        }

        // Ayırıcı
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )

        // Gider
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gider",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(expense),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun QuickStatsRow(
    transactions: List<Transaction>,
    timeFrame: String,
    modifier: Modifier = Modifier
) {
    val avgIncome = transactions.filter { it.type == "income" }.let {
        if (it.isNotEmpty()) it.sumOf { it.amount } / it.size else 0.0
    }
    val avgExpense = transactions.filter { it.type == "expense" }.let {
        if (it.isNotEmpty()) it.sumOf { abs(it.amount) } / it.size else 0.0
    }
    val largestExpense = transactions.filter { it.type == "expense" }.maxByOrNull { abs(it.amount) }?.amount ?: 0.0

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            title = "Ort. Gelir",
            value = avgIncome,
            color = PositiveGreen,
            icon = Icons.Default.TrendingUp,
            modifier = Modifier.weight(1f)
        )

        QuickStatCard(
            title = "Ort. Gider",
            value = avgExpense,
            color = MaterialTheme.colorScheme.error,
            icon = Icons.Default.TrendingDown,
            modifier = Modifier.weight(1f)
        )

        QuickStatCard(
            title = "En Büyük Gider",
            value = largestExpense,
            color = WarningOrange,
            icon = Icons.Default.Warning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatCurrencyShort(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChartTypeSelector(
    selectedChartType: String,
    onChartTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Gelir-Gider Butonu
            ChartTypeButton(
                type = "Gelir-Gider",
                icon = Icons.Default.BarChart,
                selected = selectedChartType == "Gelir-Gider",
                onClick = { onChartTypeSelected("Gelir-Gider") },
                modifier = Modifier.weight(1f)
            )

            // Kategoriler Butonu
            ChartTypeButton(
                type = "Kategoriler",
                icon = Icons.Default.PieChart,
                selected = selectedChartType == "Kategoriler",
                onClick = { onChartTypeSelected("Kategoriler") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChartTypeButton(
    type: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    ElevatedAssistChip(
        onClick = onClick,
        label = {
            Text(
                text = type,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = type,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            leadingIconContentColor = contentColor,
            trailingIconContentColor = contentColor,
            containerColor = containerColor,
            labelColor = contentColor
        ),
        modifier = modifier
    )
}

@Composable
private fun BarChartPlaceholder(
    transactions: List<Transaction>,
    timeFrame: String
) {
    ChartContainer(title = "Gelir - Gider Trendi") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(InfoBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = "Çubuk Grafik",
                    tint = InfoBlue,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gelir-Gider Analizi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfoBlue,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${transactions.size} işlem - $timeFrame",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PieChartPlaceholder(
    transactions: List<Transaction>,
    timeFrame: String
) {
    ChartContainer(title = "Gider Dağılımı") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(WarningOrange.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = "Pasta Grafik",
                    tint = WarningOrange,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kategori Analizi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarningOrange,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${transactions.size} işlem - $timeFrame",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartContainer(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun EmptyChartState() {
    ChartContainer(title = "Veri Analizi") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = "Veri Yok",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bu dönem için veri bulunamadı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(
    transactions: List<Transaction>,
    timeFrame: String
) {
    val categoryExpenses = transactions
        .filter { it.type == "expense" }
        .groupBy { it.category }
        .mapValues { (_, trans) -> trans.sumOf { abs(it.amount) } }
        .toList()
        .sortedByDescending { it.second }
        .take(5) // Sadece ilk 5 kategori

    if (categoryExpenses.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📋 Kategori Bazlı Giderler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                categoryExpenses.forEach { (category, amount) ->
                    CategoryExpenseRow(
                        category = category,
                        amount = amount,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseRow(
    category: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(category).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = category,
                    tint = getCategoryColor(category),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// Yardımcı Fonksiyonlar
private fun formatCurrencyShort(amount: Double): String {
    return when {
        amount >= 1000000 -> "%.1fM".format(amount / 1000000)
        amount >= 1000 -> "%.1fK".format(amount / 1000)
        else -> "%.0f".format(amount)
    }
}

private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Market" -> Icons.Default.ShoppingCart
        "Fatura" -> Icons.Default.Receipt
        "Ulaşım" -> Icons.Default.DirectionsCar
        "Yemek" -> Icons.Default.Restaurant
        "Eğlence" -> Icons.Default.Movie
        "Sağlık" -> Icons.Default.LocalHospital
        "Giyim" -> Icons.Default.Checkroom
        else -> Icons.Default.Category
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "Market" -> Color(0xFF4CAF50)
        "Fatura" -> Color(0xFF2196F3)
        "Ulaşım" -> Color(0xFF795548)
        "Yemek" -> Color(0xFFFF9800)
        "Eğlence" -> Color(0xFF9C27B0)
        "Sağlık" -> Color(0xFFF44336)
        "Giyim" -> Color(0xFFE91E63)
        else -> Color(0xFF607D8B)
    }
}

// Firebase fonksiyonları
private fun filterTransactionsByTimeFrame(transactions: List<Transaction>, timeFrame: String): List<Transaction> {
    val now = Calendar.getInstance()
    return transactions.filter { transaction ->
        transaction.timestamp?.toDate()?.let { transactionDate ->
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            when (timeFrame) {
                "Haftalık" -> {
                    val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }
                    transactionCalendar.after(oneWeekAgo)
                }
                "Aylık" -> transactionCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        transactionCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                "Yıllık" -> transactionCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                else -> true
            }
        } ?: false
    }
}

private fun fetchAllReportTransactions(onResult: (List<Transaction>) -> Unit) {
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
        .addOnFailureListener { onResult(emptyList()) }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    return format.format(amount)
}