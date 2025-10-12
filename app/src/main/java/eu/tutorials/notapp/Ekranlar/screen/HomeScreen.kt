package eu.tutorials.notapp.Ekranlar.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.notapp.Ekranlar.page.butcelerimPage
import eu.tutorials.notapp.Ekranlar.page.dashboardPage
import eu.tutorials.notapp.Ekranlar.page.islemlerPage
import eu.tutorials.notapp.Ekranlar.page.profilPage
import eu.tutorials.notapp.Ekranlar.page.raporPage
import eu.tutorials.notapp.model.navigationItems // 'model' yerine 'models' paketini kullandığını varsayıyorum

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController) {
    var selectedIndex by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            ModernNavigationBar(
                selectedIndex = selectedIndex,
                onItemSelected = { index -> selectedIndex = index }
            )
        }
    ) { paddingValues ->
        ContentScreen(
            modifier = modifier.padding(paddingValues),
            selectedIndex = selectedIndex,
            navController = navController
        )
    }
}

@Composable
private fun ModernNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    // BU SATIRI SİLİYORUZ -> val navigationItems = getNavigationItems()

    NavigationBar(
        // ... (geri kalan kodun aynı kalacak)
        windowInsets = NavigationBarDefaults.windowInsets,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        // forEachIndexed doğrudan import ettiğimiz global 'navigationItems' listesini kullanacak
        navigationItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ContentScreen(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    navController: NavController
) {
    when (selectedIndex) {
        0 -> dashboardPage(modifier, navController)
        1 -> islemlerPage(modifier, navController)
        2 -> butcelerimPage(modifier, navController)
        3 -> raporPage(modifier, navController)
        4 -> profilPage(modifier, navController)
    }
}