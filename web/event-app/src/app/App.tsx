import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AdminPreparationPage } from "../features/admin/AdminPreparationPage";
import { AdminUserDetailPage } from "../features/admin/AdminUserDetailPage";
import { RequireAdmin } from "../features/admin/RequireAdmin";
import { AccountPage } from "../features/account/AccountPage";
import { ForbiddenPage } from "../features/authentication/ForbiddenPage";
import { ForgotPasswordPage } from "../features/authentication/ForgotPasswordPage";
import { GuestHomePage } from "../features/authentication/GuestHomePage";
import { NotFoundPage } from "../features/authentication/NotFoundPage";
import { RequireAuthenticated } from "../features/authentication/RequireAuthenticated";
import { SignInPage } from "../features/authentication/SignInPage";
import { SignUpPage } from "../features/authentication/SignUpPage";
import { VerifyEmailPage } from "../features/authentication/VerifyEmailPage";
import { CreatorProfilePage } from "../features/creators/CreatorProfilePage";
import { CreatorsDirectoryPage } from "../features/creators/CreatorsDirectoryPage";
import { EventShell } from "../shared/layout/EventShell";
import "../shared/styles/global.css";
import { AppProviders } from "./AppProviders";

export function AppRoutes() {
  return (
    <EventShell>
      <Routes>
        <Route path="/" element={<GuestHomePage />} />
        <Route path="/creators" element={<CreatorsDirectoryPage />} />
        <Route path="/creators/:slug" element={<CreatorProfilePage />} />
        <Route path="/sign-in" element={<SignInPage />} />
        <Route path="/sign-up" element={<SignUpPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route
          path="/account"
          element={
            <RequireAuthenticated>
              <AccountPage />
            </RequireAuthenticated>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireAuthenticated>
              <RequireAdmin>
                <AdminPreparationPage />
              </RequireAdmin>
            </RequireAuthenticated>
          }
        />
        <Route
          path="/admin/users/:userId"
          element={
            <RequireAuthenticated>
              <RequireAdmin>
                <AdminUserDetailPage />
              </RequireAdmin>
            </RequireAuthenticated>
          }
        />
        <Route path="/forbidden" element={<ForbiddenPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </EventShell>
  );
}

export function App() {
  return (
    <AppProviders>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AppProviders>
  );
}
