package eu.tutorials.notapp.model // veya .models, senin paket adın ne ise o

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

// 1. Yazım hatasını düzelterek data class'ın adını standart hale getirdik.
data class NavigationItem(
    val title: String,
    val icon: ImageVector
)

// 2. Listeyi tekrar sabit (val) hale getirdik ve metinleri doğrudan Türkçe yazdık.
val navigationItems = listOf(
    NavigationItem(
        title = "Ana Sayfa",
        icon = Icons.Default.Home
    ),
    NavigationItem(
        title = "İşlemler",
        icon = Icons.AutoMirrored.Filled.List
    ),
    NavigationItem(
        title = "Bütçeler",
        icon = Icons.Default.AccountBalanceWallet
    ),
    NavigationItem(
        title = "Raporlar",
        icon = Icons.Default.PieChart
    ),
    NavigationItem(
        title = "Profil",
        icon = Icons.Default.AccountCircle
    )
)