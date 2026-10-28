package com.cmo.hospital.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmo.hospital.data.model.*
import com.cmo.hospital.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorMainScreen(
    viewModel: DoctorViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val consulta by viewModel.consultaActiva.collectAsState()
    val ordenesLab by viewModel.ordenesLab.collectAsState()
    val expedientes by viewModel.expedientes.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var mostrarDialogoAgendarCita by remember { mutableStateOf(false) }
    val citasGlobales by viewModel.citas.collectAsState()

    LaunchedEffect(uiState.userMessage, uiState.errorMessage) {
        val msg = uiState.userMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CmoPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👨‍⚕️", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hospital CMO · Portal Médico",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CmoSurface
                            )
                            Text(
                                text = "${uiState.currentUser?.nombreCompleto ?: "Dr. Alejandro Restrepo"} · ${uiState.currentUser?.cargo ?: "Medicina Interna"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CmoTextMuted
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoAgendarCita = true }) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Agendar Cita de Control",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CmoBackgroundDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = CmoSurface, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                    label = { Text("Consulta Activa", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CmoPrimary,
                        selectedTextColor = CmoPrimary,
                        indicatorColor = CmoPrimarySoft
                    )
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.FolderShared, contentDescription = null) },
                    label = { Text("Expedientes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CmoPrimary,
                        selectedTextColor = CmoPrimary,
                        indicatorColor = CmoPrimarySoft
                    )
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.Science, contentDescription = null) },
                    label = { Text("Laboratorio LIS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CmoPrimary,
                        selectedTextColor = CmoPrimary,
                        indicatorColor = CmoPrimarySoft
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CmoBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> DoctorConsultaTab(
                    consulta = consulta,
                    isCronometroCorriendo = uiState.isCronometroCorriendo,
                    onToggleCronometro = { viewModel.toggleCronometro() },
                    onActualizarSignos = { viewModel.actualizarSignosVitales(it) },
                    onAgregarCie10 = { cod, desc -> viewModel.agregarCie10(cod, desc) },
                    onEliminarCie10 = { viewModel.eliminarCie10(it) },
                    onGuardarAnamnesis = { viewModel.guardarAnamnesis(it) },
                    onAgregarReceta = { viewModel.agregarReceta(it) },
                    onEliminarReceta = { viewModel.eliminarReceta(it) },
                    onEmitirLab = { examenes, prio, obs, pac, st ->
                        viewModel.emitirOrdenLab(examenes, prio, obs, pac, st)
                    },
                    onCambiarEstadoLab = { id, st -> viewModel.cambiarEstadoLaboratorio(id, st) },
                    onFinalizar = { viewModel.finalizarConsulta {} },
                    onIrALab = { viewModel.setTab(2) },
                    onAbrirAgendarCita = { mostrarDialogoAgendarCita = true }
                )
                1 -> DoctorExpedientesTab(
                    expedientes = expedientes,
                    onEmitirLab = { examenes, prio, obs, pac, st ->
                        viewModel.emitirOrdenLab(examenes, prio, obs, pac, st)
                    },
                    onCambiarEstadoLab = { id, st -> viewModel.cambiarEstadoLaboratorio(id, st) }
                )
                2 -> DoctorLaboratorioTab(
                    ordenes = ordenesLab,
                    pacientesDisponibles = expedientes.map { it.paciente },
                    onEmitir = { examenes, prio, obs, pac, st ->
                        viewModel.emitirOrdenLab(examenes, prio, obs, pac, st)
                    },
                    onCambiarEstado = { id, st -> viewModel.cambiarEstadoLaboratorio(id, st) }
                )
            }
        }
    }

    if (mostrarDialogoAgendarCita) {
        DialogoAgendarCitaDoctor(
            pacienteDefault = consulta.paciente,
            expedientesDisponibles = expedientes,
            doctorNombre = uiState.currentUser?.nombreCompleto ?: "Dr. Alejandro Restrepo",
            especialidadDoctor = uiState.currentUser?.cargo ?: "Medicina Interna",
            citasExistentes = citasGlobales,
            onDismiss = { mostrarDialogoAgendarCita = false },
            onConfirmar = { pac, fecha, hora, motivo, esp ->
                viewModel.agendarCitaSeguimiento(pac, fecha, hora, motivo, esp) {
                    mostrarDialogoAgendarCita = false
                }
            }
        )
    }
}

