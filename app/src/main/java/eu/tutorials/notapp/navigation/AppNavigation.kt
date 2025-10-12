package eu.tutorials.notapp.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.tutorials.notapp.Ekranlar.page.islemlerPage
import eu.tutorials.notapp.Ekranlar.screen.HomeScreen

import eu.tutorials.notapp.Ekranlar.screen.LoginScreen
import eu.tutorials.notapp.Ekranlar.screen.SignUpScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login_ekrani")
    {
        composable(route = "login_ekrani")
        {
            LoginScreen(navController=navController)
        }
        composable(route = "signup_ekrani")
        {
            SignUpScreen(navController=navController)
        }
        composable(route = "home_page"){
            HomeScreen(navController=navController)
        }
        composable(route="transactions")
        {
            islemlerPage(navController=navController)
        }
    }
}