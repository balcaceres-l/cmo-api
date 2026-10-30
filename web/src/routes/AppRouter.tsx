import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { MainLayout } from '../components/layout/MainLayout';
import { LoginPage } from '../pages/auth/LoginPage';
import { DashboardPage } from '../pages/dashboard/DashboardPage';
import { UsersPage } from '../pages/users/UsersPage';
import { PatientsPage } from '../pages/patients/PatientsPage';
import { PublicPortalPage } from '../pages/public/PublicPortalPage';
import { ModulePlaceholder } from '../pages/common/ModulePlaceholder';

export const AppRouter: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Ruta pública del Hospital (HU-69) */}
        <Route path="/publico" element={<PublicPortalPage />} />

        {/* Ruta pública de Login (HU-24) */}
        <Route path="/login" element={<LoginPage />} />

        {/* Rutas Administrativas Protegidas */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />

            {/* Operaciones */}
            <Route
              path="/camas"
              element={
                <ModulePlaceholder
                  title="Gestión de Camas e Ingresos"
                  hu="HU-55 / HU-56"
                  icon="🛏️"
                  description="Mapa de camas en tiempo real para el control de ocupación, altas, ingresos y traslados sin sobrecupo."
                  rules={[
                    '3ª Planta: Exactamente 5 Habitaciones VIP individuales (VIP-301 a VIP-305).',
                    '2ª Planta: Exactamente 6 Habitaciones Semiprivadas (HAB-201 a HAB-206) = 12 camas.',
                    'Capacidad total del hospital: 17 camas.',
                    'Control estricto de concurrencia: El sistema no permite sobrecupo ni doble asignación de camas (Error 409).',
                    'Sincronización en tiempo real con la app Android de recepción vía Reverb / Polling.',
                  ]}
                />
              }
            />

            <Route
              path="/ambulancia"
              element={
                <ModulePlaceholder
                  title="Control de la Ambulancia en Tiempo Real"
                  hu="HU-61 / HU-62"
                  icon="🚑"
                  description="Módulo de despacho, seguimiento en vivo de emergencias, bitácora y control de mantenimiento de la unidad móvil."
                  rules={[
                    'El hospital cuenta únicamente con 1 Ambulancia privada.',
                    'Estados del recurso: DISPONIBLE, EN_SERVICIO, EN_MANTENIMIENTO.',
                    'No se permite pasar a mantenimiento si existe un servicio de emergencia en curso (Error 409).',
                    'Recepción de reportes de traslado enviados por paramédicos en camino.',
                    'Bitácora filtrable por rango de fechas, prioridad y estado.',
                  ]}
                />
              }
            />

            {/* Personal (HU-30) */}
            <Route path="/usuarios" element={<UsersPage />} />

            <Route
              path="/turnos"
              element={
                <ModulePlaceholder
                  title="Programación y Control de Turnos"
                  hu="HU-64"
                  icon="📅"
                  description="Cuadrante semanal de guardia y turnos para doctores, enfermeras, recepcionistas y personal operativo."
                  rules={[
                    'Grilla semanal visual por persona, cargo y área médica.',
                    'Soporte para turnos que cruzan la medianoche (ej. 22:00 a 06:00).',
                    'Prevención de solapes de horarios para una misma persona (Error 409).',
                  ]}
                />
              }
            />

            {/* Clínico: Pacientes (HU-31 / HU-44) */}
            <Route path="/pacientes" element={<PatientsPage />} />

            <Route
              path="/citas"
              element={
                <ModulePlaceholder
                  title="Supervisión de Citas Médicas"
                  hu="HU-38"
                  icon="🩺"
                  description="Supervisión de la agenda hospitalaria, confirmación de solicitudes de pacientes y reprogramación."
                  rules={[
                    'Filtros por fecha, médico especialista y estado de la cita.',
                    'Transiciones válidas: SOLICITADA → AGENDADA → EN_ESPERA → EN_ATENCION → ATENDIDA.',
                    'Confirmación de solicitudes de atención con asignación de consultorio (C-101 a C-104).',
                  ]}
                />
              }
            />

            <Route
              path="/laboratorio"
              element={
                <ModulePlaceholder
                  title="Laboratorio Clínico"
                  hu="HU-49"
                  icon="🧪"
                  description="Monitoreo y avance de las órdenes de análisis clínicos emitidas por los médicos."
                  rules={[
                    'Flujo estricto de tres estados: PENDIENTE → EN_PROCESO → COMPLETADO.',
                    'El sistema gestiona estados y trazabilidad temporal, sin valores médicos complejos ni PDFs inventados.',
                    'Sincronización con las órdenes solicitadas por los médicos desde iOS/Android.',
                  ]}
                />
              }
            />

            {/* Logística & Avisos */}
            <Route
              path="/inventario"
              element={
                <ModulePlaceholder
                  title="Recursos e Inventario"
                  hu="HU-75"
                  icon="📦"
                  description="Control de stock de medicamentos, insumos hospitalarios y equipos médicos con registro de movimientos."
                  rules={[
                    'Clasificación por categorías: Medicamento, Insumo, Equipo.',
                    'Control estricto de existencias: No se permiten salidas que dejen el stock en negativo (Error 409).',
                    'Alertas visuales automáticas para ítems que alcanzan o caen bajo el stock mínimo.',
                    'Un recurso con movimientos registrados no puede ser eliminado del catálogo.',
                  ]}
                />
              }
            />

            <Route
              path="/avisos"
              element={
                <ModulePlaceholder
                  title="Avisos y Comunicados del Hospital"
                  hu="HU-70"
                  icon="📢"
                  description="Publicación de boletines y avisos institucionales para el público o de circulación interna del personal."
                  rules={[
                    'Distinción de audiencia: Avisos Públicos (visibles en el portal de pacientes) vs. Avisos Internos (personal de salud).',
                    'Vigencia temporal: Los avisos caducados se ocultan automáticamente.',
                  ]}
                />
              }
            />
          </Route>
        </Route>

        {/* Redirección por defecto */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
};
