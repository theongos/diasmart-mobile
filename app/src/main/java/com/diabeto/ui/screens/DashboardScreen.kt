package com.diabeto.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabeto.R
import com.diabeto.util.AppUpdateChecker
import com.diabeto.data.entity.RendezVousAvecPatient
import com.diabeto.data.model.UserRole
import com.diabeto.ui.components.RollyIcon
import com.diabeto.ui.components.RollyIconInline
import com.diabeto.ui.theme.*
import com.diabeto.ui.viewmodel.DashboardViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPatients: () -> Unit,
    onNavigateToPatientDetail: (Long) -> Unit,
    onNavigateToRendezVous: () -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToChatbot: () -> Unit = {},
    onNavigateToMessagerie: () -> Unit = {},
    onNavigateToRepasAnalyse: () -> Unit = {},
    onNavigateToDataSharing: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToJournal: () -> Unit = {},
    onNavigateToPedometer: () -> Unit = {},
    onNavigateToPredictive: () -> Unit = {},
    onNavigateToValidations: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // ── Mise a jour automatique ──
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    var updateChangelog by remember { mutableStateOf("") }
    var updateForce by remember { mutableStateOf(false) }

    var needsInstallPermission by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    // Fonction pour vérifier la permission d'installation
    fun checkInstallPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            !context.packageManager.canRequestPackageInstalls()
        } else false
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("diasmart_prefs", android.content.Context.MODE_PRIVATE)
        val pendingVersion = prefs.getString("pending_update_version", null)
        val pendingUrl = prefs.getString("pending_update_url", null)
        if (!pendingVersion.isNullOrBlank() && !pendingUrl.isNullOrBlank()) {
            updateVersion = pendingVersion
            updateUrl = pendingUrl
            updateChangelog = prefs.getString("pending_update_changelog", "") ?: ""
            updateForce = prefs.getBoolean("pending_update_force", false)
            needsInstallPermission = checkInstallPermission()
            showUpdateDialog = true
        }
    }

    // Re-vérifier la permission quand l'app revient au premier plan (retour des paramètres)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && showUpdateDialog) {
                val hadPermissionIssue = needsInstallPermission
                needsInstallPermission = checkInstallPermission()

                // Si la permission vient d'être accordée, lancer le téléchargement automatiquement
                if (hadPermissionIssue && !needsInstallPermission && updateUrl.isNotBlank()) {
                    val checker = AppUpdateChecker(context)
                    checker.downloadAndInstall(updateUrl, updateVersion)
                    isDownloading = true
                    showUpdateDialog = false
                    context.getSharedPreferences("diasmart_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().remove("pending_update_version").remove("pending_update_url")
                        .remove("pending_update_changelog").remove("pending_update_force").apply()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Dialog de mise a jour
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!updateForce) {
                    showUpdateDialog = false
                    context.getSharedPreferences("diasmart_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().remove("pending_update_version").remove("pending_update_url")
                        .remove("pending_update_changelog").remove("pending_update_force").apply()
                }
            },
            icon = {
                Icon(Icons.Default.SystemUpdateAlt, null, tint = Primary, modifier = Modifier.size(40.dp))
            },
            title = {
                Text("Mise a jour disponible", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "Version $updateVersion",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                    if (updateChangelog.isNotBlank()) {
                        Text("Nouveautes :", fontWeight = FontWeight.Medium)
                        Text(
                            updateChangelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (needsInstallPermission) {
                        HorizontalDivider()
                        Text(
                            "Pour installer automatiquement les mises a jour, autorisez l'installation depuis cette source dans les parametres.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Warning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (updateForce) {
                        Text(
                            "Cette mise a jour est obligatoire.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (needsInstallPermission) {
                        // Bouton 1 : Ouvrir les paramètres de permission
                        Button(
                            onClick = {
                                val checker = AppUpdateChecker(context)
                                checker.openInstallPermissionSettings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Warning)
                        ) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Autoriser les mises a jour")
                        }
                        Spacer(Modifier.height(8.dp))
                        // Bouton 2 : Télécharger directement via navigateur (fallback)
                        TextButton(
                            onClick = {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(updateUrl)
                                )
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Telecharger via navigateur", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                val checker = AppUpdateChecker(context)
                                checker.downloadAndInstall(updateUrl, updateVersion)
                                isDownloading = true
                                showUpdateDialog = false
                                context.getSharedPreferences("diasmart_prefs", android.content.Context.MODE_PRIVATE)
                                    .edit().remove("pending_update_version").remove("pending_update_url")
                                    .remove("pending_update_changelog").remove("pending_update_force").apply()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Installer la mise a jour")
                        }
                    }
                }
            },
            dismissButton = {
                if (!updateForce) {
                    TextButton(onClick = {
                        showUpdateDialog = false
                        context.getSharedPreferences("diasmart_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().remove("pending_update_version").remove("pending_update_url")
                            .remove("pending_update_changelog").remove("pending_update_force").apply()
                    }) {
                        Text("Plus tard")
                    }
                }
            }
        )
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            // ── Top Bar avec gradient ─────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GradientStart, GradientMid, GradientEnd)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_diasmart_logo),
                                contentDescription = "DiaSmart",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "DiaSmart",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Tableau de bord",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = onNavigateToProfile) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = "Mon Profil",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Paramètres",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onNavigateToAddPatient) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Nouveau patient",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // ── Bottom Navigation Bar moderne ─────────────────
            DiaSmartBottomBar(
                selectedIndex = selectedNavIndex,
                onNavigateToPatients = { selectedNavIndex = 1; onNavigateToPatients() },
                onNavigateToRendezVous = { selectedNavIndex = 2; onNavigateToRendezVous() },
                onNavigateToChatbot = { selectedNavIndex = 3; onNavigateToChatbot() },
                onNavigateToMessagerie = { selectedNavIndex = 4; onNavigateToMessagerie() },
                onDashboard = { selectedNavIndex = 0 },
                isMedecin = uiState.userRole == UserRole.MEDECIN
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Bannière hors-ligne ───────────────────────────
            if (!uiState.isOnline) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Warning.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = Warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Mode hors-ligne",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    "Patients, Glycémie, Médicaments et RDV restent accessibles",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9E6600)
                                )
                            }
                        }
                    }
                }
            }

            // ── Cartes de statistiques ─────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GradientStatCard(
                        value = uiState.totalPatients.toString(),
                        label = "Patients",
                        icon = Icons.Outlined.People,
                        gradientColors = listOf(Color(0xFF00D2FF), Color(0xFF0B8FAC)),
                        modifier = Modifier.weight(1f)
                    )
                    GradientStatCard(
                        value = if (uiState.avgGlucose > 0) "${uiState.avgGlucose.toInt()}" else "-",
                        label = "Moy. mg/dL",
                        icon = Icons.Outlined.MonitorHeart,
                        gradientColors = listOf(Color(0xFF26A69A), Color(0xFF00897B)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GradientStatCard(
                        value = uiState.todayRendezVous.toString(),
                        label = "RDV aujourd'hui",
                        icon = Icons.Outlined.CalendarMonth,
                        gradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF5C6BC0)),
                        modifier = Modifier.weight(1f)
                    )
                    GradientStatCard(
                        value = uiState.upcomingMedicaments.toString(),
                        label = "Rappels",
                        icon = Icons.Outlined.Medication,
                        gradientColors = listOf(Color(0xFFFF9800), Color(0xFFF57C00)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Accès rapide IA + Messagerie ──────────────────
            // ROLLY uniquement pour les patients
            if (uiState.userRole == UserRole.PATIENT) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RollyActionCard(
                            isOnline = uiState.isOnline,
                            onClick = { if (uiState.isOnline) onNavigateToChatbot() },
                            modifier = Modifier.weight(1f)
                        )
                        GradientActionCard(
                            title = "Messagerie",
                            subtitle = if (uiState.isOnline) "Patient-Médecin" else "Hors-ligne",
                            icon = Icons.Default.Forum,
                            gradientColors = if (uiState.isOnline)
                                listOf(Color(0xFF7C4DFF), Color(0xFF5C6BC0))
                            else
                                listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E)),
                            onClick = { if (uiState.isOnline) onNavigateToMessagerie() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Analyse Repas (patient uniquement)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientActionCard(
                            title = "Analyse Repas",
                            subtitle = if (uiState.isOnline) "Glucides & IG" else "Hors-ligne",
                            icon = Icons.Default.Restaurant,
                            gradientColors = if (uiState.isOnline)
                                listOf(Color(0xFF26A69A), Color(0xFF00897B))
                            else
                                listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E)),
                            onClick = { if (uiState.isOnline) onNavigateToRepasAnalyse() },
                            modifier = Modifier.weight(1f)
                        )
                        GradientActionCard(
                            title = "Partager",
                            subtitle = "Données médecin",
                            icon = Icons.Default.Share,
                            gradientColors = listOf(Color(0xFFFF6F00), Color(0xFFE65100)),
                            onClick = onNavigateToDataSharing,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Journal + Podomètre (patient uniquement)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientActionCard(
                            title = "Carnet de bord",
                            subtitle = "Humeur, stress, sommeil",
                            icon = Icons.Default.MenuBook,
                            gradientColors = listOf(Color(0xFF8E24AA), Color(0xFF6A1B9A)),
                            onClick = onNavigateToJournal,
                            modifier = Modifier.weight(1f)
                        )
                        GradientActionCard(
                            title = "Podomètre",
                            subtitle = "Compteur de pas",
                            icon = Icons.Default.DirectionsWalk,
                            gradientColors = listOf(Color(0xFF43A047), Color(0xFF2E7D32)),
                            onClick = onNavigateToPedometer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Courbes prédictives + Communauté (patient uniquement)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientActionCard(
                            title = "Courbes prédictives",
                            subtitle = "Prédiction glycémie",
                            icon = Icons.Default.TrendingUp,
                            gradientColors = listOf(Color(0xFFFF6F00), Color(0xFFE65100)),
                            onClick = onNavigateToPredictive,
                            modifier = Modifier.weight(1f)
                        )
                        GradientActionCard(
                            title = "Communauté",
                            subtitle = "Échanges patients",
                            icon = Icons.Default.Groups,
                            gradientColors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)),
                            onClick = onNavigateToCommunity,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Vue Médecin : accès données patients ─────────
            if (uiState.userRole == UserRole.MEDECIN) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientActionCard(
                            title = "Données patients",
                            subtitle = "Consulter les données glycémiques",
                            icon = Icons.Default.Assessment,
                            gradientColors = listOf(Color(0xFF26A69A), Color(0xFF00897B)),
                            onClick = onNavigateToPatients,
                            modifier = Modifier.weight(1f)
                        )
                        GradientActionCard(
                            title = "Messagerie",
                            subtitle = if (uiState.isOnline) "Patient-Médecin" else "Hors-ligne",
                            icon = Icons.Default.Forum,
                            gradientColors = if (uiState.isOnline)
                                listOf(Color(0xFF7C4DFF), Color(0xFF5C6BC0))
                            else
                                listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E)),
                            onClick = { if (uiState.isOnline) onNavigateToMessagerie() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Prochains rendez-vous ─────────────────────────
            item {
                ModernCardSection(
                    title = "Prochains rendez-vous",
                    action = "Voir tout" to onNavigateToRendezVous
                ) {
                    if (uiState.upcomingRendezVous.isEmpty()) {
                        EmptyStateMessage("Aucun rendez-vous à venir")
                    } else {
                        uiState.upcomingRendezVous.take(3).forEach { rdv ->
                            ModernRendezVousItem(
                                rdv = rdv,
                                onClick = { onNavigateToPatientDetail(rdv.patient.id) }
                            )
                        }
                    }
                }
            }

            // ── Patients récents ──────────────────────────────
            item {
                ModernCardSection(
                    title = "Patients récents",
                    action = "Voir tout" to onNavigateToPatients
                ) {
                    if (uiState.recentPatients.isEmpty()) {
                        EmptyStateMessage("Aucun patient enregistré")
                    } else {
                        uiState.recentPatients.forEach { patient ->
                            ModernPatientItem(
                                name = patient.nomComplet,
                                subtitle = "${patient.age} ans • ${patient.typeDiabete.name}",
                                onClick = { onNavigateToPatientDetail(patient.id) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  Composants réutilisables
// ══════════════════════════════════════════════════════════════════

/**
 * Bottom Navigation Bar avec design moderne
 */
@Composable
private fun DiaSmartBottomBar(
    selectedIndex: Int,
    onDashboard: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToRendezVous: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToMessagerie: () -> Unit,
    isMedecin: Boolean = false
) {
    NavigationBar(
        containerColor = NavBarBackground,
        tonalElevation = 8.dp,
        modifier = Modifier
            .shadow(12.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        // Onglets standards (sans ROLLY)
        data class NavItem(val label: String, val outlined: ImageVector, val filled: ImageVector, val isRolly: Boolean = false)

        val items = if (isMedecin) {
            listOf(
                NavItem("Accueil", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
                NavItem("Patients", Icons.Outlined.People, Icons.Filled.People),
                NavItem("RDV", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
                NavItem("Données", Icons.Outlined.Assessment, Icons.Filled.Assessment),
                NavItem("Messages", Icons.Outlined.Forum, Icons.Filled.Forum)
            )
        } else {
            listOf(
                NavItem("Accueil", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
                NavItem("Patients", Icons.Outlined.People, Icons.Filled.People),
                NavItem("RDV", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
                NavItem("ROLLY", Icons.Outlined.Dashboard, Icons.Filled.Dashboard, isRolly = true),
                NavItem("Messages", Icons.Outlined.Forum, Icons.Filled.Forum)
            )
        }
        val actions = if (isMedecin) {
            listOf(onDashboard, onNavigateToPatients, onNavigateToRendezVous, onNavigateToPatients, onNavigateToMessagerie)
        } else {
            listOf(onDashboard, onNavigateToPatients, onNavigateToRendezVous, onNavigateToChatbot, onNavigateToMessagerie)
        }

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = actions[index],
                icon = {
                    if (item.isRolly) {
                        // Icône ROLLY personnalisée
                        RollyIconInline(
                            size = 26.dp,
                            tint = if (selectedIndex == index) NavBarSelected else NavBarUnselected
                        )
                    } else {
                        Icon(
                            imageVector = if (selectedIndex == index) item.filled else item.outlined,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NavBarSelected,
                    selectedTextColor = NavBarSelected,
                    unselectedIconColor = NavBarUnselected,
                    unselectedTextColor = NavBarUnselected,
                    indicatorColor = NavBarSelected.copy(alpha = 0.12f)
                )
            )
        }
    }
}

/**
 * Carte de statistique avec gradient
 */
@Composable
private fun GradientStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(95.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(colors = gradientColors)
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = value,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Carte d'action rapide avec gradient
 */
@Composable
private fun GradientActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(colors = gradientColors)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Section carte moderne
 */
@Composable
private fun ModernCardSection(
    title: String,
    action: Pair<String, () -> Unit>? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                action?.let { (text, onClick) ->
                    TextButton(onClick = onClick) {
                        Text(text, color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = Primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ModernRendezVousItem(
    rdv: RendezVousAvecPatient,
    onClick: () -> Unit
) {
    val isToday = rdv.rendezVous.estAujourdhui()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isToday) Warning.copy(alpha = 0.08f) else Color(0xFFF8F9FA)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isToday) Warning.copy(alpha = 0.15f) else Primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isToday) Warning else Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rdv.patient.nomComplet,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(rdv.rendezVous.titre, fontSize = 12.sp, color = OnSurfaceVariant)
                Text(
                    text = rdv.rendezVous.dateHeure.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    ),
                    fontSize = 11.sp,
                    color = OnSurfaceVariant
                )
            }
            if (rdv.rendezVous.estConfirme) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Confirmé",
                    tint = Success,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernPatientItem(
    name: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F9FA)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Primary.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.split(" ").filter { it.isNotBlank() }
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .take(2).joinToString(""),
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Carte spéciale ROLLY avec icône personnalisée
 */
@Composable
private fun RollyActionCard(
    isOnline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = if (isOnline)
                            listOf(Color(0xFF001A3C), Color(0xFF003366), Color(0xFF0B8FAC))
                        else
                            listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E))
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RollyIcon(
                    size = 46.dp,
                    showBackground = false,
                    tint = if (isOnline) Color(0xFF00D2FF) else Color(0xFFBDBDBD)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ROLLY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        if (isOnline) "Assistant IA" else "Hors-ligne",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = OnSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
