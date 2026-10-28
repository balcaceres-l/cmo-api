package com.cmo.hospital.ui.recepcionista

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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmo.hospital.data.model.*
import com.cmo.hospital.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecepcionistaMainScreen(
    viewModel: RecepcionistaViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val citas by viewModel.citas.collectAsState()
    val turnoEnBox by viewModel.turnoEnBox.collectAsState()
    val camas by viewModel.camas.collectAsState()
    val ambulancia by viewModel.ambulancia.collectAsState()

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
                                .background(CmoPrimaryFixed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = CmoPrimaryDark,
                                modifier = Modifier.size(24.dp)
                            )
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
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CmoPrimary
                                ) {
                                    Text(
                                        text = "RECEPCIÓN",
                                        color = CmoPrimaryFixed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.currentUser?.nombreCompleto ?: "Puesto 04"} · Guardia Activa",
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
            NavigationBar(
                containerColor = CmoSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null) },
                    label = { Text("Citas & Llamador", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CmoPrimary,
                        selectedTextColor = CmoPrimary,
                        indicatorColor = CmoPrimarySoft
                    )
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.Hotel, contentDescription = null) },
                    label = { Text("Admisión & Camas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CmoPrimary,
                        selectedTextColor = CmoPrimary,
                        indicatorColor = CmoPrimarySoft
                    )
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.Emergency, contentDescription = null) },
                    label = { Text("Ambulancia & Turnos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                0 -> CitasLlamadorTab(
                    citas = citas,
                    turnoEnBox = turnoEnBox,
                    onAgendar = { esp, doc, nom, dui, fn, fecha, hora, mot, box ->
                        viewModel.agendarCita(esp, doc, nom, dui, fn, fecha, hora, mot, box) {}
                    },
                    onLlegada = { viewModel.registrarLlegada(it) },
                    onCancelar = { id, mot -> viewModel.cancelarCita(id, mot) },
                    onTomarSignosYPasarAConsulta = { id, vitals, pasar ->
                        viewModel.tomarSignosYPasarAConsulta(id, vitals, pasar) {}
                    },
                    onReLlamar = { viewModel.reLlamar() },
                    onSiguiente = { viewModel.siguienteTurno() }
                )
                1 -> AdmisionCamasTab(
                    camas = camas,
                    onRegistrarEIngresar = { pac, vit, cama, mot, eme ->
                        viewModel.registrarEIngresarPaciente(pac, vit, cama, mot, eme) {}
                    }
                )
                2 -> AmbulanciaTurnosTab(
                    ambulancia = ambulancia,
                    personalGuardia = viewModel.personalGuardia,
                    onCambiarEstado = { viewModel.cambiarEstadoAmbulancia(it) },
                    onDespacho = { dir, pac, desc, prio -> viewModel.solicitarDespacho(dir, pac, desc, prio) }
                )
            }
        }
    }
}

