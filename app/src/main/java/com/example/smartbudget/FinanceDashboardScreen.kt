package com.example.smartbudget

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceDashboardScreen(
    financeViewModel: FinanceViewModel,
    onNavigateToChat: () -> Unit,
    navController: NavController
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = onNavigateToChat,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Chat with AI"
                    )
                }
            }
        },
//        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
        ) {
            DashboardHeader()
            ExpenseInputSection(financeViewModel)
            SectionDivider()
            ExpenseHistorySection(financeViewModel)
            SectionDivider()
            DebtInputSection(financeViewModel)
            SectionDivider()
            DebtSummarySection(financeViewModel)
            SectionDivider()
            GoalsInputSection(financeViewModel)
            GoalsSummarySection(financeViewModel)
            SectionDivider()
            AIAnalysisSection(financeViewModel, scrollState, onNavigateToChat)
            SectionDivider()
            SignOutSection(navController)

        }
    }
}



@Composable
private fun DashboardHeader() {
    Text(
        text = stringResource(R.string.dashboard_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun ExpenseInputSection(financeViewModel: FinanceViewModel) {
    var descriptionText by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var categoryText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val suggestedCategory by financeViewModel.categorySuggestion.collectAsState()

    val isAddExpenseButtonEnabled by remember {
        derivedStateOf {
            descriptionText.isNotBlank() && amountText.isNotBlank()
        }
    }

    LaunchedEffect(suggestedCategory) {
        val category = suggestedCategory
        if (!category.isNullOrBlank() && categoryText.isBlank()) {
            categoryText = category
        }
    }

    SectionTitle("Expense Input")

    ExpenseInputField(
        value = descriptionText,
        onValueChange = { descriptionText = it },
        label = "Description"
    )

    ExpenseInputField(
        value = amountText,
        onValueChange = { amountText = it },
        label = "Amount",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    ExpenseInputField(
        value = categoryText,
        onValueChange = { categoryText = it },
        label = "Category (Suggested)"
    )

    LoadingButton(
        text = "Add Expense",
        isLoading = isLoading,
        onClick = {
            val amountDouble = amountText.toDoubleOrNull() ?: 0.0
            scope.launch {
                isLoading = true
                val category = categoryText.ifBlank {
                    financeViewModel.fetchCategoryForExpense(descriptionText)
                }
                financeViewModel.addExpense(descriptionText, amountDouble, category)
                descriptionText = ""
                amountText = ""
                categoryText = ""
                isLoading = false
            }
        },
        enabled = isAddExpenseButtonEnabled
    )
}

@Composable
private fun ExpenseInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun LoadingButton(
    text: String,
    loadingText: String = "Processing...",
    isLoading: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(loadingText)
        } else {
            Text(text)
        }
    }
}

@Composable
private fun ExpenseHistorySection(financeViewModel: FinanceViewModel) {
    val expensesList by financeViewModel.expenses.collectAsState()

    SectionTitle("Expense History")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TableHeader(
            "Description" to 1f,
            "Category" to 1f,
            "Amount (KES)" to 0.7f
        )

        if (expensesList.isEmpty()) {
            EmptyState("No expenses recorded yet.")
        } else {
            expensesList.forEach { expense ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = expense.description,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = expense.category ?: "",
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "KES ${String.format("%.2f", expense.amount)}",
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeader(vararg columns: Pair<String, Float>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        columns.forEach { (text, weight) ->
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(weight)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtInputSection(financeViewModel: FinanceViewModel) {
    var debtDescriptionText by rememberSaveable { mutableStateOf("") }
    var debtAmountText by rememberSaveable { mutableStateOf("") }
    var selectedDueDate by rememberSaveable { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val isAddDebtButtonEnabled by remember {
        derivedStateOf {
            debtDescriptionText.isNotBlank() && debtAmountText.isNotBlank()
        }
    }

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                            selectedDueDate = dateFormatter.format(date)
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    SectionTitle("Debt Input")

    ExpenseInputField(
        value = debtDescriptionText,
        onValueChange = { debtDescriptionText = it },
        label = "Debt Description"
    )

    ExpenseInputField(
        value = debtAmountText,
        onValueChange = { debtAmountText = it },
        label = "Total Debt Amount",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    OutlinedTextField(
        value = selectedDueDate,
        onValueChange = { },
        label = { Text("Due Date (MM/DD/YYYY)") },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePickerDialog = true }) {
                Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Select Date")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )

    Button(
        onClick = {
            val amountDouble = debtAmountText.toDoubleOrNull() ?: 0.0
            financeViewModel.addDebt(
                debtDescriptionText,
                amountDouble,
                selectedDueDate
            )
            debtDescriptionText = ""
            debtAmountText = ""
            selectedDueDate = ""
        },
        enabled = isAddDebtButtonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Add Debt")
    }
}

@Composable
private fun DebtSummarySection(financeViewModel: FinanceViewModel) {
    val debtsList by financeViewModel.debts.collectAsState()

    SectionTitle("Debt Summary")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TableHeader(
            "Description" to 1f,
            "Due Date" to 1f,
            "Amount (KES)" to 0.7f
        )

        if (debtsList.isEmpty()) {
            EmptyState("No debts recorded yet.")
        } else {
            debtsList.forEach { debt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    val formattedDate = debt.dueDate?.let { dateFormatter.format(it) } ?: "N/A"
                    Text(text = debt.description, modifier = Modifier.weight(1f))
                    Text(text = formattedDate, modifier = Modifier.weight(1f))
                    Text(text = "KES ${String.format("%.2f", debt.totalAmount)}", modifier = Modifier.weight(0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalsInputSection(financeViewModel: FinanceViewModel) {
    var goalDescriptionText by rememberSaveable { mutableStateOf("") }
    var goalAmountText by rememberSaveable { mutableStateOf("") }
    var selectedTargetDate by rememberSaveable { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val isAddGoalButtonEnabled by remember {
        derivedStateOf {
            goalDescriptionText.isNotBlank() && goalAmountText.isNotBlank()
        }
    }

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                            selectedTargetDate = dateFormatter.format(date)
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    SectionTitle("Financial Goals")

    ExpenseInputField(
        value = goalDescriptionText,
        onValueChange = { goalDescriptionText = it },
        label = "Goal Description"
    )

    ExpenseInputField(
        value = goalAmountText,
        onValueChange = { goalAmountText = it },
        label = "Target Amount",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    OutlinedTextField(
        value = selectedTargetDate,
        onValueChange = { },
        label = { Text("Target Date (MM/DD/YYYY, optional)") },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePickerDialog = true }) {
                Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Select Date")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )

    Button(
        onClick = {
            val amountDouble = goalAmountText.toDoubleOrNull() ?: 0.0
            financeViewModel.addGoal(
                goalDescriptionText,
                amountDouble,
                selectedTargetDate.takeIf { it.isNotBlank() }
            )
            goalDescriptionText = ""
            goalAmountText = ""
            selectedTargetDate = ""
        },
        enabled = isAddGoalButtonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Add Goal")
    }
}



@Composable
private fun GoalsSummarySection(financeViewModel: FinanceViewModel) {
    val goalsList by financeViewModel.goals.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TableHeader(
            "Goal" to 1f,
            "Target (KES)" to 0.7f,
            "Due Date" to 1f
        )

        if (goalsList.isEmpty()) {
            EmptyState("No goals set yet.")
        } else {
            goalsList.forEach { goal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    val formattedDate = goal.targetDate?.let { dateFormatter.format(it) } ?: "N/A"
                    Text(text = goal.description, modifier = Modifier.weight(1f))
                    Text(text = "KES ${String.format("%.2f", goal.targetAmount)}", modifier = Modifier.weight(0.7f))
                    Text(text = formattedDate, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AIAnalysisSection(
    financeViewModel: FinanceViewModel,
    scrollState: ScrollState,
    onNavigateToChat: () -> Unit
) {
    var aiAnalysisResult by rememberSaveable { mutableStateOf("") }
    var isLoadingAnalysis by remember { mutableStateOf(false) }
    var aiSectionOffset by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                aiSectionOffset = coordinates.positionInParent().y
            }
    ) {
        Column {
            SectionTitle("AI Analysis")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LoadingButton(
                    text = "Get AI Analysis",
                    isLoading = isLoadingAnalysis,
                    onClick = {
                        scope.launch {
                            isLoadingAnalysis = true
                            try {
                                aiAnalysisResult = financeViewModel.getFinancialAnalysis()
                            } catch (e: Exception) {
                                aiAnalysisResult = "Analysis failed: ${e.message ?: "Unknown error"}"
                            } finally {
                                isLoadingAnalysis = false
                            }
                        }
                    }
                )

                if (aiAnalysisResult.isNotEmpty() && !isLoadingAnalysis) {
                    Column {
                        Text(
                            text = aiAnalysisResult,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            lineHeight = 20.sp
                        )
                        Button(
                            onClick = onNavigateToChat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Chat for More")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(aiAnalysisResult) {
        if (aiAnalysisResult.isNotEmpty()) {
            scrollState.animateScrollTo(aiSectionOffset.toInt())
        }
    }
}

@Composable
private fun SignOutSection(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("83956256458-b5g043l0598t2t0fh5gmljchodkcpjli.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    Button(
        onClick = {
            auth.signOut() // Firebase sign-out
            googleSignInClient.signOut().addOnCompleteListener {
                navController.navigate("login") {
                    popUpTo("dashboard") { inclusive = true }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Sign Out")
    }
}



//@Preview(showSystemUi = true)
//@Composable
//fun FinanceDashboardScreenPreview() {
//    FinanceDashboardScreen(
//        onNavigateToChat = {},
//        financeViewModel = TODO()
//    )
//}