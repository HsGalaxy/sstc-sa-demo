import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './app/AuthContext'
import { AppShell } from './components/AppShell'
import { Empty } from './components/ui'
import { LoginPage } from './pages/LoginPage'
import { OverviewPage } from './pages/OverviewPage'
import { ArchitecturePage } from './pages/ArchitecturePage'
import { BoardPage } from './pages/BoardPage'
import { FileTrackerPage } from './pages/FileTrackerPage'
import { OkrPage } from './pages/OkrPage'
import { OperationLogPage } from './pages/OperationLogPage'
import { CollaborationPage } from './pages/CollaborationPage'
import { MeetingsPage } from './pages/MeetingsPage'
import { InboxPage } from './pages/InboxPage'
import { DocumentsPage } from './pages/toolbox/DocumentsPage'
import { PresentationsPage } from './pages/toolbox/PresentationsPage'
import { ReposPage } from './pages/toolbox/ReposPage'
import { EnvironmentsPage } from './pages/toolbox/EnvironmentsPage'
import { ProjectFilesPage } from './pages/toolbox/ProjectFilesPage'
import { UsersPage } from './pages/identity/UsersPage'
import { PoliciesPage } from './pages/identity/PoliciesPage'
import { AccessAuditPage } from './pages/identity/AccessAuditPage'

function RequireAuth({ children }: { children: JSX.Element }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) {
    return (
      <div className="content">
        <div className="skeleton" style={{ height: 140 }} />
      </div>
    )
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<OverviewPage />} />
        <Route path="architecture" element={<ArchitecturePage />} />
        <Route path="tasks" element={<BoardPage />} />
        <Route path="files" element={<FileTrackerPage />} />
        <Route path="okr" element={<OkrPage />} />
        <Route path="operations" element={<OperationLogPage />} />
        <Route path="collaboration" element={<CollaborationPage />} />
        <Route path="meetings" element={<MeetingsPage />} />
        <Route path="inbox" element={<InboxPage />} />
        <Route path="toolbox/documents" element={<DocumentsPage />} />
        <Route path="toolbox/presentations" element={<PresentationsPage />} />
        <Route path="toolbox/repos" element={<ReposPage />} />
        <Route path="toolbox/environments" element={<EnvironmentsPage />} />
        <Route path="toolbox/files" element={<ProjectFilesPage />} />
        <Route path="identity/users" element={<UsersPage />} />
        <Route path="identity/policies" element={<PoliciesPage />} />
        <Route path="identity/audit" element={<AccessAuditPage />} />
        <Route path="*" element={<Empty>Page not found.</Empty>} />
      </Route>
    </Routes>
  )
}
