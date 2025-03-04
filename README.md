# SmartBudget

SmartBudget is an Android personal finance application built with Jetpack Compose that helps users track expenses, manage debts, and gain insights into their spending habits through AI-powered analysis using Google's Gemini API.

## Features

### Expense Tracking
- Add expenses with descriptions, amounts, and categories
- AI-powered category suggestions using Gemini API
- Expense history display with categorization

### Debt Management
- Track debts with descriptions, amounts, and due dates
- Calendar date picker for selecting due dates
- Debt summary view with payment deadlines

### Financial Analytics
- Category spending breakdowns
- Visual charts for spending patterns
- AI-powered financial analysis and personalized recommendations

## Screenshots

[Add screenshots here]

## Architecture

SmartBudget follows the MVVM (Model-View-ViewModel) architecture pattern:
- **View Layer**: Jetpack Compose UI components
- **ViewModel**: Handles UI logic and data processing
- **Model**: Data classes for expenses and debts

## Technologies Used

- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI toolkit for building native Android UI
- **Coroutines**: For asynchronous programming
- **Google Generative AI SDK**: For AI-powered analysis and category suggestions
- **Material 3**: Modern design system implementation

## Setup Instructions

1. Clone the repository:
```bash
git clone https://github.com/mutaician/SmartBudget.git
```
2. Add your Gemini API key to the `local.properties` file:
```
apiKey=your_gemini_api_key_here
```
3. Build and run the project in Android Studio

## Future Roadmap

- Data persistence using Room database
- User authentication and cloud backup
- Budget setting and tracking
- Receipt scanning for automatic expense entry
- Multiple currency support
- Expense sharing for group finances

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

