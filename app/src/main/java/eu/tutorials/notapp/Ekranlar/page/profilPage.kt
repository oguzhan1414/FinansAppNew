package eu.tutorials.notapp.Ekranlar.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext

// Profil sayfası için data class
data class UserProfile(
    val userId: String = "",
    val name: String = "Kullanıcı",
    val email: String = "",
    val phone: String = "",
    val joinDate: String = "",
    val totalTransactions: Int = 0,
    val monthlyBudget: Double = 0.0,
    val profileImage: String? = null
)

@Composable
fun profilPage(
    modifier: Modifier = Modifier,
    navController: NavController? = null
) {
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Kullanıcı verilerini çek
    LaunchedEffect(Unit) {
        try {
            fetchUserProfile { profile ->
                userProfile = profile
                isLoading = false
            }
        } catch (e: Exception) {
            println("❌ Profil sayfası hatası: ${e.message}")
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            ProfileLoadingState()
        } else {
            ModernProfileContent(
                userProfile = userProfile,
                onEditProfile = { showEditDialog = true },
                onLogout = { showLogoutDialog = true },
                onAbout = { showAboutDialog = true },
                navController = navController
            )
        }
    }

    // Profil Düzenleme Dialogu
    if (showEditDialog) {
        ModernEditProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditDialog = false },
            onProfileUpdated = { updatedProfile ->
                userProfile = updatedProfile
                updateUserProfile(updatedProfile)
            }
        )
    }

    // Çıkış Yap Dialogu
    if (showLogoutDialog) {
        ModernLogoutConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                logoutUser(navController)
                showLogoutDialog = false
            }
        )
    }

    // Uygulama Hakkında Dialogu
    if (showAboutDialog) {
        ModernAboutAppDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun ModernProfileContent(
    userProfile: UserProfile,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    navController: NavController?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header - Profil Bilgileri
        ModernProfileHeader(userProfile = userProfile, onEditProfile = onEditProfile)

        // İstatistik Kartları
        ModernProfileStats(userProfile = userProfile)

        // Ayarlar Listesi
        ModernProfileSettings(
            onLogout = onLogout,
            onAbout = onAbout,
            navController = navController
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ModernProfileHeader(
    userProfile: UserProfile,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profil Fotoğrafı
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.name.take(2).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Kullanıcı Bilgileri
            Text(
                text = userProfile.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = userProfile.email,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (userProfile.phone.isNotEmpty()) {
                Text(
                    text = userProfile.phone,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Profil Düzenle Butonu
            FilledTonalButton(
                onClick = onEditProfile,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Profili Düzenle", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ModernProfileStats(userProfile: UserProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toplam İşlemler
        ModernStatCard(
            title = "Toplam İşlem",
            value = userProfile.totalTransactions.toString(),
            icon = Icons.Default.ReceiptLong,
            color = Color(0xFF00C853),
            modifier = Modifier.weight(1f)
        )

        // Aylık Bütçe
        ModernStatCard(
            title = "Aylık Bütçe",
            value = formatCurrency(userProfile.monthlyBudget),
            icon = Icons.Default.AccountBalanceWallet,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModernStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernProfileSettings(
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    navController: NavController?
) {
    val context = LocalContext.current

    val settingsItems = listOf(
        ModernSettingsItem(
            title = "Hesap Ayarları",
            description = "Profil bilgilerinizi yönetin",
            icon = Icons.Default.Person,
            color = MaterialTheme.colorScheme.primary,
            action = { /* Gelecekte implement edilecek */ }
        ),
        ModernSettingsItem(
            title = "Bildirimler",
            description = "Bildirim ayarlarınızı düzenleyin",
            icon = Icons.Default.Notifications,
            color = Color(0xFFFF9800),
            action = { /* Gelecekte implement edilecek */ }
        ),
        ModernSettingsItem(
            title = "Gizlilik & Güvenlik",
            description = "Gizlilik ayarlarınızı yönetin",
            icon = Icons.Default.Security,
            color = Color(0xFF2196F3),
            action = { /* Gelecekte implement edilecek */ }
        ),
        ModernSettingsItem(
            title = "Yardım & Destek",
            description = "Sıkça sorulan sorular ve destek",
            icon = Icons.Default.Help,
            color = Color(0xFF9C27B0),
            action = {
                // E-posta gönder
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("destek@butcetakip.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Bütçe Takip Uygulaması Destek")
                }
                context.startActivity(Intent.createChooser(intent, "E-posta Gönder"))
            }
        ),
        ModernSettingsItem(
            title = "Uygulama Hakkında",
            description = "Versiyon bilgisi ve detaylar",
            icon = Icons.Default.Info,
            color = Color(0xFF607D8B),
            action = onAbout
        ),
        ModernSettingsItem(
            title = "Çıkış Yap",
            description = "Hesabınızdan güvenli çıkış",
            icon = Icons.Default.Logout,
            color = MaterialTheme.colorScheme.error,
            action = onLogout
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {
            Text(
                text = "Hesap & Ayarlar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(20.dp)
            )

            settingsItems.forEachIndexed { index, item ->
                ModernSettingsItemRow(
                    item = item,
                    isLast = index == settingsItems.size - 1
                )
            }
        }
    }
}

@Composable
private fun ModernSettingsItemRow(
    item: ModernSettingsItem,
    isLast: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { item.action() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(item.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Git",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isLast) {
            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                thickness = 1.dp,
                modifier = Modifier.padding(start = 76.dp)
            )
        }
    }
}

@Composable
private fun ModernEditProfileDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onProfileUpdated: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var email by remember { mutableStateOf(userProfile.email) }
    var phone by remember { mutableStateOf(userProfile.phone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Profili Düzenle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ad Soyad") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedProfile = userProfile.copy(
                        name = name,
                        email = email,
                        phone = phone
                    )
                    onProfileUpdated(updatedProfile)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Değişiklikleri Kaydet")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun ModernLogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Uyarı",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Çıkış Yap",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Hesabınızdan çıkış yapmak istediğinizden emin misiniz? Bu işlem geri alınamaz.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Evet, Çıkış Yap")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun ModernAboutAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Uygulama Hakkında",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Uygulama logosu veya ikonu
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BÜ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Bütçe Takip Uygulaması",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sürüm 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Kişisel bütçenizi yönetmek ve finansal hedeflerinize ulaşmak için tasarlanmış kullanıcı dostu bir uygulama. Gelir ve giderlerinizi takip edin, bütçeler oluşturun ve finansal sağlığınızı iyileştirin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // İletişim butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("destek@butcetakip.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Bütçe Takip Uygulaması Destek")
                            }
                            context.startActivity(Intent.createChooser(intent, "E-posta Gönder"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "E-posta", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Destek")
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Web Sitesi", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Web")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tamam")
            }
        }
    )
}

@Composable
private fun ProfileLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Profil yükleniyor...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Data Classes
data class ModernSettingsItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val action: () -> Unit
)

// Çıkış Yap Fonksiyonu - GÜNCELLENDİ
private fun logoutUser(navController: NavController?) {
    try {
        // Firebase'den çıkış yap
        FirebaseAuth.getInstance().signOut()
        println("✅ Firebase'den çıkış yapıldı")

        // Login ekranına yönlendir
        navController?.let { nav ->
            // Tüm back stack'i temizle ve login sayfasına git
            nav.navigate("login") {
                popUpTo(nav.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            println("✅ Login sayfasına yönlendiriliyor...")
        } ?: run {
            println("❌ NavController null, yönlendirme yapılamıyor")
        }
    } catch (e: Exception) {
        println("❌ Çıkış yapma hatası: ${e.message}")
    }
}

// Firebase Fonksiyonları
private fun fetchUserProfile(onResult: (UserProfile) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val userId = user?.uid

    if (userId != null) {
        Firebase.firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val profile = UserProfile(
                    userId = userId,
                    name = document.get("name")?.toString() ?: user.displayName ?: "Kullanıcı",
                    email = user.email ?: "",
                    phone = document.get("phone")?.toString() ?: "",
                    joinDate = document.get("joinDate")?.toString() ?: "",
                    totalTransactions = (document.get("totalTransactions") as? Number)?.toInt() ?: 0,
                    monthlyBudget = (document.get("monthlyBudget") as? Double) ?: 0.0,
                    profileImage = document.get("profileImage")?.toString()
                )
                onResult(profile)
            }
            .addOnFailureListener {
                // Varsayılan profil
                val defaultProfile = UserProfile(
                    userId = userId,
                    name = user.displayName ?: "Kullanıcı",
                    email = user.email ?: "",
                    joinDate = "2024",
                    totalTransactions = 0,
                    monthlyBudget = 0.0
                )
                onResult(defaultProfile)
            }
    } else {
        onResult(UserProfile())
    }
}

private fun updateUserProfile(profile: UserProfile) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId != null) {
        Firebase.firestore.collection("users")
            .document(userId)
            .set(
                mapOf(
                    "name" to profile.name,
                    "email" to profile.email,
                    "phone" to profile.phone,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                println("✅ Profil güncellendi")
            }
            .addOnFailureListener {
                println("❌ Profil güncelleme hatası")
            }
    }
}

// Yardımcı Fonksiyon
private fun formatCurrency(amount: Double): String {
    val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("tr", "TR"))
    return format.format(amount)
}