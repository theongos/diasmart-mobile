package com.diabeto.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabeto.data.model.RepasAnalyse
import com.diabeto.data.model.RepasDocument
import com.diabeto.ui.components.RollyIconInline
import com.diabeto.ui.theme.*
import com.diabeto.ui.viewmodel.RepasViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Dark theme colors (identiques au ChatbotScreen / ROLLY) ─────────────
private val DarkBg = Color(0xFF0D0D1A)
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkCard = Color(0xFF16213E)
private val DarkInput = Color(0xFF1E1E30)
private val DrawerBg = Color(0xFF111128)
private val AccentCyan = Color(0xFF00D2FF)
private val AccentPurple = Color(0xFF7C4DFF)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentOrange = Color(0xFFFF9800)
private val AccentRed = Color(0xFFFF5252)
private val DimWhite = Color.White.copy(alpha = 0.6f)
private val MutedWhite = Color.White.copy(alpha = 0.4f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepasAnalyseScreen(
    onNavigateBack: () -> Unit,
    viewModel: RepasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDrawer by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            snackbarHostState.showSnackbar("Repas enregistre avec succes !")
            viewModel.clearSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        // ── Main Scaffold ────────────────────────────────────────────────
        Scaffold(
            containerColor = DarkBg,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = AccentCyan.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Analyse Repas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    "ROLLY - Nutritionniste IA",
                                    fontSize = 11.sp,
                                    color = AccentCyan.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        // Hamburger menu pour ouvrir le drawer (comme ChatbotScreen)
                        IconButton(onClick = { showDrawer = true }) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        // Bouton retour
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = DimWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface,
                        titleContentColor = Color.White
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Zone de saisie
                DarkSaisieRepasCard(
                    description = uiState.descriptionRepas,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onAnalyser = viewModel::analyserRepas,
                    onAnalyserImage = viewModel::analyserRepasImage,
                    onImageCaptured = viewModel::setCapturedBitmap,
                    onClearImage = viewModel::clearImage,
                    capturedBitmap = uiState.capturedBitmap,
                    isImageMode = uiState.isImageMode,
                    isAnalysing = uiState.isAnalysing
                )

                // Chargement
                if (uiState.isAnalysing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AccentCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("ROLLY analyse votre repas...", color = AccentCyan)
                        }
                    }
                }

                // Resultats
                uiState.analyseResult?.let { analyse ->
                    DarkNomRepasEditableCard(
                        nomRepas = uiState.nomRepasEdite,
                        onNomChange = viewModel::onNomRepasChange
                    )
                    DarkResultatAnalyseCard(analyse = analyse)
                    DarkValidationMedicaleCard(
                        glucides = uiState.glucidesEdites,
                        onGlucidesChange = viewModel::onGlucidesChange,
                        indexGlycemique = uiState.indexGlycemiqueEdite,
                        onIndexGlycemiqueChange = viewModel::onIndexGlycemiqueChange,
                        calories = uiState.caloriesEditees,
                        onCaloriesChange = viewModel::onCaloriesChange,
                        proteines = uiState.proteinesEditees,
                        onProteinesChange = viewModel::onProteinesChange,
                        lipides = uiState.lipidesEdites,
                        onLipidesChange = viewModel::onLipidesChange,
                        fibres = uiState.fibresEditees,
                        onFibresChange = viewModel::onFibresChange,
                        glycemieAvant = uiState.glycemieAvant,
                        onGlycemieAvantChange = viewModel::onGlycemieAvantChange,
                        glycemieApres = uiState.glycemieApres,
                        onGlycemieApresChange = viewModel::onGlycemieApresChange
                    )
                    if (analyse.recommandations.isNotEmpty()) {
                        DarkRecommandationsCard(analyse = analyse)
                    }
                    Button(
                        onClick = viewModel::demanderConfirmation,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Confirmer et enregistrer", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // ── Drawer latéral historique (identique au SessionDrawer) ────────
        if (showDrawer) {
            AnalyseHistoriqueDrawer(
                historique = uiState.historique,
                isLoading = uiState.isLoadingHistorique,
                expandedRepasId = uiState.expandedRepasId,
                onToggleExpand = viewModel::toggleExpandRepas,
                onDelete = viewModel::supprimerRepas,
                onClose = { showDrawer = false }
            )
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────
    if (uiState.showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = viewModel::annulerConfirmation,
            containerColor = DarkSurface,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.85f),
            icon = { Icon(Icons.Default.Warning, null, tint = AccentOrange) },
            title = { Text("Validation medicale", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ces valeurs sont des estimations de l'intelligence artificielle (ROLLY).",
                        fontWeight = FontWeight.Medium,
                        color = AccentOrange
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text("Avant d'enregistrer, veuillez verifier que :", color = Color.White.copy(alpha = 0.85f))
                    Text("- Le nom du repas est correct", color = DimWhite)
                    Text("- Les glucides estimes vous semblent corrects", color = DimWhite)
                    Text("- L'index glycemique est coherent", color = DimWhite)
                    Text("- Vous avez corrige les valeurs si necessaire", color = DimWhite)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "L'analyse sera aussi enregistree dans l'historique ROLLY.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Ces donnees ne remplacent pas l'avis de votre medecin ou dieteticien.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedWhite
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmerEtSauvegarder,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) { Text("Je confirme les valeurs") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::annulerConfirmation) {
                    Text("Modifier d'abord", color = AccentCyan)
                }
            }
        )
    }

    if (uiState.showIntegrerGlycemieDialog) {
        AlertDialog(
            onDismissRequest = viewModel::refuserIntegrationGlycemie,
            containerColor = DarkSurface,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.85f),
            icon = { Icon(Icons.Default.ShowChart, null, tint = AccentCyan) },
            title = { Text("Integrer la glycemie ?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.glycemieAvantSauvegardee?.let {
                        Text(
                            "Avant repas : ${it.toInt()} mg/dL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AccentCyan
                        )
                    }
                    uiState.glycemieApresSauvegardee?.let {
                        Text(
                            "Apres repas : ${it.toInt()} mg/dL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AccentOrange
                        )
                    }
                    if (uiState.glycemieAvantSauvegardee != null && uiState.glycemieApresSauvegardee != null) {
                        val diff = uiState.glycemieApresSauvegardee!! - uiState.glycemieAvantSauvegardee!!
                        val sign = if (diff >= 0) "+" else ""
                        Text(
                            "Evolution : $sign${diff.toInt()} mg/dL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (diff > 40) AccentRed else if (diff > 0) AccentOrange else AccentGreen
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Ajouter ces valeurs dans votre courbe predictive ?", color = Color.White.copy(alpha = 0.85f))
                    Text(
                        "ROLLY pourra mieux predire les risques d'hypo/hyperglycemie et analyser votre metabolisme post-repas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedWhite
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::integrerGlycemiePrediction,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Icon(Icons.Default.AddChart, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Oui, integrer", color = DarkBg)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::refuserIntegrationGlycemie) {
                    Text("Non, pas maintenant", color = DimWhite)
                }
            }
        )
    }
}

// =============================================================================
//  DRAWER HISTORIQUE — Copie exacte du style SessionDrawer du ChatbotScreen
// =============================================================================

@Composable
private fun AnalyseHistoriqueDrawer(
    historique: List<RepasDocument>,
    isLoading: Boolean,
    expandedRepasId: String?,
    onToggleExpand: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    // Fond overlay noir semi-transparent (comme SessionDrawer)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClose)
    ) {
        // Panneau drawer a gauche — 280.dp comme SessionDrawer
        Surface(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .clickable(enabled = false) {},
            color = DrawerBg,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header (identique a SessionDrawer) ──────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🍽️", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Analyses",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Fermer", tint = DimWhite)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // ── Bouton "Nouvelle analyse" (comme "+ Nouvelle discussion") ──
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { onClose() },
                    shape = RoundedCornerShape(12.dp),
                    color = AccentCyan.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Nouvelle analyse",
                            color = AccentCyan,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Liste des analyses ──────────────────────────────────
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(32.dp))
                    }
                } else if (historique.isEmpty()) {
                    // Etat vide (comme "Aucune discussion")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Aucune analyse.",
                            color = MutedWhite,
                            fontSize = 13.sp
                        )
                        Text(
                            "Analysez votre premier repas !",
                            color = MutedWhite,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // Grouper par date (comme SessionDrawer)
                    val grouped = groupHistoriqueByDate(historique)

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        grouped.forEach { (category, repasInCategory) ->
                            // En-tete de categorie
                            item {
                                Text(
                                    text = category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MutedWhite,
                                    modifier = Modifier.padding(
                                        start = 8.dp,
                                        top = 12.dp,
                                        bottom = 4.dp
                                    )
                                )
                            }
                            // Items
                            items(repasInCategory, key = { it.id }) { repas ->
                                AnalyseDrawerItem(
                                    repas = repas,
                                    isExpanded = repas.id == expandedRepasId,
                                    onToggleExpand = { onToggleExpand(repas.id) },
                                    onDelete = { onDelete(repas.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grouper les repas par categorie de date (comme SessionDrawer)
 */
private fun groupHistoriqueByDate(historique: List<RepasDocument>): List<Pair<String, List<RepasDocument>>> {
    val now = Calendar.getInstance()
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val sevenDaysAgo = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -7)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    val groups = mutableMapOf<String, MutableList<RepasDocument>>()

    historique.forEach { repas ->
        val repasDate = Calendar.getInstance().apply { time = repas.timestamp.toDate() }
        val category = when {
            repasDate.after(today) || repasDate == today -> "Aujourd'hui"
            repasDate.after(yesterday) -> "Hier"
            repasDate.after(sevenDaysAgo) -> "7 derniers jours"
            else -> {
                val sdf = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)
                sdf.format(repas.timestamp.toDate()).replaceFirstChar { it.uppercase() }
            }
        }
        groups.getOrPut(category) { mutableListOf() }.add(repas)
    }

    // Ordre: Aujourd'hui > Hier > 7 derniers jours > mois
    val order = listOf("Aujourd'hui", "Hier", "7 derniers jours")
    return groups.entries.sortedWith(compareBy { entry ->
        val idx = order.indexOf(entry.key)
        if (idx >= 0) idx else order.size
    }).map { it.key to it.value }
}

/**
 * Item d'analyse dans le drawer — style identique a SessionItem
 */
@Composable
private fun AnalyseDrawerItem(
    repas: RepasDocument,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(10.dp),
        color = if (isExpanded) AccentCyan.copy(alpha = 0.1f) else Color.Transparent,
        border = if (isExpanded) BorderStroke(1.dp, AccentCyan.copy(alpha = 0.2f)) else null
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji (comme SessionItem "💬")
                Text("🍽️", fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))

                // Titre + preview
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        repas.nomRepas,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(repas.timestamp.toDate()),
                        fontSize = 11.sp,
                        color = MutedWhite,
                        maxLines = 1
                    )
                }

                // Bouton supprimer (comme SessionItem)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        modifier = Modifier.size(16.dp),
                        tint = AccentRed.copy(alpha = 0.6f)
                    )
                }
            }

            // Detail expandable (specifique aux analyses)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Score + macros rapides
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Score: ${repas.scoreDiabete}/100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = scoreColor(repas.scoreDiabete))
                        Text("${repas.caloriesEstimees} kcal", fontSize = 12.sp, color = AccentOrange)
                    }

                    // Nutriments
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MiniNutrient("Gluc.", "${repas.glucidesEstimes.toInt()}g", AccentCyan)
                        MiniNutrient("Prot.", "${repas.proteinesEstimees.toInt()}g", AccentPurple)
                        MiniNutrient("Lip.", "${repas.lipidesEstimes.toInt()}g", AccentOrange)
                        MiniNutrient("Fib.", "${repas.fibresEstimees.toInt()}g", AccentGreen)
                    }

                    // IG
                    Text(
                        "Index Glycemique: ${repas.indexGlycemique} (${repas.categorieIG})",
                        fontSize = 11.sp,
                        color = darkIgColor(repas.categorieIG)
                    )

                    // Glycemies
                    if (repas.glycemieAvantRepas != null || repas.glycemieApresRepas != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repas.glycemieAvantRepas?.let {
                                Text("Avant: ${it.toInt()} mg/dL", fontSize = 11.sp, color = AccentCyan)
                            }
                            repas.glycemieApresRepas?.let {
                                Text("Apres: ${it.toInt()} mg/dL", fontSize = 11.sp, color = AccentOrange)
                            }
                        }
                    }

                    // Impact
                    if (repas.impactGlycemique.isNotBlank()) {
                        Text(
                            repas.impactGlycemique,
                            fontSize = 10.sp,
                            color = DimWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Recommandations
                    if (repas.recommandations.isNotEmpty()) {
                        repas.recommandations.take(2).forEach { r ->
                            Text("- $r", fontSize = 10.sp, color = DimWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniNutrient(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
        Text(label, fontSize = 9.sp, color = MutedWhite)
    }
}

// =============================================================================
//  COMPOSANTS PRINCIPAUX — Dark theme
// =============================================================================

@Composable
private fun DarkNomRepasEditableCard(
    nomRepas: String,
    onNomChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restaurant, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nom du repas identifie", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentCyan)
            }
            OutlinedTextField(
                value = nomRepas,
                onValueChange = onNomChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Nom du repas", color = MutedWhite) },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = DimWhite) },
                supportingText = { Text("Modifiez si le nom ne correspond pas", fontSize = 11.sp, color = MutedWhite) },
                colors = darkFieldColors()
            )
        }
    }
}

@Composable
private fun DarkSaisieRepasCard(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onAnalyser: () -> Unit,
    onAnalyserImage: () -> Unit,
    onImageCaptured: (android.graphics.Bitmap) -> Unit,
    onClearImage: () -> Unit,
    capturedBitmap: android.graphics.Bitmap?,
    isImageMode: Boolean,
    isAnalysing: Boolean
) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val maxDim = 1024
                    val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
                    val resized = if (scale < 1f) {
                        android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                    } else bitmap
                    onImageCaptured(resized)
                }
            } catch (_: Exception) {}
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val maxDim = 1024
            val scale = minOf(maxDim.toFloat() / it.width, maxDim.toFloat() / it.height, 1f)
            val resized = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(it, (it.width * scale).toInt(), (it.height * scale).toInt(), true)
            } else it
            onImageCaptured(resized)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Analysez votre repas", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Text("Prenez une photo ou decrivez votre repas pour une analyse nutritionnelle IA.", style = MaterialTheme.typography.bodySmall, color = MutedWhite)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                    border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Photo", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple),
                    border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galerie", fontSize = 13.sp)
                }
            }

            if (capturedBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = capturedBitmap.asImageBitmap(),
                        contentDescription = "Photo du repas",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Retirer", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Button(
                    onClick = onAnalyserImage,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAnalysing,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analyser la photo avec ROLLY Vision", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (capturedBitmap == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                    Text("  ou decrivez  ", fontSize = 11.sp, color = MutedWhite)
                    HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    placeholder = { Text("Ex: Couscous avec legumes, poulet grille, et un verre de lben", fontSize = 13.sp, color = MutedWhite) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAnalyser() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = darkFieldColors()
                )
                Button(
                    onClick = onAnalyser,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = description.isNotBlank() && !isAnalysing,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    RollyIconInline(size = 18.dp, tint = DarkBg)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyser avec ROLLY", fontWeight = FontWeight.Bold, color = DarkBg)
                }
            }
        }
    }
}

