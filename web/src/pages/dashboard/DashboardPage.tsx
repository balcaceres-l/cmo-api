import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { isJefeOperaciones } from '../../utils/roles';
import { OperationsDashboard } from './OperationsDashboard';
import { AdminDashboard } from './AdminDashboard';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  if (isJefeOperaciones(user)) {
    return <OperationsDashboard />;
  }

  return <AdminDashboard />;
};
