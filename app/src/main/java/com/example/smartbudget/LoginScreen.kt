
package com.example.smartbudget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle // Placeholder for Google icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var showLoginFields by remember { mutableStateOf(false) }

    // Google Sign-In setup
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("83956256458-b5g043l0598t2t0fh5gmljchodkcpjli.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            scope.launch {
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                            onLoginSuccess(userId)
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            error = "Google Sign-In failed: ${task.exception?.message}"
                        }
                    }
            }
        } catch (e: ApiException) {
            error = "Google Sign-In error: ${e.message}"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = "Robot Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (!showLoginFields) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp)) // Not too top
                    Text(
                        text = "Welcome to",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontSize = 40.sp // Increased font size
                    )
                    Text(
                        text = "SmartBudget",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp // Increased font size
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Spacer(modifier = Modifier.height(24.dp))


                // Subtitle
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Manage your finances with",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Add spacing between lines
                    Text(
                        text = "help from our chatbot",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Get Started Button
                    Button(
                        onClick = { showLoginFields = true },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF006400), // Darker green
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(48.dp)) // Not too bottom
            } else {
                // Login Fields
                Column (
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.Black, // Text color when focused
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.Black, // Label color when focused
                            unfocusedLabelColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.Black, // Text color when focused
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.Black, // Label color when focused
                            unfocusedLabelColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (error.isNotEmpty()) {
                        Text(error, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val userId = auth.currentUser?.uid
                                                    ?: return@addOnCompleteListener
                                                onLoginSuccess(userId)
                                                navController.navigate("dashboard") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            } else {
                                                error = "Login failed: ${task.exception?.message}"
                                            }
                                        }
                                }
                            },
                            enabled = email.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF154815),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF004D40), // Slightly darker when disabled
                                disabledContentColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Login")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val userId = auth.currentUser?.uid
                                                    ?: return@addOnCompleteListener
                                                onLoginSuccess(userId)
                                                navController.navigate("dashboard") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            } else {
                                                error =
                                                    "Registration failed: ${task.exception?.message}"
                                            }
                                        }
                                }
                            },
                            enabled = email.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50), // Lighter green
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF388E3C), // Slightly darker when disabled
                                disabledContentColor = Color.LightGray

                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Register")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { launcher.launch(googleSignInClient.signInIntent) },

                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.signin_google),
                            contentDescription = "Google Sign-In",
                            tint = Color.Unspecified
                        )

                    }
                }
            }
        }
    }
}