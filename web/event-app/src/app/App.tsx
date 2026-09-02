import { BrowserRouter, Route, Routes } from "react-router-dom";
import "../shared/styles/global.css";
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
import { ContentDetailPage } from "../features/feed/ContentDetailPage";
import { LikedContentPage, SavedContentPage } from "../features/feed/ContentCollectionPage";
import { FeedPage } from "../features/feed/FeedPage";
import "../features/feed/content.css";
import { MyUploadsPage } from "../features/uploads/MyUploadsPage";
import { RequireCreator } from "../features/uploads/RequireCreator";
import { UploadPage } from "../features/uploads/UploadPage";
import "../features/uploads/uploads.css";
import { EventShell } from "../shared/layout/EventShell";
import { AppProviders } from "./AppProviders";

export function AppRoutes() {
  return (
    <EventShell>
      <Routes>
        <Route path="/" element={<FeedPage />} />
        <Route path="/guest" element={<GuestHomePage />} />
        <Route path="/content/:contentId" element={<ContentDetailPage />} />
        <Route path="/creators" element={<CreatorsDirectoryPage />} />
        <Route path="/creators/:slug" element={<CreatorProfilePage />} />
        <Route
          path="/liked"
          element={
            <RequireAuthenticated>
              <LikedContentPage />
            </RequireAuthenticated>
          }
        />
        <Route
          path="/saved"
          element={
            <RequireAuthenticated>
              <SavedContentPage />
            </RequireAuthenticated>
          }
        />
        <Route
          path="/upload"
          element={
            <RequireAuthenticated>
              <RequireCreator>
                <UploadPage />
              </RequireCreator>
            </RequireAuthenticated>
          }
        />
        <Route
          path="/uploads"
          element={
            <RequireAuthenticated>
              <RequireCreator>
                <MyUploadsPage />
              </RequireCreator>
            </RequireAuthenticated>
          }
        />
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
