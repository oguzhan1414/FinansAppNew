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
import eu.tutorials.notapp.model.Budget
import eu.tutorials.notapp.model.Transaction
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

@Composable
fun butcelerimPage(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var budgets by remember { mutableStateOf<List<Budget>?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>?>(null) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var selectedTimeFrame by remember { mutableStateOf("Aylık") }
    val isLoading = budgets == null || transactions == null

    fun refreshData() {
        fetchBudgetsForPage { budgetList ->
            fetchTransactionsForBudgets { transactionList ->
                transactions = transactionList
                budgets = budgetList.map { budget ->
                    updateBudgetWithExpenses(budget, transactionList)
                }.sortedByDescending { it.timestamp }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onBudgetAdded = { newBudget ->
                addBudgetToFirebase(newBudget) {
                    refreshData()
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            LoadingState()
        } else {
            EnhancedBudgetScreenContent(
                budgets = budgets ?: emptyList(),
                transactions = transactions ?: emptyList(),
                selectedTimeFrame = selectedTimeFrame,
                onTimeFrameChange = { selectedTimeFrame = it },
                onAddBudgetClick = { showAddBudgetDialog = true },
                navController = navController
            )
        }
    }
}

@Composable
private fun EnhancedBudgetScreenContent(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    selectedTimeFrame: String,
    onTimeFrameChange: (String) -> Unit,
    onAddBudgetClick: () -> Unit,
    navController: NavController
) {
    val filteredTransactions = filterTransactionsByTimeFrame(transactions, selectedTimeFrame)
    val totalIncome = filteredTransactions.filter { it.type == "income" }.sumOf { abs(it.amount) }
    val totalExpenses = filteredTransactions.filter { it.type == "expense" }.sumOf { abs(it.amount) }
    val totalBudget = budgets.sumOf { it.allocated }
    val totalSpent = budgets.sumOf { it.spent }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            EnhancedHeader(
                navController = navController,
                onAddBudgetClick = onAddBudgetClick
            )
        }

        item {
            TimeFrameSelector(
                selectedTimeFrame = selectedTimeFrame,
                onTimeFrameSelected = onTimeFrameChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            AnalyticsOverviewCard(
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                modifier = Modifier.padding(16.dp)
            )
        }

        item {
            BudgetProgressCard(
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            Text(
                text = "Kategori Bazlı Bütçeler (${budgets.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp)
            )
        }

        if (budgets.isNotEmpty()) {
            items(budgets, key = { it.id }) { budget ->
                EnhancedBudgetItem(
                    budget = budget,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        } else {
            item {
                EmptyBudgetState(onAddBudgetClick)
            }
        }
    }
}

@Composable
private fun EnhancedHeader(navController: NavController, onAddBudgetClick: () -> Unit) {
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
            text = "Bütçe Analiz Merkezi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(
            onClick = onAddBudgetClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Yeni Bütçe")
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
private fun AnalyticsOverviewCard(
    totalBudget: Double,
    totalSpent: Double,
    totalIncome: Double,
    totalExpenses: Double,
    modifier: Modifier = Modifier
) {
    val budgetUsage = if (totalBudget > 0) (totalSpent / totalBudget * 100) else 0.0
    val netBalance = totalIncome - totalExpenses

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📊 Finansal Özet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bütçe Kullanımı
            BudgetUsageIndicator(
                usagePercentage = budgetUsage,
                spent = totalSpent,
                budget = totalBudget,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Hızlı İstatistikler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    title = "Toplam Gelir",
                    value = totalIncome,
                    color = Color(0xFF00C853),
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                QuickStatCard(
                    title = "Net Bakiye",
                    value = netBalance,
                    color = if (netBalance >= 0) Color(0xFF2196F3) else MaterialTheme.colorScheme.error,
                    icon = if (netBalance >= 0) Icons.Default.AccountBalance else Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BudgetUsageIndicator(
    usagePercentage: Double,
    spent: Double,
    budget: Double,
    modifier: Modifier = Modifier
) {
    val progress = (usagePercentage / 100).toFloat().coerceIn(0f, 1f)
    val statusColor = when {
        usagePercentage > 100 -> MaterialTheme.colorScheme.error
        usagePercentage > 80 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when {
        usagePercentage > 100 -> "BÜTÇE AŞIMI"
        usagePercentage > 80 -> "DİKKAT"
        else -> "SAĞLIKLI"
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bütçe Kullanımı",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = statusColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Harcanan: ${formatCurrency(spent)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "%.1f%%".format(usagePercentage),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            Text(
                text = "Bütçe: ${formatCurrency(budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
                text = formatCurrency(value),
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
private fun BudgetProgressCard(
    totalBudget: Double,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    val remaining = totalBudget - totalSpent
    val isOverBudget = totalSpent > totalBudget

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dairesel Progress
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.0f%%".format((totalSpent / totalBudget * 100).coerceAtMost(100.0)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("kullanıldı", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Detaylar
            Column(modifier = Modifier.weight(1f)) {
                BudgetDetailRow(
                    label = "Toplam Bütçe:",
                    value = formatCurrency(totalBudget),
                    color = MaterialTheme.colorScheme.onSurface
                )
                BudgetDetailRow(
                    label = "Harcanan:",
                    value = formatCurrency(totalSpent),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                BudgetDetailRow(
                    label = "Kalan:",
                    value = formatCurrency(remaining),
                    color = if (remaining >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BudgetDetailRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun EnhancedBudgetItem(budget: Budget, modifier: Modifier = Modifier) {
    val progress = (budget.spent / budget.allocated).toFloat().coerceIn(0f, 1f)
    val isOverBudget = budget.spent > budget.allocated
    val remaining = budget.allocated - budget.spent
    val usagePercentage = (budget.spent / budget.allocated * 100).toFloat()

    val statusColor = when {
        isOverBudget -> MaterialTheme.colorScheme.error
        usagePercentage > 80 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusIcon = when {
        isOverBudget -> Icons.Default.Warning
        usagePercentage > 80 -> Icons.Default.Info
        else -> Icons.Default.CheckCircle
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header - Kategori ve Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Durum",
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budget.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%.1f%% kullanıldı".format(usagePercentage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Kalan Bütçe
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(remaining),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                    )
                    Text(
                        text = if (isOverBudget) "AŞIM" else "Kalan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Alt Bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Harcanan: ${formatCurrency(budget.spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Bütçe: ${formatCurrency(budget.allocated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyBudgetState(onAddBudgetClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Analytics,
            "Boş Bütçe",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Henüz bütçeniz bulunmuyor",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Bütçe oluşturarak harcamalarınızı daha iyi kontrol edebilir ve finansal hedeflerinize ulaşabilirsiniz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onAddBudgetClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("İlk Bütçeni Oluştur")
        }
    }
}

// Yardımcı Fonksiyonlar
private fun filterTransactionsByTimeFrame(transactions: List<Transaction>, timeFrame: String): List<Transaction> {
    // Basit filtreleme - gerçek uygulamada tarih kontrolü yapılmalı
    return when (timeFrame) {
        "Haftalık" -> transactions.takeLast(7) // Son 7 işlem
        "Yıllık" -> transactions // Tüm işlemler
        else -> transactions.takeLast(30) // Varsayılan: Son 30 işlem
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    return format.format(amount)
}

// Firebase fonksiyonları aynı kalacak...
private fun fetchBudgetsForPage(onResult: (List<Budget>) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(emptyList())
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
        .addOnFailureListener { onResult(emptyList()) }
}

private fun fetchTransactionsForBudgets(onResult: (List<Transaction>) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(emptyList())
    Firebase.firestore.collection("users").document(userId)
        .collection("transactions")
        .get()
        .addOnSuccessListener { result ->
            val transactions = result.toObjects(Transaction::class.java).mapIndexed { index, transaction ->
                transaction.copy(id = result.documents[index].id)
            }
            onResult(transactions)
        }
        .addOnFailureListener { onResult(emptyList()) }
}

private fun updateBudgetWithExpenses(budget: Budget, transactions: List<Transaction>): Budget {
    val categoryExpenses = transactions
        .filter { it.type == "expense" && it.category == budget.category }
        .sumOf { abs(it.amount) }
    return budget.copy(spent = categoryExpenses)
}

private fun addBudgetToFirebase(budget: Budget, onSuccess: () -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val budgetWithTimestamp = budget.copy(timestamp = Timestamp.now())
    Firebase.firestore.collection("users").document(userId)
        .collection("budgets")
        .add(budgetWithTimestamp)
        .addOnSuccessListener { onSuccess() }
}