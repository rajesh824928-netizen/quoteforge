package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.QuoteForgeLogoMark
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.WarmIvory
import com.example.ui.viewmodel.QuotationViewModel
import kotlinx.coroutines.launch

@Composable
fun LandingScreen(
    viewModel: QuotationViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToWorkspaceSetup: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSignUpMode by remember { mutableStateOf(false) }
    var showEmailForm by remember { mutableStateOf(false) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleInputEmail by remember { mutableStateOf("") }
    var googleInputName by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val workspaces by viewModel.allWorkspaces.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Hero Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Official QuoteForge Brand Logo Mark matching image.png
                QuoteForgeLogoMark(
                    size = 80.dp,
                    elevation = 6.dp,
                    modifier = Modifier.testTag("landing_quoteforge_logo")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "QuoteForge",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Professional Quotation & Estimation SaaS",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature Highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeaturePill(icon = Icons.Outlined.DesignServices, title = "Smart BOQ", modifier = Modifier.weight(1f))
                    FeaturePill(icon = Icons.Outlined.Receipt, title = "GST Engine", modifier = Modifier.weight(1f))
                    FeaturePill(icon = Icons.Outlined.PictureAsPdf, title = "PDF Studio", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Authentication Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (!showEmailForm) "Get Started" else if (isSignUpMode) "Create your SaaS Account" else "Welcome Back",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (!showEmailForm) "Sign in to manage projects, teams, and high-fidelity estimates" else "Enter your credentials to continue",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // 1. CONTINUE WITH GOOGLE BUTTON
                    Button(
                        onClick = {
                            isLoading = true
                            coroutineScope.launch {
                                val result = viewModel.authManager.signInWithGoogle(context)
                                isLoading = false
                                result.onSuccess { user ->
                                    Toast.makeText(context, "Signed in as ${user.displayName}", Toast.LENGTH_SHORT).show()
                                    viewModel.ensureWorkspaceForUser(user)
                                    if (workspaces.isEmpty()) {
                                        onNavigateToWorkspaceSetup()
                                    } else {
                                        onNavigateToDashboard()
                                    }
                                }.onFailure { _ ->
                                    // Credential Manager not active or user cancelled; prompt custom Google account entry
                                    showGoogleDialog = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_continue_google"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Google "G" circle badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFF4285F4)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Divider with "OR"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  OR  ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    // 2. CREATE ACCOUNT / EMAIL FORM TOGGLE
                    AnimatedVisibility(
                        visible = showEmailForm,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSignUpMode) {
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("Full Name or Studio Name") },
                                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    coroutineScope.launch {
                                        val result = if (isSignUpMode) {
                                            viewModel.authManager.createAccountWithEmail(email, password, displayName)
                                        } else {
                                            viewModel.authManager.signInWithEmail(email, password)
                                        }
                                        isLoading = false
                                        result.onSuccess { user ->
                                            Toast.makeText(context, "Logged in as ${user.displayName}", Toast.LENGTH_SHORT).show()
                                            viewModel.ensureWorkspaceForUser(user)
                                            if (workspaces.isEmpty()) {
                                                onNavigateToWorkspaceSetup()
                                            } else {
                                                onNavigateToDashboard()
                                            }
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_submit_auth"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(if (isSignUpMode) "Create Account" else "Sign In", fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Already have an account?" else "Need an account?",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                                    Text(
                                        text = if (isSignUpMode) "Sign In" else "Create Account",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (!showEmailForm) {
                        Button(
                            onClick = {
                                showEmailForm = true
                                isSignUpMode = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_create_account"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create an Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        TextButton(
                            onClick = {
                                showEmailForm = true
                                isSignUpMode = false
                            },
                            modifier = Modifier.testTag("btn_toggle_signin")
                        ) {
                            Text("Already registered? Sign In with Email", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Demo & Guest Quick Access
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.signInAsDemo {
                                Toast.makeText(context, "Loaded Knot Architects Demo Workspace", Toast.LENGTH_SHORT).show()
                                onNavigateToDashboard()
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_quick_demo")
                ) {
                    Icon(Icons.Outlined.BusinessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Explore Knot Architects Demo Workspace", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cloud Drive Sync • Multi-Workspace • GST Ready",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF4285F4))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In with Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter your Google account email to sign in and link your architectural cloud workspace:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = googleInputEmail,
                        onValueChange = { googleInputEmail = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("yourname@gmail.com") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_google_email")
                    )
                    OutlinedTextField(
                        value = googleInputName,
                        onValueChange = { googleInputName = it },
                        label = { Text("Your Name or Studio Name (Optional)") },
                        placeholder = { Text("e.g. Ar. Vikram Mehta") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_google_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputEmail = googleInputEmail.trim()
                        if (inputEmail.isBlank() || !inputEmail.contains("@")) {
                            Toast.makeText(context, "Please enter a valid Google email address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showGoogleDialog = false
                        isLoading = true
                        coroutineScope.launch {
                            val result = viewModel.authManager.signInWithGoogle(
                                context = context,
                                explicitEmail = inputEmail,
                                explicitDisplayName = googleInputName.ifBlank { null }
                            )
                            isLoading = false
                            result.onSuccess { user ->
                                Toast.makeText(context, "Signed in as ${user.displayName}", Toast.LENGTH_SHORT).show()
                                viewModel.ensureWorkspaceForUser(user)
                                if (workspaces.isEmpty()) {
                                    onNavigateToWorkspaceSetup()
                                } else {
                                    onNavigateToDashboard()
                                }
                            }.onFailure { err ->
                                Toast.makeText(context, "Sign-in failed: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_google_signin")
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = title, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