@Composable
private fun DarkResultatAnalyseCard(analyse: RepasAnalyse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(analyse.nomRepas, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text(analyse.description, style = MaterialTheme.typography.bodySmall, color = MutedWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                DarkScoreBadge(analyse.scoreDiabete)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DarkNutrientChip("Glucides", "${analyse.glucidesEstimes.toInt()}g", AccentCyan)
                DarkNutrientChip("Proteines", "${analyse.proteinesEstimees.toInt()}g", AccentPurple)
                DarkNutrientChip("Lipides", "${analyse.lipidesEstimes.toInt()}g", AccentOrange)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DarkNutrientChip("Calories", "${analyse.caloriesEstimees}", AccentOrange)
                DarkNutrientChip("IG", "${analyse.indexGlycemique}", darkIgColor(analyse.categorieIG))
                DarkNutrientChip("Fibres", "${analyse.fibresEstimees.toInt()}g", AccentGreen)
            }
            // Impact glycemique
            Card(
                colors = CardDefaults.cardColors(containerColor = darkIgColor(analyse.categorieIG).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = darkIgColor(analyse.categorieIG), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Index Glycemique : ${analyse.categorieIG.uppercase()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = darkIgColor(analyse.categorieIG))
                        Text(analyse.impactGlycemique, style = MaterialTheme.typography.bodySmall, color = DimWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkValidationMedicaleCard(
    glucides: String, onGlucidesChange: (String) -> Unit,
    indexGlycemique: String, onIndexGlycemiqueChange: (String) -> Unit,
    calories: String, onCaloriesChange: (String) -> Unit,
    proteines: String, onProteinesChange: (String) -> Unit,
    lipides: String, onLipidesChange: (String) -> Unit,
    fibres: String, onFibresChange: (String) -> Unit,
    glycemieAvant: String, onGlycemieAvantChange: (String) -> Unit,
    glycemieApres: String, onGlycemieApresChange: (String) -> Unit
) {
    val fc = darkFieldColors()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Verification & correction", fontWeight = FontWeight.Bold, color = AccentOrange)
            }
            Text("Verifiez et corrigez les valeurs estimees par ROLLY.", style = MaterialTheme.typography.bodySmall, color = MutedWhite)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = glucides, onValueChange = onGlucidesChange, label = { Text("Glucides (g)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fc)
                OutlinedTextField(value = proteines, onValueChange = onProteinesChange, label = { Text("Proteines (g)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fc)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = lipides, onValueChange = onLipidesChange, label = { Text("Lipides (g)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fc)
                OutlinedTextField(value = fibres, onValueChange = onFibresChange, label = { Text("Fibres (g)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fc)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = calories, onValueChange = onCaloriesChange, label = { Text("Calories (kcal)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = fc)
                OutlinedTextField(value = indexGlycemique, onValueChange = onIndexGlycemiqueChange, label = { Text("Index Glycemique") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = fc)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Text("Glycemie (optionnel - pour la courbe de prediction)", color = AccentCyan, fontWeight = FontWeight.Medium, fontSize = 13.sp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = glycemieAvant, onValueChange = onGlycemieAvantChange, label = { Text("Avant repas (mg/dL)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fc)
                OutlinedTextField(value = glycemieApres, onValueChange = onGlycemieApresChange, label = { Text("Apres repas (mg/dL)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fc)
            }
            Text("Si vous ajoutez une glycemie, ROLLY vous proposera de l'integrer dans votre courbe de prediction.", style = MaterialTheme.typography.bodySmall, color = MutedWhite, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DarkRecommandationsCard(analyse: RepasAnalyse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recommandations ROLLY", fontWeight = FontWeight.Bold, color = Color.White)
            analyse.recommandations.forEach { r ->
                Row {
                    Text("- ", color = AccentCyan, fontWeight = FontWeight.Bold)
                    Text(r, style = MaterialTheme.typography.bodySmall, color = DimWhite)
                }
            }
            if (analyse.alternativesSaines.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                Text("Alternatives plus saines", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 13.sp)
                analyse.alternativesSaines.forEach { a ->
                    Row {
                        Text("-> ", color = AccentGreen, fontWeight = FontWeight.Bold)
                        Text(a, style = MaterialTheme.typography.bodySmall, color = DimWhite)
                    }
                }
            }
        }
    }
}

// =============================================================================
//  Utilitaires
// =============================================================================

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    cursorColor = AccentCyan,
    focusedContainerColor = DarkInput,
    unfocusedContainerColor = DarkInput,
    focusedLabelColor = AccentCyan,
    unfocusedLabelColor = DimWhite
)

@Composable
private fun DarkNutrientChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 15.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedWhite)
    }
}

@Composable
private fun DarkScoreBadge(score: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = scoreColor(score).copy(alpha = 0.15f)
    ) {
        Text(
            "$score/100",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = scoreColor(score)
        )
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 70 -> AccentGreen
    score >= 40 -> AccentOrange
    else -> AccentRed
}

private fun darkIgColor(categorie: String): Color = when (categorie.lowercase()) {
    "bas" -> AccentGreen
    "moyen" -> AccentOrange
    "eleve" -> AccentRed
    else -> DimWhite
}
