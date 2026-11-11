import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { MainLayout } from '../components/layout/MainLayout';
import { LoginPage } from '../pages/auth/LoginPage';
import { DashboardPage } from '../pages/dashboard/DashboardPage';
import { UsersPage } from '../pages/users/UsersPage';
import { PatientsPage } from '../pages/patients/PatientsPage';
import { PublicPortalPage } from '../pages/public/PublicPortalPage';
import { BedsPage } from '../pages/beds/BedsPage';
import { AmbulancePage } from '../pages/ambulance/AmbulancePage';
import { ShiftsPage } from '../pages/shifts/ShiftsPage';
import { AppointmentsPage } from '../pages/appointments/AppointmentsPage';
import { LaboratoryPage } from '../pages/laboratory/LaboratoryPage';
import { InventoryPage } from '../pages/inventory/InventoryPage';
import { NoticesPage } from '../pages/notices/NoticesPage';

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
            <Route path="/camas" element={<BedsPage />} />
            <Route path="/ambulancia" element={<AmbulancePage />} />

            {/* Personal y Turnos */}
            <Route path="/usuarios" element={<UsersPage />} />
            <Route path="/turnos" element={<ShiftsPage />} />

            {/* Gestión Clínica */}
            <Route path="/pacientes" element={<PatientsPage />} />
            <Route path="/citas" element={<AppointmentsPage />} />
            <Route path="/laboratorio" element={<LaboratoryPage />} />

            {/* Logística & Avisos */}
            <Route path="/inventario" element={<InventoryPage />} />
            <Route path="/avisos" element={<NoticesPage />} />
          </Route>
        </Route>

        {/* Redirección por defecto */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
};
