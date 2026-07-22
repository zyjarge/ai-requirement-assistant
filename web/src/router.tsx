import { createBrowserRouter, Navigate } from 'react-router-dom';
import ResponsiveLayout from '@/components/layout/ResponsiveLayout';
import RequirementsListPage from '@/pages/RequirementsListPage';
import RequirementDetailPage from '@/pages/RequirementDetailPage';
import NewRequirementPage from '@/pages/NewRequirementPage';
import DashboardPage from '@/pages/DashboardPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <ResponsiveLayout />,
    children: [
      { index: true, element: <Navigate to="/requirements" replace /> },
      { path: 'requirements', element: <RequirementsListPage /> },
      { path: 'requirements/new', element: <NewRequirementPage /> },
      { path: 'requirements/:id', element: <RequirementDetailPage /> },
      { path: 'dashboard', element: <DashboardPage /> },
      { path: '*', element: <Navigate to="/requirements" replace /> },
    ],
  },
], {
  basename: '/admin/app',
});
