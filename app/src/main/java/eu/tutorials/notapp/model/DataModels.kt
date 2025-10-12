// eu.tutorials.notapp.model/DataModels.kt

package eu.tutorials.notapp.model

import com.google.firebase.Timestamp
import androidx.compose.ui.graphics.vector.ImageVector
// ... diğer importların

data class Transaction(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "",
    val timestamp: Timestamp? = null
)

// BURAYI GÜNCELLE
data class Budget(
    val id: String = "",
    val category: String = "",
    val allocated: Double = 0.0,
    val spent: Double = 0.0,
    val timestamp: Timestamp? = null // YENİ EKLENEN ALAN
)

data class TransactionCategory(
    val name: String,
    val type: String,
    val icon: String
)