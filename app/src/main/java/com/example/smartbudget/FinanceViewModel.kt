package com.example.smartbudget

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.ParseException
import java.text.SimpleDateFormat
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

data class Goal(
    val description: String,
    val targetAmount: Double,
    val targetDate: Date? = null
)


class FinanceViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-lite",
        apiKey = BuildConfig.apiKey
    )

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _categorySuggestion = MutableStateFlow<String?>(null)
    val categorySuggestion: StateFlow<String?> = _categorySuggestion.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList()) // Mutable StateFlow for debts
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow() // Public StateFlow for debts

    private val _categorySpending = MutableStateFlow<Map<String, Double>>(emptyMap())

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val chatHistory: StateFlow<List<Pair<String, String>>> = _chatHistory.asStateFlow()

    // testing purposes for demo
    init {
        if (_expenses.value.isEmpty() && _debts.value.isEmpty() && _goals.value.isEmpty()) {
            loadTestData()
        }
    }
    fun loadTestData() {
        _expenses.value = listOf(
            Expense("Mama Mboga (Veggies)", 150.0, "Food"),
            Expense("Matatu to Campus", 70.0, "Transport"),
            Expense("Chapo Smokie", 50.0, "Food"),
            Expense("Airtime", 100.0, "Communication"),
            Expense("Photocopy Notes", 30.0, "Education")
        )
        _debts.value = listOf(
            Debt("HELB Loan", 5000.0, Date(2025 - 1900, 11, 31)),
            Debt("Roommate Borrowed", 200.0, Date(2025 - 1900, 4, 1))
        )
        _goals.value = listOf(
            Goal("New Laptop", 15000.0, Date(2025 - 1900, 7, 31)),
            Goal("Graduation Party", 3000.0, Date(2025 - 1900, 11, 1))
        )
    }

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

    fun addGoal(description: String, targetAmount: Double, targetDate: String?) {
        val date = targetDate?.let {
            SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(it)
        }
        val newGoal = Goal(description, targetAmount, date)
        _goals.value += newGoal
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
        val goals = _goals.value
        val totalExpenses = expenses.sumOf { it.amount }
        val totalDebt = debts.sumOf { it.totalAmount }
        val spendingByCategory = expenses.groupBy { it.category ?: "Uncategorized" }
            .mapValues { it.value.sumOf { it.amount } }
        val debtDetails = debts.joinToString("\n") {
            "${it.description}: ${it.totalAmount} KES due ${it.dueDate?.let { SimpleDateFormat("MM/dd/yyyy", Locale.US).format(it) } ?: "N/A"}"
        }
        val goalDetails = goals.joinToString("\n") {
            "${it.description}: ${it.targetAmount} KES${it.targetDate?.let { " due ${SimpleDateFormat("MM/dd/yyyy", Locale.US).format(it)}" } ?: ""}"
        }

        val prompt = """
        Analyze the user’s financial data:
        - Total monthly expenses: $totalExpenses KES
        - Spending by category: ${spendingByCategory.map { "${it.key}: ${it.value} KES" }.joinToString(", ")}
        - Total debt: $totalDebt KES
        - Debt details: $debtDetails
        - Financial goals: $goalDetails
        - Today's date is  is ${SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date())}

        Write a response in this exact style:
        - Start with a friendly note about their spending and debt, saying if they’re doing okay.
        - Point out where most of their money is going, keeping it casual.
        - Suggest which debt to pay first and why, mentioning due dates.
        - Note if anything’s overdue or not.
        - End with practical suggestions to reach their goals, like cutting spending or saving more, tied to their data.

        Use short paragraphs, a warm and chatty tone, and specific numbers. Avoid markdown or formal labels.
    """.trimIndent()

        return try {
            Log.d("FinanceViewModel", "AI Analysis prompt: $prompt")
            val response = generativeModel.generateContent(content { text(prompt) })
            val result = response.text ?: "Sorry, I couldn’t analyze your data right now."
//            _chatHistory.value += Pair("Get AI Analysis", result)

            result
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "AI Analysis failed: ${e.message}")
            val error = "Oops! Something went wrong with the analysis."
//            _chatHistory.value += Pair("Get AI Analysis", error)
            error
        }
    }

    suspend fun getChatResponse(userQuery: String): String {
        val expenses = _expenses.value
        val debts = _debts.value
        val goals = _goals.value
        val totalExpenses = expenses.sumOf { it.amount }
        val totalDebt = debts.sumOf { it.totalAmount }
        val spendingByCategory = expenses.groupBy { it.category ?: "Uncategorized" }
            .mapValues { it.value.sumOf { it.amount } }
        val debtDetails = debts.joinToString("\n") {
            "${it.description}: ${it.totalAmount} KES due ${it.dueDate?.let { SimpleDateFormat("MM/dd/yyyy", Locale.US).format(it) } ?: "N/A"}"
        }
        val goalDetails = goals.joinToString("\n") {
            "${it.description}: ${it.targetAmount} KES${it.targetDate?.let { " due ${SimpleDateFormat("MM/dd/yyyy", Locale.US).format(it)}" } ?: ""}"
        }

        val prompt = """
            The user asked: "$userQuery"
            Their financial data:
            - Total monthly expenses: $totalExpenses KES
            - Spending by category: ${spendingByCategory.map { "${it.key}: ${it.value} KES" }.joinToString(", ")}
            - Total debt: $totalDebt KES
            - Debt details: $debtDetails
            - Financial goals: $goalDetails
            - Past chat history: ${_chatHistory.value.joinToString("\n") { "User: ${it.first}\nAI: ${it.second}" }}
            - Today's date is  is ${SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date())}
        
            Act as a financial expert and provide a detailed, thorough response in a friendly, conversational tone. Use the user’s financial data and past chats to give personalized advice. Include specific numbers and examples where relevant, explain your reasoning, and offer actionable steps they can take. Make the response comprehensive, not short or long, and avoid markdown or formal labels.
        """.trimIndent()

        return try {
            Log.d("FinanceViewModel", "AI Chat prompt: $prompt")
            val response = generativeModel.generateContent(content { text(prompt) })
            val result = response.text ?: "Hmm, I’m not sure how to answer that right now!"
            _chatHistory.value += Pair(userQuery, result)
            result
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "Chat response failed: ${e.message}")
            val error = "Oops, something went wrong with my reply!"
            _chatHistory.value += Pair(userQuery, error)
            error
        }
    }

}