// =================================================================
// TAB 1: CITAS Y LLAMADOR EN SALA (BLOQUES DE 30 MIN - CASCADA)
// =================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CitasLlamadorTab(
    citas: List<Cita>,
    turnoEnBox: Cita?,
    onAgendar: (String, String, String, String, String, String, String, String, String) -> Unit,
    onLlegada: (String) -> Unit,
    onCancelar: (String, String) -> Unit,
    onTomarSignosYPasarAConsulta: (String, SignosVitales?, Boolean) -> Unit,
    onReLlamar: () -> Unit,
    onSiguiente: () -> Unit
) {
    var mostrarFormAgendar by remember { mutableStateOf(false) }
    var citaParaTriaje by remember { mutableStateOf<Cita?>(null) }

    // Cascada: Especialidad -> Doctor
    val especialidades = remember { CATALOGO_ESPECIALIDADES.keys.toList() }
    var citaEspecialidad by remember { mutableStateOf(especialidades.firstOrNull() ?: "Medicina General") }
    val doctoresDisponibles = remember(citaEspecialidad) {
        CATALOGO_ESPECIALIDADES[citaEspecialidad] ?: listOf("Dr. Alejandro Restrepo")
    }
    var citaDoctor by remember(citaEspecialidad) {
        mutableStateOf(doctoresDisponibles.firstOrNull() ?: "Dr. Alejandro Restrepo")
    }

    // Datos del Paciente y Cita
    var pacNombre by remember { mutableStateOf("") }
    var pacDui by remember { mutableStateOf("") }
    var pacFechaNac by remember { mutableStateOf("1990-05-15") }
    var citaFecha by remember { mutableStateOf("2026-10-28") }
    var citaHoraInicio by remember { mutableStateOf("08:30") }
    var citaMotivo by remember { mutableStateOf("") }
    var citaBox by remember { mutableStateOf("Box 03") }

    // Validación de Conflicto en Tiempo Real (Por ningún motivo citas a la misma hora)
    val hayConflicto = remember(citas, citaDoctor, citaFecha, citaHoraInicio) {
        Cita.hayConflictoHorario(citas, citaDoctor, citaFecha, citaHoraInicio)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Context Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ADMISIÓN Y CONSULTORIOS EXTERNOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CmoPrimary
                        )
                        Text(
                            text = "Gestión de Citas y Turnos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CmoText
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = StatusBlueBg
                    ) {
                        Text(
                            text = "Puesto 04",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Llamador Activo (Sala Central)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusBlueBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = StatusBlue, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Llamador Activo (Sala Central)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CmoText)
                                Text("Pantalla Pública #02", fontSize = 11.sp, color = CmoTextSoft)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = StatusAmberBg
                        ) {
                            Text("EN VOZ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusAmber, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tarjeta destacada del turno en Box con nombre anonimizado (C. M****)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CmoPrimaryDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TURNO EN BOX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoTeal)
                                Text(
                                    text = turnoEnBox?.consultorio?.codigo ?: "Box 03",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = CmoPrimaryFixedDim, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = anonimizarNombre(turnoEnBox?.paciente?.nombreCompleto ?: "Carlos Mendoza"),
                                        fontSize = 12.sp,
                                        color = CmoPrimaryFixed
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(shape = RoundedCornerShape(6.dp), color = CmoPrimaryContainer) {
                                    Text(turnoEnBox?.horaInicio ?: "08:30", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoPrimaryFixed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(turnoEnBox?.consultorio?.nombre ?: "Medicina Interna", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botones de acción del llamador
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onReLlamar,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CmoSurfaceContainer, contentColor = CmoText)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-llamar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onSiguiente,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CmoPrimary, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Siguiente", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Formulario Agendar Cita (en Cascada Especialidad -> Médico -> Paciente -> Intervalo 30m sin solapamiento)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarFormAgendar = !mostrarFormAgendar },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddTask, contentDescription = null, tint = CmoPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Agendar Cita (Cascada Especialidad-Doctor)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                                Text("Intervalos de 30 min · Sin solapamiento horario", fontSize = 11.sp, color = CmoPrimary)
                            }
                        }
                        Icon(
                            imageVector = if (mostrarFormAgendar) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = CmoTextSoft
                        )
                    }

                    if (mostrarFormAgendar) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // PASO 1: Cascada Especialidad
                        Text("1. Especialidad Médica:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoText)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items = especialidades) { esp ->
                                val sel = esp == citaEspecialidad
                                FilterChip(
                                    selected = sel,
                                    onClick = { citaEspecialidad = esp },
                                    label = { Text(esp, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PASO 2: Doctor según Especialidad
                        Text("2. Médico Asignado:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoText)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items = doctoresDisponibles) { doc ->
                                val sel = doc == citaDoctor
                                FilterChip(
                                    selected = sel,
                                    onClick = { citaDoctor = doc },
                                    label = { Text(doc, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // PASO 3: Datos del Paciente
                        Text("3. Datos del Paciente:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoText)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = pacNombre,
                            onValueChange = { pacNombre = it },
                            label = { Text("Nombre completo del paciente *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pacDui,
                                onValueChange = { pacDui = it },
                                label = { Text("DUI / Documento") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = pacFechaNac,
                                onValueChange = { pacFechaNac = it },
                                label = { Text("Fecha Nac. (AAAA-MM-DD)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // PASO 4: Fecha y Bloque Horario (30 minutos)
                        Text("4. Horario Disponible (Intervalos de 30 min):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoText)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = citaFecha,
                                onValueChange = { citaFecha = it },
                                label = { Text("Fecha (AAAA-MM-DD)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = citaBox,
                                onValueChange = { citaBox = it },
                                label = { Text("Consultorio / Box") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Seleccione bloque de 30 minutos para $citaDoctor:", fontSize = 11.sp, color = CmoTextSoft)
                        Spacer(modifier = Modifier.height(4.dp))

                        val listaBloques = Cita.BLOQUES_30_MINUTOS
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items = listaBloques) { hora ->
                                val ocupado = Cita.hayConflictoHorario(citas, citaDoctor, citaFecha, hora)
                                val sel = hora == citaHoraInicio
                                AssistChip(
                                    onClick = { if (!ocupado) citaHoraInicio = hora },
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

                        val horaFinCalculada = Cita.calcularFin30Minutos(citaHoraInicio)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = CmoSurfaceContainerLow) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Horario bloque fijado:", fontSize = 11.sp, color = CmoTextSoft)
                                Text("$citaHoraInicio - $horaFinCalculada (30 minutos exactos)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoPrimary)
                            }
                        }

                        // Banner de conflicto si existiese
                        if (hayConflicto) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusRedBg
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "⛔ Horario no disponible: $citaDoctor ya tiene una cita a las $citaHoraInicio el $citaFecha. Por ningún motivo se permiten citas a la misma hora.",
                                        fontSize = 11.sp,
                                        color = StatusRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = citaMotivo,
                            onValueChange = { citaMotivo = it },
                            label = { Text("Motivo de consulta *") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (pacNombre.isNotBlank() && citaMotivo.isNotBlank() && !hayConflicto) {
                                    onAgendar(
                                        citaEspecialidad,
                                        citaDoctor,
                                        pacNombre,
                                        pacDui,
                                        pacFechaNac,
                                        citaFecha,
                                        citaHoraInicio,
                                        citaMotivo,
                                        citaBox
                                    )
                                    pacNombre = ""
                                    pacDui = ""
                                    citaMotivo = ""
                                    mostrarFormAgendar = false
                                }
                            },
                            enabled = pacNombre.isNotBlank() && citaMotivo.isNotBlank() && !hayConflicto,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CmoPrimary)
                        ) {
                            Text("Confirmar y Agendar Cita (30 min)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Listado de citas de hoy
        item {
            Text("Citas y Turnos Programados Hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
        }

        items(citas) { cita ->
            CitaRecepcionCard(
                cita = cita,
                onLlegada = { onLlegada(cita.id) },
                onCancelar = { onCancelar(cita.id, "Cancelada por paciente") },
                onTomarSignos = { citaParaTriaje = cita },
                onPasarDirectoAConsulta = { onTomarSignosYPasarAConsulta(cita.id, null, true) }
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Modal de Triaje (Signos Vitales Opcionales y Pase a Consulta)
    citaParaTriaje?.let { citaSel ->
        DialogoTriajeCitaRecepcion(
            cita = citaSel,
            onDismiss = { citaParaTriaje = null },
            onGuardarSignos = { signos, pasarAConsulta ->
                onTomarSignosYPasarAConsulta(citaSel.id, signos, pasarAConsulta)
                citaParaTriaje = null
            }
        )
    }
}

@Composable
fun CitaRecepcionCard(
    cita: Cita,
    onLlegada: () -> Unit,
    onCancelar: () -> Unit,
    onTomarSignos: () -> Unit,
    onPasarDirectoAConsulta: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CmoSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = CmoPrimarySoft) {
                        Text(
                            text = "${cita.horaInicio} - ${cita.horaFin ?: Cita.calcularFin30Minutos(cita.horaInicio)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CmoPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(cita.consultorio?.codigo ?: "Box", fontSize = 11.sp, color = CmoTextSoft, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = when (cita.estado) {
                        "EN_ESPERA" -> StatusAmberBg
                        "ATENDIDA" -> StatusGreenBg
                        "EN_ATENCION" -> StatusBlueBg
                        "CANCELADA" -> StatusRedBg
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        text = when (cita.estado) {
                            "EN_ESPERA" -> "En Espera"
                            "ATENDIDA" -> "Atendida"
                            "EN_ATENCION" -> "En Box"
                            "CANCELADA" -> "Cancelada"
                            else -> "Agendada"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (cita.estado) {
                            "EN_ESPERA" -> StatusAmber
                            "ATENDIDA" -> StatusGreen
                            "EN_ATENCION" -> StatusBlue
                            "CANCELADA" -> StatusRed
                            else -> Color(0xFF475569)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(cita.paciente?.nombreCompleto ?: "Paciente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "DUI: ${cita.paciente?.dui ?: "N/D"} · Exp: ${cita.paciente?.numeroExpediente ?: "N/D"}",
                    fontSize = 11.sp,
                    color = CmoTextSoft
                )
                Text(
                    text = cita.medicoNombre ?: "Dr. Alejandro Restrepo",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CmoPrimary
                )
            }

            cita.motivo?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, fontSize = 12.sp, color = CmoTextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones de acción según estado
            when (cita.estado) {
                "AGENDADA" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onLlegada,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CmoPrimary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Registrar Llegada", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onCancelar,
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancelar", fontSize = 11.sp, color = StatusRed)
                        }
                    }
                }
                "EN_ESPERA" -> {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onTomarSignos,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = StatusRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("🩺 Triaje / Signos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onPasarDirectoAConsulta,
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CmoPrimary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("🚀 Pasar a Consulta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = onCancelar,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancelar Cita", fontSize = 11.sp, color = StatusRed)
                        }
                    }
                }
                "EN_ATENCION" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusBlueBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = StatusBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paciente actualmente en consulta con el doctor", fontSize = 11.sp, color = StatusBlue, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de Triaje de Signos Vitales para Recepcionista / Enfermería.
 * No es obligatorio llenar todos los campos (apoya emergencias o pase directo).
 * Permite guardar signos y pasar a consulta con un solo toque.
 */
@Composable
fun DialogoTriajeCitaRecepcion(
    cita: Cita,
    onDismiss: () -> Unit,
    onGuardarSignos: (SignosVitales?, Boolean) -> Unit
) {
    var sistolica by remember { mutableStateOf("") }
    var diastolica by remember { mutableStateOf("") }
    var frecCardiaca by remember { mutableStateOf("") }
    var frecResp by remember { mutableStateOf("") }
    var temperatura by remember { mutableStateOf("") }
    var saturacion by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var talla by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }

    val imcCalculado = remember(peso, talla) {
        val p = peso.toDoubleOrNull()
        val t = talla.toDoubleOrNull()
        if (p != null && t != null && t > 0) {
            val hMetros = if (t > 3) t / 100.0 else t
            p / (hMetros * hMetros)
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Triaje y Signos Vitales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${cita.paciente?.nombreCompleto ?: "Paciente"} · Exp: ${cita.paciente?.numeroExpediente ?: "N/D"}",
                    fontSize = 12.sp,
                    color = CmoPrimary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CmoSurfaceContainerLow
                    ) {
                        Text(
                            "ℹ️ La toma de signos vitales es opcional y flexible. En una emergencia puede pasar directamente a consulta y el doctor podrá registrarlos.",
                            fontSize = 11.sp,
                            color = CmoTextSoft,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                item {
                    Text("Presión Arterial (mmHg):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sistolica,
                            onValueChange = { sistolica = it },
                            label = { Text("Sistólica") },
                            placeholder = { Text("120") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = diastolica,
                            onValueChange = { diastolica = it },
                            label = { Text("Diastólica") },
                            placeholder = { Text("80") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = frecCardiaca,
                            onValueChange = { frecCardiaca = it },
                            label = { Text("FC (lpm)") },
                            placeholder = { Text("75") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = frecResp,
                            onValueChange = { frecResp = it },
                            label = { Text("FR (rpm)") },
                            placeholder = { Text("18") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = temperatura,
                            onValueChange = { temperatura = it },
                            label = { Text("Temp (°C)") },
                            placeholder = { Text("36.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = saturacion,
                            onValueChange = { saturacion = it },
                            label = { Text("SpO2 (%)") },
                            placeholder = { Text("98") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = peso,
                            onValueChange = { peso = it },
                            label = { Text("Peso (kg)") },
                            placeholder = { Text("70") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = talla,
                            onValueChange = { talla = it },
                            label = { Text("Talla (cm)") },
                            placeholder = { Text("170") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                if (imcCalculado != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusBlueBg
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("IMC Calculado:", fontSize = 11.sp, color = StatusBlue)
                                Text(
                                    String.format(java.util.Locale.US, "%.1f kg/m²", imcCalculado),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusBlue
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = obs,
                        onValueChange = { obs = it },
                        label = { Text("Observaciones de enfermería") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val signos = if (sistolica.isNotBlank() || frecCardiaca.isNotBlank() || temperatura.isNotBlank()) {
                        SignosVitales(
                            presionSistolica = sistolica.toIntOrNull(),
                            presionDiastolica = diastolica.toIntOrNull(),
                            frecuenciaCardiaca = frecCardiaca.toIntOrNull(),
                            frecuenciaRespiratoria = frecResp.toIntOrNull(),
                            temperaturaC = temperatura.toDoubleOrNull(),
                            saturacionOxigeno = saturacion.toIntOrNull(),
                            pesoKg = peso.toDoubleOrNull(),
                            tallaCm = talla.toDoubleOrNull(),
                            observaciones = obs.ifBlank { null }
                        )
                    } else null
                    onGuardarSignos(signos, true)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CmoPrimary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("🚀 Pasar a Consulta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
                TextButton(
                    onClick = {
                        val signos = if (sistolica.isNotBlank() || frecCardiaca.isNotBlank() || temperatura.isNotBlank()) {
                            SignosVitales(
                                presionSistolica = sistolica.toIntOrNull(),
                                presionDiastolica = diastolica.toIntOrNull(),
                                frecuenciaCardiaca = frecCardiaca.toIntOrNull(),
                                frecuenciaRespiratoria = frecResp.toIntOrNull(),
                                temperaturaC = temperatura.toDoubleOrNull(),
                                saturacionOxigeno = saturacion.toIntOrNull(),
                                pesoKg = peso.toDoubleOrNull(),
                                tallaCm = talla.toDoubleOrNull(),
                                observaciones = obs.ifBlank { null }
                            )
                        } else null
                        onGuardarSignos(signos, false)
                    }
                ) {
                    Text("Solo Guardar Signos")
                }
            }
        }
    )
}

// =================================================================
// TAB 2: ADMISIÓN & CAMAS (17 CAMAS + TRIAJE SIGNOS VITALES)
// =================================================================
@Composable
fun AdmisionCamasTab(
    camas: List<Cama>,
    onRegistrarEIngresar: (Paciente, SignosVitales?, String?, String, Boolean) -> Unit
) {
    // Filiación
    var nombre by remember { mutableStateOf("") }
    var dui by remember { mutableStateOf("") }
    var fechaNac by remember { mutableStateOf("1992-06-18") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var contactoEmergencia by remember { mutableStateOf("") }
    var motivoIngreso by remember { mutableStateOf("") }
    var protegido by remember { mutableStateOf(false) }

    // Triaje de Signos Vitales (Opcionales y Flexibles)
    var paseEmergenciaDirecto by remember { mutableStateOf(false) }
    var sistolica by remember { mutableStateOf("") }
    var diastolica by remember { mutableStateOf("") }
    var frecCardiaca by remember { mutableStateOf("") }
    var frecResp by remember { mutableStateOf("") }
    var temperatura by remember { mutableStateOf("") }
    var saturacion by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var talla by remember { mutableStateOf("") }
    var obsVitals by remember { mutableStateOf("") }

    // Selección de Cama
    var camaSeleccionada by remember { mutableStateOf<String?>(null) }

    // Cálculo dinámico de edad a partir de fechaNac
    val edadCalculada = remember(fechaNac) {
        try {
            val parts = fechaNac.split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull() ?: 1990
                val m = parts[1].toIntOrNull() ?: 1
                val d = parts[2].toIntOrNull() ?: 1
                java.time.Period.between(java.time.LocalDate.of(y, m, d), java.time.LocalDate.now()).years.coerceAtLeast(0)
            } else 0
        } catch (_: Exception) { 0 }
    }

    // Cálculo dinámico de IMC
    val imcCalculado = remember(peso, talla) {
        val p = peso.toDoubleOrNull() ?: 0.0
        val t = talla.toDoubleOrNull() ?: 0.0
        if (p > 0 && t > 0) {
            val m = t / 100.0
            Math.round((p / (m * m)) * 10.0) / 10.0
        } else null
    }

    val totalCamas = 17
    val ocupadas = camas.count { it.estado == "OCUPADA" }
    val disponibles = totalCamas - ocupadas

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Switch de anonimización / Ley de datos
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = CmoPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Ley de Protección de Datos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoText)
                            Text("Anonimizar en monitores públicos", fontSize = 11.sp, color = CmoTextSoft)
                        }
                    }
                    Switch(checked = protegido, onCheckedChange = { protegido = it })
                }
            }
        }

        // Paso 1: Filiación
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paso 1: Datos de Filiación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre y Apellidos completos") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dui,
                            onValueChange = { dui = it },
                            label = { Text("DUI") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fechaNac,
                            onValueChange = { fechaNac = it },
                            label = { Text("Fecha Nac. (AAAA-MM-DD)") },
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Edad calculada: $edadCalculada años",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CmoPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contactoEmergencia,
                        onValueChange = { contactoEmergencia = it },
                        label = { Text("Acompañante / Contacto de Emergencia") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = motivoIngreso,
                        onValueChange = { motivoIngreso = it },
                        label = { Text("Motivo principal de ingreso") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Paso 2: Triaje Clínico (Toma de Signos Vitales - Flexibles)
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
                            Text("Paso 2: Triaje (Signos Vitales)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                            Text("Campos flexibles y opcionales", fontSize = 11.sp, color = CmoTextSoft)
                        }

                        // Switch pase directo emergencia
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (paseEmergenciaDirecto) StatusRedBg else CmoSurfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { paseEmergenciaDirecto = !paseEmergenciaDirecto }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🚨 Emergencia Directa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (paseEmergenciaDirecto) StatusRed else CmoTextMuted)
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(
                                    checked = paseEmergenciaDirecto,
                                    onCheckedChange = { paseEmergenciaDirecto = it },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }
                        }
                    }

                    if (paseEmergenciaDirecto) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusAmberBg
                        ) {
                            Text(
                                text = "El paciente pasará de inmediato a Box de Atención. El Doctor completará los signos vitales directamente en la consulta médica.",
                                fontSize = 11.sp,
                                color = StatusAmber,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sistolica,
                            onValueChange = { sistolica = it },
                            label = { Text("PA Sist. (mmHg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = diastolica,
                            onValueChange = { diastolica = it },
                            label = { Text("PA Diast. (mmHg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = frecCardiaca,
                            onValueChange = { frecCardiaca = it },
                            label = { Text("FC (lpm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = frecResp,
                            onValueChange = { frecResp = it },
                            label = { Text("FR (rpm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = temperatura,
                            onValueChange = { temperatura = it },
                            label = { Text("Temp (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = saturacion,
                            onValueChange = { saturacion = it },
                            label = { Text("SpO2 (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = peso,
                            onValueChange = { peso = it },
                            label = { Text("Peso (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = talla,
                            onValueChange = { talla = it },
                            label = { Text("Talla (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (imcCalculado != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CmoPrimarySoft
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("IMC: $imcCalculado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoPrimaryDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (imcCalculado) {
                                        in 0.0..<18.5 -> "Bajo peso"
                                        in 18.5..<25.0 -> "Normal"
                                        in 25.0..<30.0 -> "Sobrepeso"
                                        else -> "Obesidad"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CmoPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = obsVitals,
                        onValueChange = { obsVitals = it },
                        label = { Text("Signos de alerta / Observaciones de triaje") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Paso 3: Asignación Estricta de Camas (17 Camas: 5 VIP, 12 Semiprivadas)
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
                        Text("Paso 3: Cama Hospitalaria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = if (disponibles > 0) StatusGreenBg else StatusRedBg
                        ) {
                            Text(
                                text = "$ocupadas / $totalCamas Ocupadas ($disponibles libres)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (disponibles > 0) StatusGreen else StatusRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Planta 3: VIP Individual (VIP-301 a VIP-305)
                    Text("3ª Planta · VIP Individuales (5 camas)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        camas.filter { it.codigo.startsWith("VIP") }.forEach { cama ->
                            val isSelected = camaSeleccionada == cama.codigo
                            val isDisp = cama.estado == "DISPONIBLE"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CmoPrimary else if (isDisp) CmoSurfaceContainer else StatusRedBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = isDisp) { camaSeleccionada = cama.codigo }
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(cama.codigo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else CmoText)
                                    Text(if (isDisp) "Libre" else "Ocup.", fontSize = 9.sp, color = if (isSelected) Color.White else if (isDisp) StatusGreen else StatusRed)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Planta 2: Semiprivadas (HAB-201-A/B a HAB-206-A/B)
                    Text("2ª Planta · Semiprivadas (12 camas A/B)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    val semis = camas.filter { it.codigo.startsWith("HAB") }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 0 until semis.size step 4) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (j in 0 until 4) {
                                    val idx = i + j
                                    if (idx < semis.size) {
                                        val cama = semis[idx]
                                        val isSelected = camaSeleccionada == cama.codigo
                                        val isDisp = cama.estado == "DISPONIBLE"
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) CmoPrimary else if (isDisp) CmoSurfaceContainer else StatusRedBg,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable(enabled = isDisp) { camaSeleccionada = cama.codigo }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(cama.codigo, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else CmoText)
                                                Text(if (isDisp) "Libre" else "Ocup.", fontSize = 9.sp, color = if (isSelected) Color.White else if (isDisp) StatusGreen else StatusRed)
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    camaSeleccionada?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cama seleccionada: $it", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CmoPrimary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (nombre.isNotBlank()) {
                                val vitals = SignosVitales(
                                    presionSistolica = sistolica.toIntOrNull(),
                                    presionDiastolica = diastolica.toIntOrNull(),
                                    frecuenciaCardiaca = frecCardiaca.toIntOrNull(),
                                    frecuenciaRespiratoria = frecResp.toIntOrNull(),
                                    temperaturaC = temperatura.toDoubleOrNull(),
                                    saturacionOxigeno = saturacion.toIntOrNull(),
                                    pesoKg = peso.toDoubleOrNull(),
                                    tallaCm = talla.toDoubleOrNull(),
                                    observaciones = obsVitals.ifBlank { null }
                                )
                                val pac = Paciente(
                                    nombreCompleto = nombre,
                                    fechaNacimiento = fechaNac,
                                    dui = dui,
                                    telefono = telefono,
                                    contactoEmergencia = contactoEmergencia,
                                    protegido = protegido
                                )
                                onRegistrarEIngresar(pac, vitals, camaSeleccionada, motivoIngreso, paseEmergenciaDirecto)
                                nombre = ""
                                dui = ""
                                motivoIngreso = ""
                                camaSeleccionada = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (paseEmergenciaDirecto) StatusRed else CmoPrimary)
                    ) {
                        Text(
                            text = if (paseEmergenciaDirecto) "🚨 CONFIRMAR PASE DIRECTO A BOX" else "Completar Registro e Ingreso",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// =================================================================
// TAB 3: AMBULANCIA ALFA-1 & TURNOS DEL PERSONAL
// =================================================================
@Composable
fun AmbulanciaTurnosTab(
    ambulancia: AmbulanciaRecurso,
    personalGuardia: List<PersonalGuardia>,
    onCambiarEstado: (String) -> Unit,
    onDespacho: (String, String, String, String) -> Unit
) {
    var mostrarModalDespacho by remember { mutableStateOf(false) }
    var despachoDir by remember { mutableStateOf("") }
    var despachoPac by remember { mutableStateOf("") }
    var despachoDesc by remember { mutableStateOf("") }
    var despachoPrio by remember { mutableStateOf("CRITICA") }

    var filtroRol by remember { mutableStateOf("TODOS") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Estado de Ambulancia
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Emergency, contentDescription = null, tint = StatusRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Ambulancia Alfa-1", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                                Text("Recurso Único Móvil · ${ambulancia.placa}", fontSize = 11.sp, color = CmoTextSoft)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = when (ambulancia.estado) {
                                "DISPONIBLE" -> StatusGreenBg
                                "EN_SERVICIO" -> StatusAmberBg
                                else -> StatusRedBg
                            }
                        ) {
                            Text(
                                text = ambulancia.estado,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (ambulancia.estado) {
                                    "DISPONIBLE" -> StatusGreen
                                    "EN_SERVICIO" -> StatusAmber
                                    else -> StatusRed
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de estados operativos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DISPONIBLE", "EN_SERVICIO", "EN_MANTENIMIENTO").forEach { est ->
                            val sel = ambulancia.estado == est
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = if (sel) CmoPrimary else CmoSurfaceContainerLow,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCambiarEstado(est) }
                            ) {
                                Text(
                                    text = if (est == "EN_MANTENIMIENTO") "Mantenim." else est.replace("_", " "),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else CmoTextMuted,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Misión activa si está en servicio
                    ambulancia.servicioActivo?.let { srv ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusRedBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🚨 MISIÓN EN CURSO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                    Text("ETA: ${srv.minutosLlegada ?: 15} min", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(srv.descripcion ?: "Traslado de emergencia", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CmoText)
                                Text("Destino: ${srv.destino}", fontSize = 11.sp, color = CmoTextMuted)
                                Text("Tripulación: ${srv.tripulacionParamedico} / ${srv.tripulacionChofer}", fontSize = 11.sp, color = CmoTextSoft)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { mostrarModalDespacho = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SOLICITAR DESPACHO DE EMERGENCIA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Control de Turnos del Personal en Guardia
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CmoSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Turnos Activos del Personal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CmoText)
                    Text("Horarios y personal de guardia hospitalaria", fontSize = 11.sp, color = CmoTextSoft)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtros rápidos
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("TODOS", "MEDICO", "RECEPCIONISTA")) { rol ->
                            val sel = filtroRol == rol
                            FilterChip(
                                selected = sel,
                                onClick = { filtroRol = rol },
                                label = { Text(if (rol == "TODOS") "Todos" else if (rol == "MEDICO") "Doctores" else "Recepción & Asistentes", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filtrados = if (filtroRol == "TODOS") personalGuardia else personalGuardia.filter { it.rol == filtroRol }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filtrados.forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CmoSurfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(p.nombreCompleto, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CmoText)
                                        Text(p.cargo, fontSize = 11.sp, color = CmoPrimary)
                                        Text("${p.horaInicio} - ${p.horaFin} (${p.area})", fontSize = 11.sp, color = CmoTextSoft)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(shape = RoundedCornerShape(99.dp), color = CmoPrimaryFixed) {
                                            Text(p.estado, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CmoPrimaryDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        p.relevoEn?.let {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Relevo: $it", fontSize = 10.sp, color = StatusRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    if (mostrarModalDespacho) {
        AlertDialog(
            onDismissRequest = { mostrarModalDespacho = false },
            title = { Text("Despacho de Emergencia", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = despachoDir, onValueChange = { despachoDir = it }, label = { Text("Dirección / Ubicación") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = despachoPac, onValueChange = { despachoPac = it }, label = { Text("Nombre del paciente o referencia") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = despachoDesc, onValueChange = { despachoDesc = it }, label = { Text("Descripción de la emergencia") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (despachoDir.isNotBlank()) {
                            onDespacho(despachoDir, despachoPac, despachoDesc, despachoPrio)
                            mostrarModalDespacho = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("DESPACHAR ALFA-1")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalDespacho = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun anonimizarNombre(nombre: String): String {
    val parts = nombre.trim().split(" ")
    return if (parts.size >= 2) {
        val p1 = parts[0].firstOrNull()?.uppercase() ?: "P"
        val p2 = parts[1].firstOrNull()?.uppercase() ?: "A"
        "$p1. $p2**** (Protegido)"
    } else "$nombre (Protegido)"
}
fun Modifier.scale(scale: Float): Modifier = this
