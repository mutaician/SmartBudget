package com.example.smartbudget

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Expense(
    val description: String,
    val amount: Double,
    val category: String? = null,
    var date: Date = Date()
)

data class Debt(
    val description: String,
    val totalAmount: Double,
    val dueDate: Date? = null,
)


class FinanceViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey
    )

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _categorySuggestion = MutableStateFlow<String?>(null)
    val categorySuggestion: StateFlow<String?> = _categorySuggestion.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList()) // Mutable StateFlow for debts
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow() // Public StateFlow for debts

    private val _categorySpending = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categorySpending: StateFlow<Map<String, Double>> = _categorySpending.asStateFlow()

    fun addExpense(description: String, amount: Double, category: String?) {
        val newExpense = Expense(description = description, amount = amount, category = category)
        _expenses.value += newExpense
        _categorySuggestion.value = null
        updateCategorySpending()
    }

    fun addDebt(description: String, totalAmount: Double, dueDateString: String) {
        val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US) // Date formatter
        var dueDate: Date? = null
        if (dueDateString.isNotBlank()) {
            try {
                dueDate = dateFormatter.parse(dueDateString) // Parse due date string to Date
            } catch (e: ParseException) {
                Log.e("FinanceViewModel", "Error parsing due date: $dueDateString", e)
            }
        }

        val newDebt = Debt(description = description, totalAmount = totalAmount, dueDate = dueDate)
        _debts.value = _debts.value + newDebt // Add new debt to the list
        Log.d("FinanceViewModel", "Debt added: $newDebt") // Log the new debt
        Log.d("FinanceViewModel", "Updated debts  list: ${_debts.value}")
    }

    private fun updateCategorySpending() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) // 0-11 (Jan-Dec)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val monthlyExpenses = _expenses.value.filter { expense ->
            val expenseCal = Calendar.getInstance().apply { time = expense.date }
            expenseCal.get(Calendar.MONTH) == currentMonth && expenseCal.get(Calendar.YEAR) == currentYear
        }
        val spendingByCategory = monthlyExpenses
            .groupBy { it.category ?: "Uncategorized" }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        _categorySpending.value = spendingByCategory
        Log.d("FinanceViewModel", "Category spending: ${_categorySpending.value}")
    }

    suspend fun fetchCategoryForExpense(expenseDescription: String): String {
        return try {
            val prompt = "Categorize this expense: $expenseDescription into one of these categories: Food, Transportation, Utilities, Entertainment, Shopping, Bills, Other. Just give me the category name."
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )
            val suggestedCategory = response.text?.trim() ?: "Other"
            _categorySuggestion.value = suggestedCategory
            suggestedCategory
        } catch (e: Exception) {
            Log.e("Gemini Category Suggestion", "Gemini API error: ${e.localizedMessage ?: "Unknown error"}, using fallback 'Other'")
            _categorySuggestion.value = "Other"
            "Other" // Fallback category
        }
    }

    suspend fun getFinancialAnalysis(): String {
        val expenses = _expenses.value
        val debts = _debts.value
        val totalExpenses = expenses.sumOf { it.amount }
        val totalDebt = debts.sumOf { it.totalAmount }
        val spendingByCategory = expenses.groupBy { it.category ?: "Uncategorized" }
            .mapValues { it.value.sumOf { it.amount } }
        val debtDetails = debts.joinToString("\n") {
            "${it.description}: ${it.totalAmount} due ${it.dueDate?.let { SimpleDateFormat("MM/dd/yyyy", Locale.US).format(it) } ?: "N/A"}"
        }

        val prompt = """
        Analyze the user’s financial data:
        - Total monthly expenses: $totalExpenses KES
        - Spending by category: ${spendingByCategory.map { "${it.key}: ${it.value} KES" }.joinToString(", ")}
        - Total debt: $totalDebt KES
        - Debt details: $debtDetails

        Provide a detailed report with:
        1. A summary of their financial health.
        2. Where they’re spending the most and if it’s sustainable.
        3. Debt repayment priorities (e.g., what to pay first).
        4. Any urgent actions (e.g., overdue payments).
        5. Friendly, actionable advice to improve their finances.

        Use a warm, encouraging tone and include specific numbers where possible.
    """.trimIndent()

        return try {
            val response = generativeModel.generateContent(content { text(prompt) })
            response.text ?: "Sorry, I couldn’t analyze your data right now."
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "AI Analysis failed: ${e.message}")
            "Oops! Something went wrong with the analysis."
        }
    }

}