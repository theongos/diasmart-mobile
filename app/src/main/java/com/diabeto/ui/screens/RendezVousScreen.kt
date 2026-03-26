package com.diabeto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diabeto.data.entity.RendezVousAvecPatient
import com.diabeto.data.entity.TypeRendezVous
import com.diabeto.ui.theme.*
import com.diabeto.ui.viewmodel.PatientOption
import com.diabeto.ui.viewmodel.RendezVousFilter
import com.diabeto.ui.viewmodel.RendezVousViewModel
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RendezVousScreen(
    patientId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToAdd: (Long?) -> Unit,
    viewModel: RendezVousViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.addSuccess) {
        if (uiState.addSuccess) {
            snackbarHostState.showSnackbar("Rendez-vous ajouté avec succès")
            viewModel.clearAddSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rendez-vous") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAdd(patientId) },
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Filtres
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == RendezVousFilter.TOUS,
                    onClick = { viewModel.setFilter(RendezVousFilter.TOUS) },
                    label = { Text("Tous") }
                )
                FilterChip(
                    selected = uiState.filter == RendezVousFilter.A_VENIR,
                    onClick = { viewModel.setFilter(RendezVousFilter.A_VENIR) },
                    label = { Text("À venir") }
                )
                FilterChip(
                    selected = uiState.filter == RendezVousFilter.PASSES,
                    onClick = { viewModel.setFilter(RendezVousFilter.PASSES) },
                    label = { Text("Passés") }
                )
            }
            
            // Liste
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = viewModel.getFilteredRendezVous(),
                    key = { it.rendezVous.id }
                ) { rdv ->
                    RendezVousCard(
                        rdv = rdv,
                        showPatient = patientId == null,
                        onToggleConfirm = { viewModel.toggleConfirmation(rdv.rendezVous) },
                        onDelete = { viewModel.deleteRendezVous(rdv.rendezVous) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun RendezVousCard(
    rdv: RendezVousAvecPatient,
    showPatient: Boolean,
    onToggleConfirm: () -> Unit,
    onDelete: () -> Unit
) {
    val isPast = rdv.rendezVous.estPasse()
    val urgencyColor = when (rdv.rendezVous.getUrgenceLevel()) {
        3 -> Error
        2 -> Warning
        else -> Success
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPast) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPast -> SurfaceVariant.copy(alpha = 0.5f)
                rdv.rendezVous.estConfirme -> Success.copy(alpha = 0.1f)
                else -> Surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Patient (si liste globale)
            if (showPatient) {
                Text(
                    text = rdv.patient.nomComplet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rdv.rendezVous.titre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (rdv.rendezVous.estAujourdhui()) urgencyColor else OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = rdv.rendezVous.dateHeure.format(
                                DateTimeFormatter.ofPattern("EEEE dd MMMM à HH:mm")
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (rdv.rendezVous.estAujourdhui()) urgencyColor else OnSurface
                        )
                    }
                    
                    if (rdv.rendezVous.lieu.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = rdv.rendezVous.lieu,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = rdv.rendezVous.type.getDisplayName(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${rdv.rendezVous.dureeMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                
                // Actions
                if (!isPast) {
                    Row {
                        IconButton(onClick = onToggleConfirm) {
                            Icon(
                                imageVector = if (rdv.rendezVous.estConfirme) 
                                    Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (rdv.rendezVous.estConfirme) "Confirmé" else "Non confirmé",
                                tint = if (rdv.rendezVous.estConfirme) Success else OnSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = Error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RendezVousEditScreen(
    rdvId: Long?,
    patientId: Long?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: RendezVousViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val dateDialogState = rememberMaterialDialogState()
    val timeDialogState = rememberMaterialDialogState()
    
    LaunchedEffect(addState.error) {
        addState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.addSuccess) {
        if (uiState.addSuccess) {
            viewModel.clearAddSuccess()
            onSaveSuccess()
        }
    }

    // Dialogs
    MaterialDialog(
        dialogState = dateDialogState,
        buttons = {
            positiveButton("OK")
            negativeButton("Annuler")
        }
    ) {
        datepicker(
            initialDate = addState.date,
            title = "Date du rendez-vous"
        ) { date ->
            viewModel.updateAddField("date", date)
        }
    }
    
    MaterialDialog(
        dialogState = timeDialogState,
        buttons = {
            positiveButton("OK")
            negativeButton("Annuler")
        }
    ) {
        timepicker(
            initialTime = addState.heure,
            title = "Heure du rendez-vous"
        ) { time ->
            viewModel.updateAddField("heure", time)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (rdvId == null) "Nouveau rendez-vous" else "Modifier rendez-vous") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient (si non prédéfini)
            if (patientId == null) {
                // Liste déroulante des patients (Room + Firestore acceptés)
                var expanded by remember { mutableStateOf(false) }
                val selectedOption = uiState.patientOptions.find { it.id == addState.selectedPatientId }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedOption?.nom ?: "",
                        onValueChange = { },
                        label = { Text("Patient *") },
                        placeholder = {
                            Text(
                                if (uiState.patientOptions.isEmpty())
                                    "Aucun patient disponible"
                                else
                                    "Selectionner un patient"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.patientOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(option.nom)
                                        if (option.isFirestore) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "en ligne",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.updateAddField("patientId", option.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            OutlinedTextField(
                value = addState.titre,
                onValueChange = { viewModel.updateAddField("titre", it) },
                label = { Text("Titre *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = addState.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    onValueChange = { },
                    label = { Text("Date *") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { dateDialogState.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    }
                )
                
                OutlinedTextField(
                    value = addState.heure.format(DateTimeFormatter.ofPattern("HH:mm")),
                    onValueChange = { },
                    label = { Text("Heure *") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { timeDialogState.show() }) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        }
                    }
                )
            }
            
            // Type
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = addState.type.getDisplayName(),
                    onValueChange = { },
                    label = { Text("Type") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    TypeRendezVous.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.getDisplayName()) },
                            onClick = {
                                viewModel.updateAddField("type", type)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = addState.duree.toString(),
                onValueChange = { viewModel.updateAddField("duree", it.toIntOrNull() ?: 30) },
                label = { Text("Durée (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = addState.lieu,
                onValueChange = { viewModel.updateAddField("lieu", it) },
                label = { Text("Lieu") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = addState.notes,
                onValueChange = { viewModel.updateAddField("notes", it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Annuler")
                }
                Button(
                    onClick = { viewModel.addRendezVous() },
                    modifier = Modifier.weight(1f),
                    enabled = addState.titre.isNotBlank() && addState.selectedPatientId > 0L
                ) {
                    Text("Sauvegarder")
                }
            }
        }
    }
}
