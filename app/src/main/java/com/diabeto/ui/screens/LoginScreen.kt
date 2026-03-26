package com.diabeto.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabeto.R
import com.diabeto.data.model.UserRole
import com.diabeto.ui.theme.*
import com.diabeto.ui.viewmodel.AuthViewModel
import com.diabeto.ui.viewmodel.LoginMode
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

// Web Client ID pour Google Sign-In (oauth_client type 3 dans google-services.json)
private const val WEB_CLIENT_ID = "630181163819-78j1ha7vo07ldokvqo30cntt3tlanjv2.apps.googleusercontent.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // ── Google Sign-In launcher ─────────────────────────────────────
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                viewModel.signInWithGoogle(idToken)
            }
        } catch (_: ApiException) {
            // L'utilisateur a annulé
        }
    }

    // Animation gradient
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientAnim"
    )

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.resetEmailSent) {
        if (uiState.resetEmailSent) {
            snackbarHostState.showSnackbar("Email de réinitialisation envoyé !")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ══════════════════════════════════════════════════════════
            //  FOND DEGRADE ANIME + FLOU
            // ══════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00D2FF),
                                Color(0xFF0B8FAC),
                                Color(0xFF005F73),
                                Color(0xFF0B8FAC),
                                Color(0xFF00D2FF)
                            ),
                            start = Offset(animOffset, 0f),
                            end = Offset(animOffset + 800f, 1200f)
                        )
                    )
            )

            // Cercles décoratifs flous
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-80).dp, y = (-60).dp)
                    .blur(60.dp)
                    .background(Color(0xFF00D2FF).copy(alpha = 0.4f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .blur(50.dp)
                    .background(Color(0xFF005F73).copy(alpha = 0.5f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-30).dp)
                    .blur(40.dp)
                    .background(Color(0xFF26A69A).copy(alpha = 0.3f), CircleShape)
            )

            // ══════════════════════════════════════════════════════════
            //  CONTENU PRINCIPAL
            // ══════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // Logo
                Image(
                    painter = painterResource(id = R.drawable.ic_diasmart_logo),
                    contentDescription = "DiaSmart Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (uiState.showRegister) "Bienvenue !" else "Bonjour !",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (uiState.showRegister) "Créez votre compte DiaSmart" else "Connectez-vous à DiaSmart",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ══════════════════════════════════════════════════════
                //  CARTE FORMULAIRE BLANCHE
                // ══════════════════════════════════════════════════════
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                            ambientColor = Color.Black.copy(alpha = 0.15f)
                        ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (uiState.showRegister) "Inscription" else "Connexion",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E)
                        )

                        // ── Toggle Email / Téléphone (connexion seulement) ──
                        if (!uiState.showRegister) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF0F0F0))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                LoginModeTab(
                                    label = "Email",
                                    icon = Icons.Outlined.Email,
                                    isSelected = uiState.loginMode == LoginMode.EMAIL,
                                    onClick = { viewModel.setLoginMode(LoginMode.EMAIL) },
                                    modifier = Modifier.weight(1f)
                                )
                                LoginModeTab(
                                    label = "Téléphone",
                                    icon = Icons.Outlined.Phone,
                                    isSelected = uiState.loginMode == LoginMode.PHONE,
                                    onClick = { viewModel.setLoginMode(LoginMode.PHONE) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // ── Champs inscription ──────────────────────────
                        AnimatedVisibility(
                            visible = uiState.showRegister,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    DiaSmartTextField(
                                        value = uiState.prenom,
                                        onValueChange = viewModel::onPrenomChange,
                                        label = "Prénom",
                                        icon = Icons.Outlined.Person,
                                        modifier = Modifier.weight(1f),
                                        imeAction = ImeAction.Next
                                    )
                                    DiaSmartTextField(
                                        value = uiState.nom,
                                        onValueChange = viewModel::onNomChange,
                                        label = "Nom",
                                        icon = Icons.Outlined.Badge,
                                        modifier = Modifier.weight(1f),
                                        imeAction = ImeAction.Next
                                    )
                                }
                                Text(
                                    "Je suis :",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF444444)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    UserRole.entries.forEach { role ->
                                        val isSelected = uiState.selectedRole == role
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .selectable(
                                                    selected = isSelected,
                                                    onClick = { viewModel.onRoleChange(role) },
                                                    role = Role.RadioButton
                                                ),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) Primary.copy(alpha = 0.12f) else Color(0xFFF5F5F5),
                                            border = if (isSelected) BorderStroke(2.dp, Primary) else BorderStroke(1.dp, Color(0xFFE0E0E0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (role == UserRole.PATIENT) Icons.Outlined.PersonOutline else Icons.Outlined.LocalHospital,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Primary else Color(0xFF888888),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (role == UserRole.PATIENT) "Patient" else "Médecin",
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Primary else Color(0xFF555555),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // ── MODE EMAIL ──────────────────────────────────
                        AnimatedVisibility(
                            visible = uiState.loginMode == LoginMode.EMAIL || uiState.showRegister,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                DiaSmartTextField(
                                    value = uiState.email,
                                    onValueChange = viewModel::onEmailChange,
                                    label = "Email",
                                    icon = Icons.Outlined.Email,
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                                DiaSmartTextField(
                                    value = uiState.password,
                                    onValueChange = viewModel::onPasswordChange,
                                    label = "Mot de passe",
                                    icon = Icons.Outlined.Lock,
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                    isPassword = true,
                                    showPassword = showPassword,
                                    onTogglePassword = { showPassword = !showPassword }
                                )
                                if (!uiState.showRegister) {
                                    TextButton(
                                        onClick = viewModel::resetPassword,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(
                                            "Mot de passe oublié ?",
                                            fontSize = 12.sp,
                                            color = Primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // ── MODE TELEPHONE ──────────────────────────────
                        AnimatedVisibility(
                            visible = uiState.loginMode == LoginMode.PHONE && !uiState.showRegister,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                DiaSmartTextField(
                                    value = uiState.phoneNumber,
                                    onValueChange = viewModel::onPhoneNumberChange,
                                    label = "Numéro de téléphone",
                                    icon = Icons.Outlined.Phone,
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done,
                                    placeholder = "0555 12 34 56"
                                )
                                AnimatedVisibility(
                                    visible = uiState.isCodeSent,
                                    enter = fadeIn() + expandVertically()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            "Un code SMS a été envoyé",
                                            fontSize = 13.sp,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                        DiaSmartTextField(
                                            value = uiState.smsCode,
                                            onValueChange = viewModel::onSmsCodeChange,
                                            label = "Code de vérification",
                                            icon = Icons.Outlined.Sms,
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done,
                                            placeholder = "123456"
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // ── BOUTON PRINCIPAL GRADIENT ────────────────────
                        Button(
                            onClick = {
                                when {
                                    uiState.showRegister -> viewModel.signUp()
                                    uiState.loginMode == LoginMode.PHONE -> {
                                        if (uiState.isCodeSent) {
                                            viewModel.verifyPhoneCode()
                                        } else {
                                            (context as? Activity)?.let { viewModel.sendPhoneVerification(it) }
                                        }
                                    }
                                    else -> viewModel.signIn()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !uiState.isLoading
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = if (uiState.isLoading)
                                                listOf(Color(0xFF90CAF9), Color(0xFF90CAF9))
                                            else
                                                listOf(Color(0xFF00D2FF), Color(0xFF0B8FAC), Color(0xFF005F73))
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                uiState.showRegister -> Icons.Default.PersonAdd
                                                uiState.loginMode == LoginMode.PHONE && uiState.isCodeSent -> Icons.Default.VerifiedUser
                                                uiState.loginMode == LoginMode.PHONE -> Icons.Default.Sms
                                                else -> Icons.Default.Login
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = when {
                                                uiState.showRegister -> "Créer mon compte"
                                                uiState.loginMode == LoginMode.PHONE && uiState.isCodeSent -> "Vérifier le code"
                                                uiState.loginMode == LoginMode.PHONE -> "Envoyer le code SMS"
                                                else -> "Se connecter"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // ── SEPARATEUR ───────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                            Text("  ou connectez-vous avec  ", color = Color(0xFF999999), fontSize = 11.sp)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                        }

                        // ── BOUTONS SOCIAL ───────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Google
                            OutlinedButton(
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(WEB_CLIENT_ID)
                                        .requestEmail()
                                        .build()
                                    val client = GoogleSignIn.getClient(context, gso)
                                    client.signOut()
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                            ) {
                                Text("G", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF555555))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Téléphone raccourci
                            OutlinedButton(
                                onClick = {
                                    if (uiState.showRegister) viewModel.toggleRegister(false)
                                    viewModel.setLoginMode(
                                        if (uiState.loginMode == LoginMode.PHONE) LoginMode.EMAIL else LoginMode.PHONE
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Téléphone", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF555555))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // ── Toggle Connexion / Inscription ──────────────
                        OutlinedButton(
                            onClick = { viewModel.toggleRegister(!uiState.showRegister) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(Color(0xFF00D2FF), Color(0xFF0B8FAC)))
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                        ) {
                            Icon(
                                imageVector = if (uiState.showRegister) Icons.Default.Login else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.showRegister) "J'ai déjà un compte" else "Créer un compte",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "DiaSmart — Gestion intelligente du diabète",
                            fontSize = 11.sp,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  COMPOSANTS REUTILISABLES
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun LoginModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSelected) Primary else Color(0xFF888888))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Primary else Color(0xFF888888))
        }
    }
}

@Composable
private fun DiaSmartTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, fontSize = 13.sp, color = Color(0xFFBBBBBB)) }
        } else null,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color(0xFFF8FFFE),
            unfocusedContainerColor = Color(0xFFF5F5F5),
            focusedLabelColor = Primary,
            unfocusedLabelColor = Color(0xFF999999),
            cursorColor = Primary
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        visualTransformation = if (isPassword && !showPassword)
            PasswordVisualTransformation()
        else
            VisualTransformation.None,
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onTogglePassword?.invoke() }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null
    )
}
