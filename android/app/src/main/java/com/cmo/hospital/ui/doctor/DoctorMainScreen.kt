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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CmoPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = CmoPrimaryFixed, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HUC CMO",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = CmoPrimary) {
                                    Text(
                                        text = "MÉDICO",
                                        color = CmoPrimaryFixed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.currentUser?.nombreCompleto ?: consulta.medicoNombre} · ${consulta.box}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CmoTeal
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Color(0xFFF08A8C)
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
                    onFinalizar = { viewModel.finalizarConsulta {} },
                    onIrALab = { viewModel.setTab(2) }
                )
                1 -> DoctorExpedientesTab(
                    expedientes = expedientes
                )
                2 -> DoctorLaboratorioTab(
                    ordenes = ordenesLab,
                    onEmitir = { examenes, prio, obs -> viewModel.emitirOrdenLab(examenes, prio, obs) }
                )
            }
        }
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
    onFinalizar: () -> Unit,
    onIrALab: () -> Unit
) {
    var mostrarDialogoVitals by remember { mutableStateOf(false) }
    var mostrarDialogoCie10 by remember { mutableStateOf(false) }
    var mostrarDialogoReceta by remember { mutableStateOf(false) }

    var anamnesisText by remember(consulta.anamnesis) { mutableStateOf(consulta.anamnesis) }

    val min = consulta.segundosTranscurridos / 60
    val seg = consulta.segundosTranscurridos % 60
    val tiempoFormateado = String.format("%02d:%02d", min, seg)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Box & Turn Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CmoPrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = CmoPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(consulta.box, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoPrimaryDark)
                                Text("${consulta.especialidad} · Urgencias", fontSize = 11.sp, color = CmoTextSoft)
                            }
                        }

                        Surface(shape = RoundedCornerShape(99.dp), color = CmoPrimarySoft) {
                            Text("Mañana 07:00 - 15:00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmoPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Medidor de avance de citas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Agenda de Turno: 8 Citas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmoText)
                        Text("3 Atendidas · 1 En Box · 4 En Espera", fontSize = 11.sp, color = CmoTextSoft)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                    ) {
                        Box(modifier = Modifier.weight(3f).fillMaxHeight().background(CmoPrimary))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(StatusAmber))
                        Box(modifier = Modifier.weight(4f).fillMaxHeight().background(CmoSurfaceContainerHigh))
                    }
                }
            }
        }

        // SEMÁFORO DE TIEMPO DE CONSULTA (30 MINUTOS)
        item {
            val colorSemaforo = when (consulta.estadoSemaforo) {
                EstadoSemaforo.VERDE -> StatusGreen
                EstadoSemaforo.AMBAR -> StatusAmber
                EstadoSemaforo.ROJO -> StatusRed
            }
            val colorSemaforoBg = when (consulta.estadoSemaforo) {
                EstadoSemaforo.VERDE -> StatusGreenBg
                EstadoSemaforo.AMBAR -> StatusAmberBg
                EstadoSemaforo.ROJO -> StatusRedBg
            }
            val textoSemaforo = when (consulta.estadoSemaforo) {
                EstadoSemaforo.VERDE -> "🟢 Consulta en tiempo regular (< 20 min)"
                EstadoSemaforo.AMBAR -> "🟡 Atención por concluir · 5 min restantes"
                EstadoSemaforo.ROJO -> "🔴 Tiempo de consulta agotado (> 25 min) · Sobretiempo"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorSemaforoBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorSemaforo.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colorSemaforo)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CRONÓMETRO DE CONSULTA (30 MIN)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorSemaforo
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tiempoFormateado,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorSemaforo
                        )
                        Text(
                            text = textoSemaforo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorSemaforo
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = onToggleCronometro) {
                            Icon(
                                imageVector = if (isCronometroCorriendo) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = colorSemaforo,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = if (isCronometroCorriendo) "Pausar" else "Reanudar",
                            fontSize = 10.sp,
                            color = colorSemaforo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Paciente en Box
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
                            Text(consulta.paciente.nombreCompleto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                            Text(
                                text = "Edad: ${consulta.paciente.edad} años (${consulta.paciente.fechaNacimiento ?: "N/D"}) · DNI: ${consulta.paciente.dui ?: "N/D"}",
                                fontSize = 11.sp,
                                color = CmoTextSoft
                            )
                            Text("Ficha: ${consulta.paciente.numeroExpediente ?: "N/D"} · Cobertura: ${consulta.paciente.prevision ?: "Fonasa"}", fontSize = 11.sp, color = CmoPrimary)
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = StatusAmberBg) {
                            Text("En Atención", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusAmber, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Signos Vitales del Triaje (con botón de tomar/editar si es emergencia)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Signos Vitales Clínicos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CmoText)
                        TextButton(onClick = { mostrarDialogoVitals = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tomar / Editar Signos", fontSize = 11.sp, color = CmoPrimary)
                        }
                    }

                    val sv = consulta.signosVitales
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitalsChip(label = "Presión", value = sv.presionTexto, unit = "", color = StatusBlue, modifier = Modifier.weight(1f))
                        VitalsChip(label = "FC", value = sv.frecuenciaCardiaca?.toString() ?: "--", unit = "lpm", color = StatusRed, modifier = Modifier.weight(1f))
                        VitalsChip(label = "Temp", value = sv.temperaturaC?.toString() ?: "--", unit = "°C", color = StatusAmber, modifier = Modifier.weight(1f))
                        VitalsChip(label = "SpO2", value = sv.saturacionOxigeno?.toString() ?: "--", unit = "%", color = CmoPrimary, modifier = Modifier.weight(1f))
                    }

                    sv.imc?.let { imc ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Antropometría: ${sv.pesoKg ?: "--"} kg · ${sv.tallaCm ?: "--"} cm ➔ IMC: $imc (${sv.imcClasificacion})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CmoTextSoft
                        )
                    }

                    sv.observaciones?.let { obs ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Obs de triaje: $obs", fontSize = 11.sp, color = CmoTextMuted)
                    }
                }
            }
        }

        // Hipótesis Diagnóstica (CIE-10)
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
                        placeholder = { Text("Describa motivo de consulta, examen físico y plan terapéutico...") },
                        minLines = 4,
                        maxLines = 8
                    )
                }
            }
        }

        // Receta Médica Digital
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
                        Text("Receta Médica Digital", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                        TextButton(onClick = { mostrarDialogoReceta = true }) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir Fármaco", fontSize = 11.sp, color = CmoPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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

        // Acciones Finales de Consulta
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onIrALab,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Emitir Orden de Laboratorio / LIS", fontWeight = FontWeight.SemiBold)
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

    // Diálogo para que el Doctor tome/edite signos vitales
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
// TAB 2: EXPEDIENTES CLÍNICOS & HISTORIAL (AUDITADO & LEY PRIVACIDAD)
// =================================================================
@Composable
fun DoctorExpedientesTab(
    expedientes: List<com.cmo.hospital.repository.DoctorClinicalRepository.ExpedienteDetalle>
) {
    var searchQ by remember { mutableStateOf("") }
    var seleccionado by remember { mutableStateOf(expedientes.firstOrNull()) }

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
                label = { Text("Buscar por DNI, Ficha o Nombre...") },
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
                                    text = exp.paciente.nombreCompleto.split(" ").take(2).joinToString(" "),
                                    fontSize = 12.sp,
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
                                Text("Expediente: ${exp.paciente.numeroExpediente ?: "N/D"}", fontSize = 11.sp, color = CmoPrimary, fontWeight = FontWeight.Bold)
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
                            Text("Últimos Signos Vitales Basales:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
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

            // Cronología de Atenciones
            item {
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
}

// =================================================================
// TAB 3: LABORATORIO CLÍNICO LIS (STAT < 30 MIN VS RUTINARIO)
// =================================================================
@Composable
fun DoctorLaboratorioTab(
    ordenes: List<OrdenLaboratorioItem>,
    onEmitir: (List<String>, String, String) -> Unit
) {
    var prioridad by remember { mutableStateOf("STAT") } // STAT o RUTINA
    var obs by remember { mutableStateOf("") }

    val catalogo = listOf(
        "Hemograma Completo + Plaquetas" to "Tubo EDTA",
        "Troponina I Alta Sensibilidad (hs-cTnI)" to "Protocolo SCA",
        "Gasometría Arterial y Electrolitos" to "Heparina Li",
        "Perfil Bioquímico & Renal" to "Suero Seco",
        "Dímero D Cuantitativo" to "Citrato"
    )
    val seleccionados = remember { mutableStateListOf("Hemograma Completo + Plaquetas", "Troponina I Alta Sensibilidad (hs-cTnI)") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Card Emisión de Orden
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
                                Text("Emisión de Orden al LIS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                                Text("Laboratorio Clínico Central", fontSize = 11.sp, color = CmoTextSoft)
                            }
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = CmoPrimarySoft) {
                            Text("LIS Conectado", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de Prioridad Obligatorio
                    Text("PRIORIDAD ANALÍTICA (OBLIGATORIA)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmoTextSoft)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = if (prioridad == "RUTINA") CmoPrimary else CmoSurfaceContainerLow,
                            modifier = Modifier.weight(1f).clickable { prioridad = "RUTINA" }
                        ) {
                            Text("Rutinario (Turno)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (prioridad == "RUTINA") Color.White else CmoText, modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = if (prioridad == "STAT") StatusRed else CmoSurfaceContainerLow,
                            modifier = Modifier.weight(1f).clickable { prioridad = "STAT" }
                        ) {
                            Text("⚡ Urgente / STAT (< 30m)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (prioridad == "STAT") Color.White else CmoText, modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }

                    if (prioridad == "STAT") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("⚠️ Alerta prioritaria activa: Tiempos de respuesta reducidos a 30 min en LIS.", fontSize = 11.sp, color = StatusRed, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lista de determinaciones
                    Text("Determinaciones Bioquímicas (${seleccionados.size} elegidas)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoText)
                    Spacer(modifier = Modifier.height(6.dp))

                    catalogo.forEach { (nombre, tubo) ->
                        val isChecked = seleccionados.contains(nombre)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CmoSurfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    if (isChecked) seleccionados.remove(nombre) else seleccionados.add(nombre)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(checked = isChecked, onCheckedChange = {
                                        if (it) seleccionados.add(nombre) else seleccionados.remove(nombre)
                                    })
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(nombre, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CmoText)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = CmoPrimarySoft) {
                                    Text(tubo, fontSize = 10.sp, color = CmoPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = obs,
                        onValueChange = { obs = it },
                        label = { Text("Justificación clínica") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (seleccionados.isNotEmpty()) {
                                onEmitir(seleccionados.toList(), prioridad, obs)
                                obs = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (prioridad == "STAT") StatusRed else CmoPrimary)
                    ) {
                        Text("EMITIR ORDEN ANALÍTICA AL LIS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Cola de órdenes activas
        item {
            Text("Órdenes de Laboratorio en Seguimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
        }

        items(ordenes) { ord ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ord.examenNombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CmoText)
                            if (ord.prioridad == "STAT") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = StatusRedBg) {
                                    Text("STAT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusRed, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Text("Tubo: ${ord.tipoTubo ?: "Estándar"} · Solicitud: ${ord.fechaSolicitud ?: "Hoy"}", fontSize = 11.sp, color = CmoTextSoft)
                        ord.observaciones?.let { Text(it, fontSize = 11.sp, color = CmoTextMuted) }
                    }

                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = when (ord.estado) {
                            "COMPLETADO" -> StatusGreenBg
                            "EN_PROCESO" -> StatusAmberBg
                            else -> StatusBlueBg
                        }
                    ) {
                        Text(
                            text = ord.estado,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (ord.estado) {
                                "COMPLETADO" -> StatusGreen
                                "EN_PROCESO" -> StatusAmber
                                else -> StatusBlue
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
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
    var sat by remember { mutableStateOf(signosActuales.saturacionOxigeno?.toString() ?: "") }
    var peso by remember { mutableStateOf(signosActuales.pesoKg?.toString() ?: "") }
    var talla by remember { mutableStateOf(signosActuales.tallaCm?.toString() ?: "") }
    var obs by remember { mutableStateOf(signosActuales.observaciones ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Toma de Signos Vitales en Box", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Campos flexibles (útil para emergencias y re-toma directa):", fontSize = 11.sp, color = CmoTextSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = sist, onValueChange = { sist = it }, label = { Text("Sistólica") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = diast, onValueChange = { diast = it }, label = { Text("Diastólica") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = fc, onValueChange = { fc = it }, label = { Text("FC (lpm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = fr, onValueChange = { fr = it }, label = { Text("FR (rpm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = temp, onValueChange = { temp = it }, label = { Text("Temp (°C)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = sat, onValueChange = { sat = it }, label = { Text("SpO2 (%)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = peso, onValueChange = { peso = it }, label = { Text("Peso (kg)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = talla, onValueChange = { talla = it }, label = { Text("Talla (cm)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                OutlinedTextField(value = obs, onValueChange = { obs = it }, label = { Text("Notas de signos") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onGuardar(
                    SignosVitales(
                        presionSistolica = sist.toIntOrNull(),
                        presionDiastolica = diast.toIntOrNull(),
                        frecuenciaCardiaca = fc.toIntOrNull(),
                        frecuenciaRespiratoria = fr.toIntOrNull(),
                        temperaturaC = temp.toDoubleOrNull(),
                        saturacionOxigeno = sat.toIntOrNull(),
                        pesoKg = peso.toDoubleOrNull(),
                        tallaCm = talla.toDoubleOrNull(),
                        observaciones = obs.ifBlank { null }
                    )
                )
            }) { Text("Guardar Signos") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoAgregarCie10(onDismiss: () -> Unit, onAgregar: (String, String) -> Unit) {
    var codigo by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Diagnóstico CIE-10", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = codigo, onValueChange = { codigo = it }, label = { Text("Código (Ej. I10, J45, R07.4)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción patológica") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (codigo.isNotBlank() && desc.isNotBlank()) onAgregar(codigo.trim().uppercase(), desc.trim()) }) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoAgregarReceta(onDismiss: () -> Unit, onAgregar: (RecetaItem) -> Unit) {
    var med by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var frec by remember { mutableStateOf("") }
    var duracion by remember { mutableStateOf("") }
    var ind by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Crónico") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Medicamento a Receta", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = med, onValueChange = { med = it }, label = { Text("Medicamento (Ej. Enalapril 10mg)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = dosis, onValueChange = { dosis = it }, label = { Text("Dosis") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = frec, onValueChange = { frec = it }, label = { Text("Frecuencia") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = duracion, onValueChange = { duracion = it }, label = { Text("Duración") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = tipo, onValueChange = { tipo = it }, label = { Text("Tipo (Crónico/SOS)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = ind, onValueChange = { ind = it }, label = { Text("Indicaciones específicas") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (med.isNotBlank() && dosis.isNotBlank()) {
                    onAgregar(RecetaItem(medicamento = med, dosis = dosis, frecuencia = frec, duracion = duracion, indicaciones = ind, tipo = tipo))
                }
            }) { Text("Añadir a Receta") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