// =================================================================
// TAB 1: CONSULTA MÉDICA ACTIVA CON SEMÁFORO (30 MINUTOS)
// =================================================================
@Composable
fun DoctorConsultaTab(
    consulta: com.cmo.hospital.repository.DoctorClinicalRepository.ConsultaActivaData,
    isCronometroCorriendo: Boolean,
    onToggleCronometro: () -> Unit,
    onActualizarSignos: (SignosVitales) -> Unit,
    onAgregarCie10: (String, String) -> Unit,
    onEliminarCie10: (String) -> Unit,
    onGuardarAnamnesis: (String) -> Unit,
    onAgregarReceta: (RecetaItem) -> Unit,
    onEliminarReceta: (String) -> Unit,
    onEmitirLab: (List<String>, String, String, Paciente?, String) -> Unit,
    onCambiarEstadoLab: (String, String) -> Unit,
    onFinalizar: () -> Unit,
    onIrALab: () -> Unit,
    onAbrirAgendarCita: () -> Unit
) {
    var mostrarDialogoVitals by remember { mutableStateOf(false) }
    var mostrarDialogoCie10 by remember { mutableStateOf(false) }
    var mostrarDialogoReceta by remember { mutableStateOf(false) }
    var mostrarDialogoLabConsulta by remember { mutableStateOf(false) }

    var anamnesisText by remember(consulta.anamnesis) { mutableStateOf(consulta.anamnesis) }

    val min = consulta.segundosTranscurridos / 60
    val seg = consulta.segundosTranscurridos % 60
    val tiempoFormateado = String.format(java.util.Locale.US, "%02d:%02d", min, seg)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Banner Cronómetro y Semáforo de Consulta (30 minutos)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (consulta.estadoSemaforo) {
                        EstadoSemaforo.VERDE -> StatusGreenBg
                        EstadoSemaforo.AMBAR -> StatusAmberBg
                        EstadoSemaforo.ROJO -> StatusRedBg
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    when (consulta.estadoSemaforo) {
                        EstadoSemaforo.VERDE -> StatusGreen
                        EstadoSemaforo.AMBAR -> StatusAmber
                        EstadoSemaforo.ROJO -> StatusRed
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = when (consulta.estadoSemaforo) {
                                    EstadoSemaforo.VERDE -> StatusGreen
                                    EstadoSemaforo.AMBAR -> StatusAmber
                                    EstadoSemaforo.ROJO -> StatusRed
                                },
                                modifier = Modifier.size(12.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (consulta.estadoSemaforo) {
                                    EstadoSemaforo.VERDE -> "🟢 EN TIEMPO ÓPTIMO"
                                    EstadoSemaforo.AMBAR -> "🟡 PRÓXIMO A CONCLUIR (5 MIN)"
                                    EstadoSemaforo.ROJO -> "🔴 TIEMPO AGOTADO (30 MIN)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (consulta.estadoSemaforo) {
                                    EstadoSemaforo.VERDE -> StatusGreen
                                    EstadoSemaforo.AMBAR -> StatusAmber
                                    EstadoSemaforo.ROJO -> StatusRed
                                }
                            )
                        }

                        // Pausa / Reanudar cronómetro
                        IconButton(onClick = onToggleCronometro, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (isCronometroCorriendo) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Pausa",
                                tint = CmoText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = tiempoFormateado,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = CmoText
                            )
                            Text(
                                text = "Bloque de 30 min · ${consulta.minutosRestantes}m restantes",
                                fontSize = 11.sp,
                                color = CmoTextSoft
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CmoSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = consulta.box,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CmoPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Ficha del Paciente en Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = consulta.paciente.nombreCompleto,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CmoText
                            )
                            Text(
                                text = "Edad: ${consulta.paciente.edad} años (${consulta.paciente.fechaNacimiento ?: "N/D"}) · DNI: ${consulta.paciente.dui ?: "N/D"}",
                                fontSize = 11.sp,
                                color = CmoTextSoft
                            )
                            Text(
                                text = "Expediente: ${consulta.paciente.numeroExpediente ?: "N/D"}",
                                fontSize = 11.sp,
                                color = CmoPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CmoPrimarySoft
                        ) {
                            Text(
                                text = consulta.paciente.prevision ?: "Fonasa",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CmoPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Alergias
                    consulta.paciente.alergias?.let { alergias ->
                        if (alergias.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusRedBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alergias: $alergias", fontSize = 11.sp, color = StatusRed, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Triaje y Signos Vitales (con botón para que el Doctor los tome directamente)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Signos Vitales del Triaje",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CmoText
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (!consulta.signosVitales.hasAnyData) {
                                Surface(shape = RoundedCornerShape(4.dp), color = StatusAmberBg) {
                                    Text("Pase de Emergencia", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusAmber, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }

                        TextButton(onClick = { mostrarDialogoVitals = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tomar / Ajustar", fontSize = 11.sp, color = CmoPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VitalsChip(
                            label = "PA",
                            value = consulta.signosVitales.presionTexto,
                            unit = "",
                            color = CmoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        VitalsChip(
                            label = "PULSO",
                            value = "${consulta.signosVitales.frecuenciaCardiaca ?: "--"}",
                            unit = "lpm",
                            color = StatusGreen,
                            modifier = Modifier.weight(1f)
                        )
                        VitalsChip(
                            label = "TEMP",
                            value = "${consulta.signosVitales.temperaturaC ?: "--"}",
                            unit = "°C",
                            color = StatusAmber,
                            modifier = Modifier.weight(1f)
                        )
                        VitalsChip(
                            label = "SPO2",
                            value = "${consulta.signosVitales.saturacionOxigeno ?: "--"}",
                            unit = "%",
                            color = CmoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VitalsChip(
                            label = "PESO",
                            value = "${consulta.signosVitales.pesoKg ?: "--"}",
                            unit = "kg",
                            color = CmoText,
                            modifier = Modifier.weight(1f)
                        )
                        VitalsChip(
                            label = "TALLA",
                            value = "${consulta.signosVitales.tallaCm ?: "--"}",
                            unit = "cm",
                            color = CmoText,
                            modifier = Modifier.weight(1f)
                        )
                        VitalsChip(
                            label = "IMC",
                            value = "${consulta.signosVitales.imc ?: "--"}",
                            unit = consulta.signosVitales.imcClasificacion,
                            color = CmoPrimaryDark,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }

        // Diagnóstico CIE-10
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hipótesis Diagnóstica (CIE-10)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                        TextButton(onClick = { mostrarDialogoCie10 = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir CIE-10", fontSize = 11.sp, color = CmoPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (consulta.diagnosticosCie10.isEmpty()) {
                        Text("No se han agregado diagnósticos CIE-10.", fontSize = 12.sp, color = CmoTextSoft)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(consulta.diagnosticosCie10) { cie ->
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = CmoSurfaceContainerLow,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CmoBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cie.codigo, fontWeight = FontWeight.Bold, color = CmoPrimary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cie.descripcion, fontSize = 11.sp, color = CmoText, maxLines = 1)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = StatusRed,
                                            modifier = Modifier.size(14.dp).clickable { onEliminarCie10(cie.codigo) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Evolución Médica & Anamnesis
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Evolución Médica & Anamnesis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                        Surface(shape = RoundedCornerShape(99.dp), color = CmoPrimarySoft) {
                            Text("Autoguardado", fontSize = 10.sp, color = CmoPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = anamnesisText,
                        onValueChange = {
                            anamnesisText = it
                            onGuardarAnamnesis(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        placeholder = { Text("Registrar motivo de consulta, examen físico, hallazgos...") }
                    )
                }
            }
        }

        // Recetario Digital
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Prescripciones Farmacológicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                        TextButton(onClick = { mostrarDialogoReceta = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prescribir", fontSize = 11.sp, color = CmoPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (consulta.recetas.isEmpty()) {
                        Text("No hay medicamentos prescritos en esta consulta.", fontSize = 12.sp, color = CmoTextSoft)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            consulta.recetas.forEach { rec ->
                                Surface(shape = RoundedCornerShape(10.dp), color = CmoSurfaceContainerLow) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(rec.medicamento, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CmoText)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = CmoPrimarySoft) {
                                                    Text(rec.tipo, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CmoPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text("${rec.dosis} · ${rec.frecuencia} · ${rec.duracion}", fontSize = 11.sp, color = CmoTextMuted)
                                            rec.indicaciones?.let { Text(it, fontSize = 10.sp, color = CmoTextSoft) }
                                        }
                                        IconButton(onClick = { onEliminarReceta(rec.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Exámenes de Laboratorio Indicados en Consulta
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Exámenes de Laboratorio Indicados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                            Text("Registrados en expediente del paciente y LIS", fontSize = 11.sp, color = CmoTextSoft)
                        }
                        TextButton(onClick = { mostrarDialogoLabConsulta = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Indicar Examen", fontSize = 11.sp, color = CmoPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (consulta.ordenesLaboratorio.isEmpty()) {
                        Text("No se han indicado exámenes de laboratorio para esta consulta.", fontSize = 12.sp, color = CmoTextSoft)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            consulta.ordenesLaboratorio.forEach { ord ->
                                TarjetaOrdenLaboratorioDoctor(
                                    orden = ord,
                                    onCambiarEstado = { nuevoEstado -> onCambiarEstadoLab(ord.id, nuevoEstado) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Acciones Finales de Consulta
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAbrirAgendarCita,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp), tint = CmoPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Agendar Cita de Control / Seguimiento", fontWeight = FontWeight.SemiBold, color = CmoPrimary)
                }

                OutlinedButton(
                    onClick = onIrALab,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ir al Módulo de Laboratorio / LIS", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onFinalizar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CmoPrimary)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FINALIZAR CONSULTA Y FIRMAR RECETA", fontWeight = FontWeight.Bold)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Diálogo editar signos
    if (mostrarDialogoVitals) {
        DialogoEditarSignos(
            signosActuales = consulta.signosVitales,
            onDismiss = { mostrarDialogoVitals = false },
            onGuardar = {
                onActualizarSignos(it)
                mostrarDialogoVitals = false
            }
        )
    }

    // Diálogo añadir CIE-10
    if (mostrarDialogoCie10) {
        DialogoAgregarCie10(
            onDismiss = { mostrarDialogoCie10 = false },
            onAgregar = { cod, desc ->
                onAgregarCie10(cod, desc)
                mostrarDialogoCie10 = false
            }
        )
    }

    // Diálogo añadir Receta
    if (mostrarDialogoReceta) {
        DialogoAgregarReceta(
            onDismiss = { mostrarDialogoReceta = false },
            onAgregar = {
                onAgregarReceta(it)
                mostrarDialogoReceta = false
            }
        )
    }

    // Diálogo Indicar Examen de Laboratorio desde Consulta
    if (mostrarDialogoLabConsulta) {
        DialogoIndicarExamenesLaboratorio(
            paciente = consulta.paciente,
            onDismiss = { mostrarDialogoLabConsulta = false },
            onConfirmar = { examenes, prioridad, obs, estadoInicial ->
                onEmitirLab(examenes, prioridad, obs, consulta.paciente, estadoInicial)
                mostrarDialogoLabConsulta = false
            }
        )
    }
}

@Composable
fun VitalsChip(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CmoSurfaceContainerLow,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
            if (unit.isNotBlank()) Text(unit, fontSize = 9.sp, color = CmoTextSoft)
        }
    }
}

// =================================================================
// TAB 2: EXPEDIENTES CLÍNICOS & HISTORIAL (FORMATO LB-2026-001)
// =================================================================
@Composable
fun DoctorExpedientesTab(
    expedientes: List<com.cmo.hospital.repository.DoctorClinicalRepository.ExpedienteDetalle>,
    onEmitirLab: (List<String>, String, String, Paciente?, String) -> Unit,
    onCambiarEstadoLab: (String, String) -> Unit
) {
    var searchQ by remember { mutableStateOf("") }
    var seleccionado by remember { mutableStateOf(expedientes.firstOrNull()) }
    var mostrarDialogoLabExp by remember { mutableStateOf(false) }

    val filtrados = expedientes.filter {
        searchQ.isBlank() ||
        it.paciente.nombreCompleto.contains(searchQ, ignoreCase = true) ||
        (it.paciente.dui?.contains(searchQ, ignoreCase = true) == true) ||
        (it.paciente.numeroExpediente?.contains(searchQ, ignoreCase = true) == true)
    }

    LaunchedEffect(filtrados) {
        if (seleccionado == null || filtrados.none { it.paciente.id == seleccionado?.paciente?.id }) {
            seleccionado = filtrados.firstOrNull()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Security banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CmoPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Acceso Clínico Auditado · Ley de Privacidad", fontSize = 11.sp, color = CmoTextSoft, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(99.dp), color = CmoPrimarySoft) {
                    Text("Médico Tratante", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }

        // Buscador
        item {
            OutlinedTextField(
                value = searchQ,
                onValueChange = { searchQ = it },
                label = { Text("Buscar por DNI, Expediente (LB-2026-001) o Nombre...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CmoPrimary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Selector rápido de pacientes filtrados
        if (filtrados.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtrados) { exp ->
                        val isSel = exp.paciente.id == seleccionado?.paciente?.id
                        FilterChip(
                            selected = isSel,
                            onClick = { seleccionado = exp },
                            label = {
                                Text(
                                    text = "${exp.paciente.nombreCompleto.split(" ").take(2).joinToString(" ")} (${exp.paciente.numeroExpediente ?: "N/D"})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSel) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        // Paciente seleccionado
        seleccionado?.let { exp ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CmoSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(exp.paciente.nombreCompleto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                                Text("Edad: ${exp.paciente.edad} años (${exp.paciente.fechaNacimiento ?: "N/D"}) · DNI: ${exp.paciente.dui ?: "N/D"}", fontSize = 11.sp, color = CmoTextSoft)
                                Text("Expediente: ${exp.paciente.numeroExpediente ?: "N/D"}", fontSize = 12.sp, color = CmoPrimary, fontWeight = FontWeight.ExtraBold)
                            }
                            exp.cama?.let {
                                Surface(shape = RoundedCornerShape(6.dp), color = CmoPrimarySoft) {
                                    Text(it, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                            }
                        }

                        // Alerta Farmacológica Crítica (Alergias Severas)
                        if (exp.alergiasCriticas.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusRedBg)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("ALERTA FARMACOLÓGICA CRÍTICA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                        Text(exp.alergiasCriticas.joinToString(" · "), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoText)
                                    }
                                }
                            }
                        }

                        // Comorbilidades
                        if (exp.comorbilidades.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Comorbilidades: " + exp.comorbilidades.joinToString(" · "), fontSize = 11.sp, color = CmoTextMuted)
                        }

                        // Signos Vitales Basales del Expediente
                        if (exp.signosVitales.hasAnyData) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Últimos Signos Vitales Registrados:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VitalsChip(label = "PA", value = exp.signosVitales.presionTexto, unit = "", color = CmoPrimary, modifier = Modifier.weight(1f))
                                VitalsChip(label = "PULSO", value = "${exp.signosVitales.frecuenciaCardiaca ?: "--"}", unit = "lpm", color = StatusGreen, modifier = Modifier.weight(1f))
                                VitalsChip(label = "TEMP", value = "${exp.signosVitales.temperaturaC ?: "--"}", unit = "°C", color = StatusAmber, modifier = Modifier.weight(1f))
                                VitalsChip(label = "SPO2", value = "${exp.signosVitales.saturacionOxigeno ?: "--"}", unit = "%", color = CmoPrimary, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Exámenes de Laboratorio en el Expediente
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Exámenes de Laboratorio en Expediente (${exp.ordenesLaboratorio.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CmoText
                        )
                        Text("Historial y estado de análisis clínicos", fontSize = 11.sp, color = CmoTextSoft)
                    }
                    TextButton(onClick = { mostrarDialogoLabExp = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Indicar Examen", fontSize = 11.sp, color = CmoPrimary)
                    }
                }
            }

            if (exp.ordenesLaboratorio.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CmoSurface)
                    ) {
                        Text(
                            "No hay exámenes de laboratorio registrados en este expediente.",
                            fontSize = 12.sp,
                            color = CmoTextSoft,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(exp.ordenesLaboratorio) { ord ->
                    TarjetaOrdenLaboratorioDoctor(
                        orden = ord,
                        onCambiarEstado = { nuevoEstado -> onCambiarEstadoLab(ord.id, nuevoEstado) }
                    )
                }
            }

            // Cronología de Atenciones
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Cronología de Atenciones Médicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
            }

            items(exp.atencionesPrevias) { at ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CmoSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${at.fecha} · ${at.hora}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CmoPrimary)
                            Text(at.especialidad, fontSize = 11.sp, color = CmoTextSoft)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(at.diagnostico, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CmoText)
                        Text("Médico: ${at.medico}", fontSize = 11.sp, color = CmoTextSoft)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(at.notas, fontSize = 12.sp, color = CmoTextMuted)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    if (mostrarDialogoLabExp) {
        seleccionado?.let { exp ->
            DialogoIndicarExamenesLaboratorio(
                paciente = exp.paciente,
                onDismiss = { mostrarDialogoLabExp = false },
                onConfirmar = { examenes, prioridad, obs, estadoInicial ->
                    onEmitirLab(examenes, prioridad, obs, exp.paciente, estadoInicial)
                    mostrarDialogoLabExp = false
                }
            )
        }
    }
}

// =================================================================
// TAB 3: LABORATORIO CLÍNICO LIS (EL DOCTOR GESTIONA Y CAMBIA ESTADOS)
// =================================================================
@Composable
fun DoctorLaboratorioTab(
    ordenes: List<OrdenLaboratorioItem>,
    pacientesDisponibles: List<Paciente>,
    onEmitir: (List<String>, String, String, Paciente?, String) -> Unit,
    onCambiarEstado: (String, String) -> Unit
) {
    var filtroEstado by remember { mutableStateOf("Todos") }
    var mostrarDialogoCrear by remember { mutableStateOf(false) }

    val estadosFiltro = listOf("Todos", "En Espera", "Agendado", "En Proceso", "Realizado", "Cancelado")
    val ordenesFiltradas = if (filtroEstado == "Todos") {
        ordenes
    } else {
        ordenes.filter { it.estado.equals(filtroEstado, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Cabecera institucional LIS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Biotech, contentDescription = null, tint = CmoPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Módulo de Laboratorio Clínico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                                Text("Gestión y control de estados de órdenes", fontSize = 11.sp, color = CmoTextSoft)
                            }
                        }
                        Button(
                            onClick = { mostrarDialogoCrear = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Indicar Examen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("FILTRAR POR ESTADO DE ANÁLISIS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(estadosFiltro) { est ->
                            val isSel = filtroEstado == est
                            FilterChip(
                                selected = isSel,
                                onClick = { filtroEstado = est },
                                label = { Text(est, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CmoPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Contador y lista de órdenes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Órdenes Registradas (${ordenesFiltradas.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CmoText
                )
                Text(
                    text = "Toca el estado para cambiarlo ▾",
                    fontSize = 11.sp,
                    color = CmoPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (ordenesFiltradas.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CmoSurface)
                ) {
                    Text(
                        text = "No hay órdenes con estado: $filtroEstado",
                        fontSize = 12.sp,
                        color = CmoTextSoft,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(ordenesFiltradas) { ord ->
                TarjetaOrdenLaboratorioDoctor(
                    orden = ord,
                    mostrarInfoPaciente = true,
                    onCambiarEstado = { nuevoEstado -> onCambiarEstado(ord.id, nuevoEstado) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    if (mostrarDialogoCrear) {
        DialogoIndicarExamenesLaboratorio(
            pacientesDisponibles = pacientesDisponibles,
            onDismiss = { mostrarDialogoCrear = false },
            onConfirmar = { examenes, prioridad, obs, estadoInicial, paciente ->
                onEmitir(examenes, prioridad, obs, paciente, estadoInicial)
                mostrarDialogoCrear = false
            }
        )
    }
}

// =================================================================
// COMPONENTE: TARJETA DE ORDEN DE LABORATORIO CON CAMBIO DE ESTADO
// =================================================================
@Composable
fun TarjetaOrdenLaboratorioDoctor(
    orden: OrdenLaboratorioItem,
    mostrarInfoPaciente: Boolean = false,
    onCambiarEstado: (String) -> Unit
) {
    var menuExpandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CmoSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila 1: Nombre de Examen, Prioridad y Selector de Estado interactivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(orden.examenNombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CmoText)
                    if (orden.prioridad == "STAT") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = StatusRedBg) {
                            Text("⚡ STAT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusRed, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }

                // Botón interactivo para cambiar el estado de la orden
                Box {
                    val colorEstado = when (orden.estado) {
                        "Realizado" -> StatusGreen
                        "Cancelado" -> Color(0xFF6B7280)
                        "En Espera" -> StatusAmber
                        "Agendado" -> StatusBlue
                        "En Proceso" -> Color(0xFF7E22CE)
                        else -> CmoPrimary
                    }
                    val bgEstado = when (orden.estado) {
                        "Realizado" -> StatusGreenBg
                        "Cancelado" -> Color(0xFFF3F4F6)
                        "En Espera" -> StatusAmberBg
                        "Agendado" -> StatusBlueBg
                        "En Proceso" -> Color(0xFFF3E8FF)
                        else -> CmoPrimarySoft
                    }

                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = bgEstado,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorEstado.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { menuExpandido = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = orden.estado,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorEstado
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colorEstado, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpandido,
                        onDismissRequest = { menuExpandido = false }
                    ) {
                        OrdenLaboratorioItem.ESTADOS_VALIDOS.forEach { estadoOpcion ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = estadoOpcion,
                                        fontWeight = if (estadoOpcion == orden.estado) FontWeight.Bold else FontWeight.Normal,
                                        color = when (estadoOpcion) {
                                            "Realizado" -> StatusGreen
                                            "Cancelado" -> StatusRed
                                            "En Espera" -> StatusAmber
                                            "Agendado" -> StatusBlue
                                            "En Proceso" -> Color(0xFF7E22CE)
                                            else -> CmoText
                                        }
                                    )
                                },
                                onClick = {
                                    menuExpandido = false
                                    onCambiarEstado(estadoOpcion)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (estadoOpcion) {
                                            "Realizado" -> Icons.Default.CheckCircle
                                            "Cancelado" -> Icons.Default.Cancel
                                            "En Espera" -> Icons.Default.HourglassTop
                                            "Agendado" -> Icons.Default.Event
                                            else -> Icons.Default.Autorenew
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Paciente y Expediente (Formato LB-2026-001)
            orden.pacienteNombre?.let { pacNom ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(13.dp), tint = CmoPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$pacNom · Expediente: ${orden.numeroExpediente ?: "N/D"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CmoPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tubo: ${orden.tipoTubo ?: "Estándar"} · Solicitud: ${orden.fechaSolicitud ?: "Hoy"}${if (orden.fechaResultado != null) " · Resultado: ${orden.fechaResultado}" else ""}",
                fontSize = 11.sp,
                color = CmoTextSoft
            )

            orden.observaciones?.let { obs ->
                if (obs.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Indicación: $obs", fontSize = 11.sp, color = CmoTextMuted)
                }
            }
        }
    }
}

// =================================================================
// DIÁLOGO PARA INDICAR EXÁMENES DE LABORATORIO
// =================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoIndicarExamenesLaboratorio(
    paciente: Paciente? = null,
    pacientesDisponibles: List<Paciente> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmar: (List<String>, String, String, String, Paciente?) -> Unit = { _, _, _, _, _ -> }
) {
    var pacienteElegido by remember { mutableStateOf(paciente ?: pacientesDisponibles.firstOrNull()) }
    var prioridad by remember { mutableStateOf("RUTINA") }
    var estadoInicial by remember { mutableStateOf("En Espera") }
    var obs by remember { mutableStateOf("") }
    var examenPersonalizado by remember { mutableStateOf("") }

    val catalogo = listOf(
        "Hemograma Completo + Plaquetas",
        "Troponina I Alta Sensibilidad (hs-cTnI)",
        "Gasometría Arterial y Electrolitos",
        "Perfil Bioquímico & Renal",
        "Perfil Lipídico & Glucemia",
        "Urocultivo & Antibiograma",
        "Examen General de Orina (EGO)",
        "Dímero D Cuantitativo"
    )

    val seleccionados = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Indicar Exámenes de Laboratorio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                pacienteElegido?.let {
                    Text(
                        text = "Paciente: ${it.nombreCompleto} (${it.numeroExpediente ?: "N/D"})",
                        fontSize = 12.sp,
                        color = CmoPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Selector de paciente si no está predefinido
                if (paciente == null && pacientesDisponibles.isNotEmpty()) {
                    item {
                        Text("SELECCIONAR PACIENTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pacientesDisponibles) { pac ->
                                val isSel = pacienteElegido?.id == pac.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = { pacienteElegido = pac },
                                    label = { Text("${pac.nombreCompleto.split(" ").first()} (${pac.numeroExpediente})", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Prioridad
                item {
                    Text("PRIORIDAD ANALÍTICA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (prioridad == "RUTINA") CmoPrimary else CmoSurfaceContainerLow,
                            modifier = Modifier.weight(1f).clickable { prioridad = "RUTINA" }
                        ) {
                            Text("Rutinario", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (prioridad == "RUTINA") Color.White else CmoText, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (prioridad == "STAT") StatusRed else CmoSurfaceContainerLow,
                            modifier = Modifier.weight(1f).clickable { prioridad = "STAT" }
                        ) {
                            Text("⚡ STAT (< 30m)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (prioridad == "STAT") Color.White else CmoText, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center)
                        }
                    }
                }

                // Estado Inicial
                item {
                    Text("ESTADO INICIAL DE LA ORDEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("En Espera", "Agendado", "En Proceso").forEach { est ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (estadoInicial == est) CmoPrimary else CmoSurfaceContainerLow,
                                modifier = Modifier.weight(1f).clickable { estadoInicial = est }
                            ) {
                                Text(est, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (estadoInicial == est) Color.White else CmoText, modifier = Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                // Catálogo de Exámenes
                item {
                    Text("SELECCIONAR EXÁMENES (${seleccionados.size} seleccionados)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
                }

                items(catalogo) { ex ->
                    val isChecked = seleccionados.contains(ex)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CmoSurfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isChecked) seleccionados.remove(ex) else seleccionados.add(ex)
                        }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isChecked, onCheckedChange = {
                                if (it) seleccionados.add(ex) else seleccionados.remove(ex)
                            })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(ex, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CmoText)
                        }
                    }
                }

                // Examen personalizado
                item {
                    OutlinedTextField(
                        value = examenPersonalizado,
                        onValueChange = { examenPersonalizado = it },
                        label = { Text("Otro examen (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Justificación médica
                item {
                    OutlinedTextField(
                        value = obs,
                        onValueChange = { obs = it },
                        label = { Text("Indicaciones clínicas / Observaciones") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val listaFinal = seleccionados.toMutableList()
                    if (examenPersonalizado.isNotBlank()) {
                        listaFinal.add(examenPersonalizado.trim())
                    }
                    if (listaFinal.isNotEmpty()) {
                        onConfirmar(listaFinal, prioridad, obs, estadoInicial, pacienteElegido)
                    }
                },
                enabled = seleccionados.isNotEmpty() || examenPersonalizado.isNotBlank()
            ) {
                Text("Guardar en Expediente & Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Diálogo sobrecargado para cuando ya se conoce el paciente
@Composable
fun DialogoIndicarExamenesLaboratorio(
    paciente: Paciente,
    onDismiss: () -> Unit,
    onConfirmar: (List<String>, String, String, String) -> Unit
) {
    DialogoIndicarExamenesLaboratorio(
        paciente = paciente,
        pacientesDisponibles = listOf(paciente),
        onDismiss = onDismiss,
        onConfirmar = { examenes, prioridad, obs, estadoInicial, _ ->
            onConfirmar(examenes, prioridad, obs, estadoInicial)
        }
    )
}

// =================================================================
// DIÁLOGOS DE APOYO CLÍNICO
// =================================================================
@Composable
fun DialogoEditarSignos(
    signosActuales: SignosVitales,
    onDismiss: () -> Unit,
    onGuardar: (SignosVitales) -> Unit
) {
    var sist by remember { mutableStateOf(signosActuales.presionSistolica?.toString() ?: "") }
    var diast by remember { mutableStateOf(signosActuales.presionDiastolica?.toString() ?: "") }
    var fc by remember { mutableStateOf(signosActuales.frecuenciaCardiaca?.toString() ?: "") }
    var fr by remember { mutableStateOf(signosActuales.frecuenciaRespiratoria?.toString() ?: "") }
    var temp by remember { mutableStateOf(signosActuales.temperaturaC?.toString() ?: "") }
    var spo2 by remember { mutableStateOf(signosActuales.saturacionOxigeno?.toString() ?: "") }
    var peso by remember { mutableStateOf(signosActuales.pesoKg?.toString() ?: "") }
    var talla by remember { mutableStateOf(signosActuales.tallaCm?.toString() ?: "") }
    var obs by remember { mutableStateOf(signosActuales.observaciones ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Toma / Ajuste de Signos Vitales", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Campos flexibles (opcionales para emergencia)", fontSize = 11.sp, color = CmoTextSoft)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sist,
                            onValueChange = { sist = it },
                            label = { Text("PA Sist.") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = diast,
                            onValueChange = { diast = it },
                            label = { Text("PA Diast.") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fc,
                            onValueChange = { fc = it },
                            label = { Text("FC (lpm)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = fr,
                            onValueChange = { fr = it },
                            label = { Text("FR (rpm)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = temp,
                            onValueChange = { temp = it },
                            label = { Text("Temp (°C)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = spo2,
                            onValueChange = { spo2 = it },
                            label = { Text("SpO2 (%)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = peso,
                            onValueChange = { peso = it },
                            label = { Text("Peso (kg)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = talla,
                            onValueChange = { talla = it },
                            label = { Text("Talla (cm)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = obs,
                        onValueChange = { obs = it },
                        label = { Text("Observaciones del examen físico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val nuevos = SignosVitales(
                    presionSistolica = sist.toIntOrNull(),
                    presionDiastolica = diast.toIntOrNull(),
                    frecuenciaCardiaca = fc.toIntOrNull(),
                    frecuenciaRespiratoria = fr.toIntOrNull(),
                    temperaturaC = temp.toDoubleOrNull(),
                    saturacionOxigeno = spo2.toIntOrNull(),
                    pesoKg = peso.toDoubleOrNull(),
                    tallaCm = talla.toDoubleOrNull(),
                    observaciones = obs.ifBlank { null }
                )
                onGuardar(nuevos)
            }) {
                Text("Guardar Signos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun DialogoAgregarCie10(
    onDismiss: () -> Unit,
    onAgregar: (String, String) -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val catalogo = listOf(
        "I10" to "Hipertensión esencial (primaria)",
        "E11" to "Diabetes mellitus tipo 2",
        "J45" to "Asma bronquial",
        "R07.4" to "Dolor en el pecho, no especificado",
        "K29" to "Gastritis y duodenitis",
        "J06.9" to "Infección respiratoria aguda, no especificada",
        "N39.0" to "Infección del tracto urinario, sitio no especificado",
        "M54.5" to "Lumbago no especificado"
    )

    val filtrados = catalogo.filter {
        busqueda.isBlank() || it.first.contains(busqueda, true) || it.second.contains(busqueda, true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Diagnóstico CIE-10", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar código o patología...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtrados) { (cod, desc) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CmoSurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth().clickable { onAgregar(cod, desc) }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(cod, fontWeight = FontWeight.ExtraBold, color = CmoPrimary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(desc, fontSize = 12.sp, color = CmoText)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
fun DialogoAgregarReceta(
    onDismiss: () -> Unit,
    onAgregar: (RecetaItem) -> Unit
) {
    var med by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("") }
    var dur by remember { mutableStateOf("") }
    var indic by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Crónico") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prescripción de Medicamento", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = med, onValueChange = { med = it }, label = { Text("Medicamento (ej. Losartán 50mg)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dosis, onValueChange = { dosis = it }, label = { Text("Dosis (ej. 1 comp.)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dur, onValueChange = { dur = it }, label = { Text("Duración (ej. 30 días)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = freq, onValueChange = { freq = it }, label = { Text("Frecuencia (ej. Cada 12 hrs)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = indic, onValueChange = { indic = it }, label = { Text("Indicaciones específicas") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Crónico", "SOS", "Agudo").forEach { t ->
                        FilterChip(
                            selected = tipo == t,
                            onClick = { tipo = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (med.isNotBlank() && dosis.isNotBlank() && freq.isNotBlank()) {
                        onAgregar(
                            RecetaItem(
                                medicamento = med,
                                dosis = dosis,
                                frecuencia = freq,
                                duracion = dur.ifBlank { "7 días" },
                                indicaciones = indic.ifBlank { null },
                                tipo = tipo
                            )
                        )
                    }
                },
                enabled = med.isNotBlank() && dosis.isNotBlank() && freq.isNotBlank()
            ) {
                Text("Añadir a Receta")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * Diálogo para que el Doctor agende citas de control / seguimiento para sí mismo.
 * Valida disponibilidad en intervalos de 30 minutos sin permitir solapamientos.
 */
@Composable
fun DialogoAgendarCitaDoctor(
    pacienteDefault: Paciente,
    expedientesDisponibles: List<com.cmo.hospital.repository.DoctorClinicalRepository.ExpedienteDetalle>,
    doctorNombre: String,
    especialidadDoctor: String,
    citasExistentes: List<Cita>,
    onDismiss: () -> Unit,
    onConfirmar: (Paciente, String, String, String, String) -> Unit
) {
    var pacienteSeleccionado by remember { mutableStateOf(pacienteDefault) }
    var fechaCita by remember { mutableStateOf("2026-10-29") }
    var horaInicioCita by remember { mutableStateOf("09:00") }
    var motivoCita by remember { mutableStateOf("Control y seguimiento médico post-consulta") }

    val hayConflicto = remember(citasExistentes, doctorNombre, fechaCita, horaInicioCita) {
        Cita.hayConflictoHorario(citasExistentes, doctorNombre, fechaCita, horaInicioCita)
    }

    val horaFinCalculada = Cita.calcularFin30Minutos(horaInicioCita)
    val listaPacientes = remember(expedientesDisponibles) { expedientesDisponibles.map { it.paciente } }
    val bloquesHorarios = Cita.BLOQUES_30_MINUTOS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Agendar Cita de Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Médico: $doctorNombre · $especialidadDoctor", fontSize = 12.sp, color = CmoPrimary)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Paciente:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CmoSurfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(pacienteSeleccionado.nombreCompleto, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CmoText)
                            Text("Expediente: ${pacienteSeleccionado.numeroExpediente ?: "N/D"} · DUI: ${pacienteSeleccionado.dui ?: "N/D"}", fontSize = 11.sp, color = CmoTextSoft)
                        }
                    }
                }

                if (listaPacientes.size > 1) {
                    item {
                        Text("O cambiar paciente:", fontSize = 11.sp, color = CmoTextSoft)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items = listaPacientes) { p ->
                                val sel = p.id == pacienteSeleccionado.id
                                FilterChip(
                                    selected = sel,
                                    onClick = { pacienteSeleccionado = p },
                                    label = { Text(p.nombreCompleto.split(" ").take(2).joinToString(" "), fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = fechaCita,
                        onValueChange = { fechaCita = it },
                        label = { Text("Fecha de Cita (AAAA-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Horario de Disponibilidad (Intervalos de 30 min):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items = bloquesHorarios) { hora ->
                            val ocupado = Cita.hayConflictoHorario(citasExistentes, doctorNombre, fechaCita, hora)
                            val sel = hora == horaInicioCita
                            AssistChip(
                                onClick = { if (!ocupado) horaInicioCita = hora },
                                enabled = !ocupado,
                                label = {
                                    Text(
                                        if (ocupado) "$hora (Ocupado)" else hora,
                                        fontSize = 11.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (ocupado) StatusRed else if (sel) CmoPrimary else CmoText
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (sel) CmoPrimarySoft else if (ocupado) StatusRedBg else Color.Transparent
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = !ocupado,
                                    borderColor = if (sel) CmoPrimary else Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }

                item {
                    Surface(shape = RoundedCornerShape(8.dp), color = CmoSurfaceContainerLow) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Duración fija:", fontSize = 11.sp, color = CmoTextSoft)
                            Text("$horaInicioCita - $horaFinCalculada (30 min)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmoPrimary)
                        }
                    }
                }

                if (hayConflicto) {
                    item {
                        Surface(shape = RoundedCornerShape(8.dp), color = StatusRedBg) {
                            Text(
                                "⛔ Ya tienes una cita asignada en ese bloque ($horaInicioCita). Por ningún motivo se permiten citas a la misma hora.",
                                fontSize = 11.sp,
                                color = StatusRed,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = motivoCita,
                        onValueChange = { motivoCita = it },
                        label = { Text("Motivo / Indicaciones de seguimiento *") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (motivoCita.isNotBlank() && !hayConflicto) {
                        onConfirmar(pacienteSeleccionado, fechaCita, horaInicioCita, motivoCita, especialidadDoctor)
                    }
                },
                enabled = motivoCita.isNotBlank() && !hayConflicto,
                colors = ButtonDefaults.buttonColors(containerColor = CmoPrimary)
            ) {
                Text("Agendar Cita (30 min)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
