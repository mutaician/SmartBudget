package com.example.smartbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartbudget.ui.theme.SmartBudgetTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            SmartBudgetTheme {
                Surface(
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
    val auth = FirebaseAuth.getInstance()
    val startDestination = remember { mutableStateOf(if (auth.currentUser != null) "dashboard" else "login") }

    NavHost(
        navController = navController,
        startDestination = startDestination.value
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = { userId ->
                    financeViewModel.setUserEmail(userId)
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            FinanceDashboardScreen(
                financeViewModel = financeViewModel,
                onNavigateToChat = { navController.navigate("chat") },
                navController = navController
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