import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AccountPage } from "../features/account/AccountPage";
import { ForbiddenPage } from "../features/authentication/ForbiddenPage";
import { GuestHomePage } from "../features/authentication/GuestHomePage";
import { NotFoundPage } from "../features/authentication/NotFoundPage";
import { RequireAuthenticated } from "../features/authentication/RequireAuthenticated";
import { SignInPage } from "../features/authentication/SignInPage";
import { EventShell } from "../shared/layout/EventShell";
import "../shared/styles/global.css";
import { AppProviders } from "./AppProviders";

export function AppRoutes() {
  return (
    <EventShell>
      <Routes>
        <Route path="/" element={<GuestHomePage />} />
        <Route path="/sign-in" element={<SignInPage />} />
        <Route
          path="/account"
          element={
            <RequireAuthenticated>
              <AccountPage />
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
