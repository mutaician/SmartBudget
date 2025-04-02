package com.example.smartbudget

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Expense(
    val description: String = "",
    val amount: Double = 0.0,
    val category: String? = null,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class Debt(
    val description: String = "",
    val totalAmount: Double = 0.0,
    val dueDate: Date? = null,
    val userId: String? = null
)

data class Goal(
    val description: String = "",
    val targetAmount: Double = 0.0,
    val targetDate: Date? = null,
    val userId: String? = null
)

data class ChatEntry(
    val query: String = "",
    val response: String = "",
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)


class FinanceViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-lite",
        apiKey = BuildConfig.apiKey
    )

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _categorySuggestion = MutableStateFlow<String?>(null)
    val categorySuggestion: StateFlow<String?> = _categorySuggestion.asStateFlow()

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts.asStateFlow()

    private val _categorySpending = MutableStateFlow<Map<String, Double>>(emptyMap())

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val chatHistory: StateFlow<List<Pair<String, String>>> = _chatHistory.asStateFlow()

    private val _currentEmail = MutableStateFlow<String?>(null)
    val currentEmail: StateFlow<String?> = _currentEmail.asStateFlow()

    private var userId: String? = null


    fun setUserEmail(userId: String) {
        this.userId = userId
        val auth = FirebaseAuth.getInstance()
        val email = auth.currentUser?.email ?: "unknown@example.com"
        _currentEmail.value = email
        db.collection("users").document(userId).set(mapOf("email" to email))
            .addOnSuccessListener {
                Log.d("FinanceViewModel", "User email set: $email")
            }
            .addOnFailureListener { e ->
                Log.e("FinanceViewModel", "Failed to set user email: ${e.message}")
            }
        listenToExpenses(userId)
        listenToDebts(userId)
        listenToGoals(userId)
        listenToChatHistory(userId)

    }

    private fun listenToExpenses(userId: String) {
        db.collection("users").document(userId).collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FinanceViewModel", "Firestore listener error: ${e.message}")
                    return@addSnapshotListener
                }
                val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                _expenses.value = expenses
                updateCategorySpending()
            }
    }

    private fun listenToDebts(userId: String) {
        db.collection("users").document(userId).collection("debts")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FinanceViewModel", "Firestore debts listener error: ${e.message}")
                    return@addSnapshotListener
                }
                val debts = snapshot?.toObjects(Debt::class.java) ?: emptyList()
                _debts.value = debts
            }
    }

    private fun listenToGoals(userId: String) {
        db.collection("users").document(userId).collection("goals")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FinanceViewModel", "Firestore goals listener error: ${e.message}")
                    return@addSnapshotListener
                }
                val goals = snapshot?.toObjects(Goal::class.java) ?: emptyList()
                _goals.value = goals
            }
    }

    private fun listenToChatHistory(userId: String) {
        db.collection("users").document(userId).collection("chat_history")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FinanceViewModel", "Firestore chat history listener error: ${e.message}")
                    return@addSnapshotListener
                }
                val chatEntries = snapshot?.toObjects(ChatEntry::class.java) ?: emptyList()
                _chatHistory.value = chatEntries.map { it.query to it.response }
            }
    }




    fun addExpense(description: String, amount: Double, category: String?) {
        val userId = this.userId ?: return
        val newExpense = Expense(description = description, amount = amount, category = category, userId = userId)
        db.collection("users").document(userId).collection("expenses").add(newExpense)
            .addOnSuccessListener {
                Log.d("FinanceViewModel", "Expense added to Firestore: $newExpense")
            }
            .addOnFailureListener { e ->
                Log.d("FinanceViewModel", "Failure to add expense: ${e.message}")
            }
        _categorySuggestion.value = null
        updateCategorySpending()
    }

    fun addDebt(description: String, totalAmount: Double, dueDateString: String) {
        val userId = this.userId ?: return
        val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        var dueDate: Date? = null
        if (dueDateString.isNotBlank()) {
            try {
                dueDate = dateFormatter.parse(dueDateString)
            } catch (e: ParseException) {
                Log.e("FinanceViewModel", "Error parsing due date: $dueDateString", e)
            }
        }

        val newDebt = Debt(description = description, totalAmount = totalAmount, dueDate = dueDate, userId = userId)
        db.collection("users").document(userId).collection("debts").add(newDebt)
            .addOnSuccessListener {
                Log.d("FinanceViewModel", "Debt added to Firestore: $newDebt")
            }
            .addOnFailureListener { e ->
                Log.e("FinanceViewModel", "Failed to add debt: ${e.message}")
            }
    }

    fun addGoal(description: String, targetAmount: Double, targetDate: String?) {
        val userId = this.userId ?: return
        val date = targetDate?.let {
            SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(it)
        }
        val newGoal = Goal(description = description, targetAmount = targetAmount, targetDate = date, userId = userId )
        db.collection("users").document(userId).collection("goals").add(newGoal)
            .addOnSuccessListener {
                Log.d("FinanceViewModel", "Goal added to Firestore: $newGoal")
            }
            .addOnFailureListener { e ->
                Log.e("FinanceViewModel", "Failed to add goal: ${e.message}")
            }
    }

    private fun updateCategorySpending() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val monthlyExpenses = _expenses.value.filter { expense ->
            val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
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
        this.userId ?: return "Please log in first."
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
        val userId = this.userId ?: return "Please log in first."
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
    You are a Kenyan-based financial expert with extensive knowledge of the local and regional financial markets, economic trends, and investment opportunities. Your role is to provide accurate, culturally relevant, and up-to-date financial advice to clients in Kenya and East Africa.

    When responding, always consider the Kenyan financial context:
    - The primary currency is the Kenyan Shilling (KES).
    - Key financial institutions include the Central Bank of Kenya, Nairobi Securities Exchange, and major local banks.
    - Relevant economic factors such as inflation rates, GDP growth, and government policies.
    - Popular investment options like M-Akiba (government bonds), real estate, and mobile money platforms (e.g., M-Pesa).

    The user asked: "$userQuery"
    Their financial data:
    - Total monthly expenses: $totalExpenses KES
    - Spending by category: ${spendingByCategory.map { "${it.key}: ${it.value} KES" }.joinToString(", ")}
    - Total debt: $totalDebt KES
    - Debt details: $debtDetails
    - Financial goals: $goalDetails
    - Past chat history: ${_chatHistory.value.joinToString("\n") { "User: ${it.first}\nAI: ${it.second}" }}
    - Today's date is ${SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date())}

    Follow these steps:
    1. Carefully read and analyze the query to understand the user's financial concern or question.
    2. Consider the Kenyan and East African financial context relevant to the query and their data.
    3. Formulate a response in a friendly, conversational tone that addresses their needs, incorporating local financial knowledge and their specific financial data.

    Guidelines:
    - Prioritize the user's financial well-being and risk tolerance.
    - Provide balanced advice, discussing benefits and risks.
    - Reference Kenyan financial regulations or tax implications if applicable.
    - If the query is outside your expertise or requires legal advice, suggest consulting a professional.
    - Use KES as the primary currency, mentioning USD equivalents only for international comparisons.
    - CRITICAL: Respond in plain text only. Do not use Markdown (no *, **, #, etc.), bullet points, or any formatting symbols—just simple, readable text.

    Format your response:
    1. Start with a brief acknowledgment of the user's query.
    2. Provide your expert analysis and advice, using their data and Kenyan context.
    3. Conclude with a summary or key takeaway points.

    Keep it warm, chatty, and specific, with numbers from their data where relevant. Again, plain text only—no formatting!
""".trimIndent()

        return try {
            Log.d("FinanceViewModel", "AI Chat prompt: $prompt")
            val response = generativeModel.generateContent(content { text(prompt) })
            val result = response.text ?: "Hmm, I’m not sure how to answer that right now!"
            val chatEntry = ChatEntry(query = userQuery, response = result, userId = userId)
            db.collection("users").document(userId).collection("chat_history").add(chatEntry)
                .addOnSuccessListener {
                    Log.d("FinanceViewModel", "Chat entry added: $chatEntry")
                }
                .addOnFailureListener { e ->
                    Log.e("FinanceViewModel", "Failed to add chat entry: ${e.message}")
                }
            result
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "Chat response failed: ${e.message}")
            "Oops, something went wrong with my reply!"
        }
    }

}