package eu.tutorials.notapp.Ekranlar.screen

import android.annotation.SuppressLint
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import eu.tutorials.notapp.AppUtil
import eu.tutorials.notapp.modifier.AuthViewModel

// YENİ: Kod tekrarını önlemek ve okunabilirliği artırmak için sabitler
private val gradientBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
)
private val primaryButtonColor = Color(0xFF667eea)
private val secondaryTextColor = Color(0xFF718096)

@SuppressLint("UnrememberedMutableState")
@Composable
fun SignUpScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // --- State Değişkenleri ---
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    // YENİ: Hata durumları için state'ler
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }


    val context = LocalContext.current

    // YENİ: Formun geçerli olup olmadığını kontrol eden birleşik state
    val isFormValid by derivedStateOf {
        firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.length >= 6 &&
                password == confirmPassword &&
                acceptTerms
    }

    // YENİ: Ekran küçük olduğunda kaydırma özelliği eklemek için
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = true,
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    // YENİ: Kaydırma özelliği eklendi
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp), // Aralıklar daha tutarlı
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Create Account 🚀",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Join us and start your journey",
                        fontSize = 14.sp,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp)) // Başlık ve form arası boşluk

                // Form alanları
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = {
                                firstName = it
                                firstNameError = if (it.isBlank()) "First name cannot be empty" else null
                            },
                            label = { Text("First name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "First Name Icon") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            isError = firstNameError != null, // YENİ
                            supportingText = { if (firstNameError != null) Text(firstNameError!!) } // YENİ
                        )

                        OutlinedTextField(
                            value = lastName,
                            onValueChange = {
                                lastName = it
                                lastNameError = if (it.isBlank()) "Last name cannot be empty" else null
                            },
                            label = { Text("Last name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Last Name Icon") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            isError = lastNameError != null, // YENİ
                            supportingText = { if (lastNameError != null) Text(lastNameError!!) } // YENİ
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = if (!Patterns.EMAIL_ADDRESS.matcher(it).matches()) "Invalid email format" else null
                        },
                        label = { Text("Email address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // YENİ
                        isError = emailError != null, // YENİ
                        supportingText = { if (emailError != null) Text(emailError!!) } // YENİ
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = if (it.length < 6) "Password must be at least 6 characters" else null
                            // YENİ: Şifre değiştirildiğinde doğrulama şifresini de kontrol et
                            if (confirmPassword.isNotEmpty() && it != confirmPassword) {
                                confirmPasswordError = "Passwords do not match"
                            } else if (confirmPassword.isNotEmpty()) {
                                confirmPasswordError = null
                            }
                        },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show")
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // YENİ
                        isError = passwordError != null, // YENİ
                        supportingText = { if (passwordError != null) Text(passwordError!!) } // YENİ
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = if (it != password) "Passwords do not match" else null
                        },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password Icon") },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Text(if (confirmPasswordVisible) "Hide" else "Show")
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // YENİ
                        isError = confirmPasswordError != null, // YENİ
                        supportingText = { if (confirmPasswordError != null) Text(confirmPasswordError!!) } // YENİ
                    )
                }

                // Terms - DÜZELTME: Standart Checkbox kullanıldı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { acceptTerms = !acceptTerms }
                ) {
                    Checkbox(
                        checked = acceptTerms,
                        onCheckedChange = { acceptTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = primaryButtonColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I agree to the Terms & Conditions", color = secondaryTextColor)
                }

                // Sign Up butonu
                Button(
                    onClick = {
                        isLoading = true
                        // DÜZELTME: ViewModel'deki parametre adıyla eşleştiğinden emin ol (confirimPassword -> confirmPassword)
                        authViewModel.signup(
                            email = email,
                            firstName = firstName,
                            lastName = lastName,
                            password = password,
                            confirimPassword = confirmPassword)
                        { isSuccess, errorMessage ->
                            isLoading = false
                            if (isSuccess) {
                                // DÜZELTME: Geri tuşuyla login/signup ekranına dönmemek için
                                navController.navigate("home_page") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            } else {
                                AppUtil.showToast(context, errorMessage ?: "An unexpected error occurred")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryButtonColor,
                        contentColor = Color.White
                    ),
                    // DÜZELTME: Butonun aktif olma durumu artık formun geçerliliğine bağlı
                    enabled = isFormValid && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Giriş yap linki
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Already have an account? ", color = secondaryTextColor)
                    Text(
                        text = "Sign In",
                        color = primaryButtonColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            navController.navigate("login_screen")
                        }
                    )
                }
            }
        }
    }
}