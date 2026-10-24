package com.cmo.hospital.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmo.hospital.data.model.Cita
import com.cmo.hospital.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(
    viewModel: DoctorDashboardViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf("agenda") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CmoPrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.user?.nombreCompleto?.firstOrNull()?.uppercase() ?: "D",
                                color = CmoPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = uiState.user?.nombreCompleto ?: "Dr. Roberto Mendoza",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = uiState.user?.cargo ?: "Médico de Turno — Consulta General",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CmoBackgroundDark
                )
            )
        },
        containerColor = CmoBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info: Guardia / Estado
            item {
                Spacer(modifier = Modifier.height(4.dp))
                GuardiaBanner(ambulanciaEstado = uiState.summary.ambulanciaEstado)
            }

            // Metric Summary Cards
            item {
                Text(
                    text = "Resumen de Turno",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CmoPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Citas Hoy",
                        value = "${uiState.summary.citasHoy}",
                        subtitle = "${uiState.summary.pacientesAtendidos} atendidas",
                        icon = Icons.Default.CalendarToday,
                        color = CmoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Laboratorio",
                        value = "${uiState.summary.ordenesLaboratorio}",
                        subtitle = "órdenes activas",
                        icon = Icons.Default.Science,
                        color = StatusBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Access Tabs
            item {
                Text(
                    text = "Módulos Clínicos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CmoPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(
                        "agenda" to ("Agenda del Día" to Icons.AutoMirrored.Filled.EventNote),
                        "pacientes" to ("Expedientes" to Icons.Default.FolderShared),
                        "laboratorio" to ("Laboratorio" to Icons.Default.Science),
                        "turnos" to ("Mis Horarios" to Icons.Default.Schedule)
                    )) { (key, pair) ->
                        val (label, icon) = pair
                        FilterChip(
                            selected = selectedTab == key,
                            onClick = { selectedTab = key },
                            label = { Text(label, fontSize = 12.sp) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CmoPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
            }

            // Section: Agenda del Día
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pacientes Citados Hoy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CmoPrimaryDark
                    )
                    TextButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Actualizar", fontSize = 12.sp, color = CmoPrimary)
                    }
                }
            }

            // Citas List
            if (uiState.agendaList.isEmpty() && !uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CmoSurface)
                    ) {
                        Text(
                            text = "No hay pacientes programados para hoy en tu agenda.",
                            color = CmoTextMuted,
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(uiState.agendaList) { cita ->
                    CitaCard(cita = cita)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun GuardiaBanner(ambulanciaEstado: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CmoPrimaryDark)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "GUARDIA ACTIVA — PLANTA 1",
                    color = CmoTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Consultorios Médicos (C-101)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // Ambulance status chip
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = when (ambulanciaEstado) {
                    "DISPONIBLE" -> StatusGreenBg
                    "EN_SERVICIO" -> StatusAmberBg
                    else -> StatusRedBg
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚑 $ambulanciaEstado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (ambulanciaEstado) {
                            "DISPONIBLE" -> StatusGreen
                            "EN_SERVICIO" -> StatusAmber
                            else -> StatusRed
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CmoSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CmoTextMuted
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CmoText
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = CmoTextSoft
            )
        }
    }
}

@Composable
fun CitaCard(cita: Cita) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CmoSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Hora y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CmoPrimarySoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cita.horaInicio,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CmoPrimary
                        )
                    }
                    cita.consultorio?.codigo?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = CmoTextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Estado Chip
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = when (cita.estado) {
                        "EN_ESPERA" -> StatusAmberBg
                        "ATENDIDA" -> StatusGreenBg
                        "EN_ATENCION" -> StatusBlueBg
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        text = when (cita.estado) {
                            "EN_ESPERA" -> "En Espera"
                            "ATENDIDA" -> "Atendida"
                            "EN_ATENCION" -> "En Consulta"
                            else -> "Agendada"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (cita.estado) {
                            "EN_ESPERA" -> StatusAmber
                            "ATENDIDA" -> StatusGreen
                            "EN_ATENCION" -> StatusBlue
                            else -> Color(0xFF475569)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nombre del paciente
            Text(
                text = cita.paciente?.nombreCompleto ?: "Paciente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CmoText
            )

            // Expediente y DUI
            cita.paciente?.numeroExpediente?.let { exp ->
                Text(
                    text = "Exp: $exp · DUI: ${cita.paciente.dui ?: "N/D"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CmoTextSoft
                )
            }

            // Motivo
            cita.motivo?.let { motivo ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = motivo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CmoTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de acción clínica
            Button(
                onClick = { /* Abrir Consulta Clínica */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (cita.estado == "ATENDIDA") CmoPrimarySoft else CmoPrimary,
                    contentColor = if (cita.estado == "ATENDIDA") CmoPrimary else Color.White
                )
            ) {
                Text(
                    text = if (cita.estado == "ATENDIDA") "Ver Expediente e Historial" else "Iniciar Consulta Médica",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
