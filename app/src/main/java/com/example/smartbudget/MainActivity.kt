package com.example.smartbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartbudget.ui.theme.SmartBudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartBudgetTheme {
                Surface (
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartBudgetApp()
                }
            }
        }
    }
}

@Composable
fun SmartBudgetApp() {
    val navController = rememberNavController()
    val financeViewModel: FinanceViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            FinanceDashboardScreen(
                financeViewModel = financeViewModel,
                onNavigateToChat = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(
                onBack = { navController.popBackStack() },
                financeViewModel = financeViewModel
            )
        }
    }
}
