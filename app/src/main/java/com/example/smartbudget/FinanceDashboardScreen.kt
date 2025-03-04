package com.example.smartbudget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceDashboardScreen(
    financeViewModel: FinanceViewModel = viewModel()
) {
    val uiState by financeViewModel.uiState.collectAsState()
    var descriptionText by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var categoryText by rememberSaveable { mutableStateOf("") }
    val expensesList by financeViewModel.expenses.collectAsState()
    val isAddExpenseButtonEnabled by remember {
        derivedStateOf {
            descriptionText.isNotBlank() && amountText.isNotBlank()
        }
    }
    val suggestedCategory by financeViewModel.categorySuggestion.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }
    @OptIn(ExperimentalMaterial3Api::class)
    val datePickerState = rememberDatePickerState() // Add datePickerState here, with @OptIn annotation
    var selectedDueDate by rememberSaveable { mutableStateOf("") }
    var debtDescriptionText by rememberSaveable { mutableStateOf("") } // State for Debt Description TextField
    val debtsList by financeViewModel.debts.collectAsState()
    var debtAmountText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) } // Add loading state for expense addition
    val scope = rememberCoroutineScope()
    val categorySpending by financeViewModel.categorySpending.collectAsState()

    LaunchedEffect(suggestedCategory) {
        val category = suggestedCategory // Capture the value locally
        if (!category.isNullOrBlank() && categoryText.isBlank()) {
            categoryText = category
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false }, // Dismiss on outside click/back button
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false // Dismiss dialog
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                            selectedDueDate = dateFormatter.format(date) // Format and update selectedDueDate
                        }
                    },
                    enabled = true // Button always enabled in this simple example
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

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Expense Input",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, top = 24.dp, bottom = 8.dp)
        )
        TextField(
            value = descriptionText,
            onValueChange = { descriptionText = it },
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        TextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        TextField(
            value = categoryText,
            onValueChange = { categoryText = it },
            label = { Text("Category (Suggested)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Button(
            onClick = {
                val amountDouble = amountText.toDoubleOrNull() ?: 0.0
                scope.launch {
                    isLoading = true // Show loading state
                    val category = if (categoryText.isBlank()) {
                        // Fetch category from Gemini API
                        financeViewModel.fetchCategoryForExpense(descriptionText)
                    } else {
                        categoryText // Use the manually entered category
                    }
                    // Add expense after category is received
                    financeViewModel.addExpense(descriptionText, amountDouble, category)
                    // Reset fields
                    descriptionText = ""
                    amountText = ""
                    categoryText = ""
                    isLoading = false // Hide loading state
                }
            },
            enabled = isAddExpenseButtonEnabled && !isLoading, // Disable button while loading
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.requiredSize(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Add Expense")
            }
        }

        Text(
            text = "Expense History",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {

            Row(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Description",
                        fontWeight = FontWeight.W400,
                        modifier = Modifier.weight(1f)
                    )
                    // to fix later, loading delays
                    Text(
                        text = "Category",
                        fontWeight = FontWeight.W400,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Amount (KES)",
                        fontWeight = FontWeight.W400,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

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
                         modifier = Modifier.padding(start = 8.dp)
                     )
                 }
            }
        }


        Text(
            text = "Debt Input",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp))
        )

        TextField(
            value = debtDescriptionText,
            onValueChange = { debtDescriptionText = it },
            label = { Text("Debt Description") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        TextField(
            value = debtAmountText,
            onValueChange = { debtAmountText = it },
            label = { Text("Total Debt Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Number keyboard
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        val isAddDebtButtonEnabled by remember {
            derivedStateOf {
                debtDescriptionText.isNotBlank() && debtAmountText.isNotBlank()
            }
        }
        OutlinedTextField( // Use OutlinedTextField instead of TextField
            value = selectedDueDate, // Display selected date
            onValueChange = { /* Do nothing, read-only */ },
            label = { Text("Due Date (MM/DD/YYYY)") },
            readOnly = true, // Still read-only
            trailingIcon = { // Add a trailing icon (calendar icon)
                IconButton(onClick = { showDatePickerDialog = true }) { // Open DatePickerDialog on icon click
                    Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Select Date") // Calendar icon
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(bottom = 8.dp)
        ) {
            Text("Add Debt")
        }

        Text(
            text = "Debt Summary",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp)
        )

         Column(
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(horizontal = 16.dp)
         ) {

             Row(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(vertical = 8.dp)
             ) {
                 Text(
                     text = "Description",
                     fontWeight = FontWeight.W400,
                     modifier = Modifier.weight(1f)
                 )
                 Text(
                     text = "Due Date",
                     fontWeight = FontWeight.W400,
                     modifier = Modifier.weight(1f)
                 )
                 Text(
                     text = "Amount (KES)",
                     fontWeight = FontWeight.W400,
                     modifier = Modifier.padding(start = 8.dp)
                 )
             }
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
                     Text(text = "KES ${String.format("%.2f", debt.totalAmount)}", modifier = Modifier.padding(start = 8.dp))
                 }
             }
         }



        Text(
            text = "Insights",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            text = "Reports",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            categorySpending.forEach { (category, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "KES ${String.format("%.2f", amount)}",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }


        Button(
            onClick = { financeViewModel.loadTestData() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Load Test Data")
        }

        var aiAnalysisResult by rememberSaveable { mutableStateOf("") }
        var isLoadingAnalysis by remember { mutableStateOf(false) }
        var aiSectionOffset by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    aiSectionOffset = coordinates.positionInParent().y
                }
        ) {
            Column {
                Text(
                    text = "AI Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoadingAnalysis = true
                                aiAnalysisResult = financeViewModel.getFinancialAnalysis()
                                isLoadingAnalysis = false
                            }
                        },
                        enabled = !isLoadingAnalysis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        if (isLoadingAnalysis) {
                            CircularProgressIndicator(
                                modifier = Modifier.requiredSize(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Get AI Analysis")
                        }
                    }
                    if (aiAnalysisResult.isNotEmpty() && !isLoadingAnalysis) {
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
}


@Preview(showSystemUi = true)
@Composable
fun FinanceDashboardScreenPreview() {
    FinanceDashboardScreen()
}