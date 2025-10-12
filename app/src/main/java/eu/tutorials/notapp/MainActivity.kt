package eu.tutorials.notapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import eu.tutorials.notapp.navigation.AppNavigation
import eu.tutorials.notapp.ui.theme.NotAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotAppTheme {
                AppNavigation()
            }
        }
    }
}

