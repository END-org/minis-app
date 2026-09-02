import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { createFirebaseUser, renderWithProviders } from "../test/renderWithProviders";
import { AppRoutes } from "./App";
import {
  VIEWER_ACCOUNT,
  accountApiClient,
  firebaseTestError,
  mockBootstrapFetch,
  renderWithMutableAuth,
} from "./AppRoutesTestSupport";

describe("AppRoutes authentication routes", () => {
  it("shows sign-in validation", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AppRoutes />, { route: "/sign-in" });

    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(screen.getByText("Email is required.")).toBeInTheDocument();
    expect(screen.getByText("Password is required.")).toBeInTheDocument();
  });

  it("shows registration validation and password mismatch", async () => {
    const user = userEvent.setup();
    renderWithProviders(<AppRoutes />, { route: "/sign-up" });

    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(screen.getByText("Display name is required.")).toBeInTheDocument();
    expect(screen.getByText("Email is required.")).toBeInTheDocument();
    expect(screen.getByText("Password is required.")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Display name"), "New Viewer");
    await user.type(screen.getByLabelText("Email"), "new@example.com");
    await user.type(screen.getByLabelText("Password"), "viewer-password");
    await user.type(screen.getByLabelText("Confirm password"), "different-password");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(screen.getByText("Passwords must match.")).toBeInTheDocument();
  });

  it("creates a Firebase password account and waits for email verification", async () => {
    const user = userEvent.setup();
    const requests: string[] = [];
    const registerWithEmail = vi.fn(() =>
      Promise.resolve({ displayNameUpdated: false, verificationEmailSent: false }),
    );

    renderWithProviders(<AppRoutes />, {
      apiClient: accountApiClient(requests),
      auth: {
        registerWithEmail,
        user: createFirebaseUser({
          email: "new@example.com",
          emailVerified: false,
        }),
      },
      route: "/sign-up?next=%2Faccount",
    });

    await user.type(screen.getByLabelText("Display name"), "New Viewer");
    await user.type(screen.getByLabelText("Email"), "NEW@example.com");
    await user.type(screen.getByLabelText("Password"), "viewer-password");
    await user.type(screen.getByLabelText("Confirm password"), "viewer-password");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("heading", { name: "Verify your email" })).toBeInTheDocument();
    expect(screen.getByText(/display name was not saved/u)).toBeInTheDocument();
    expect(screen.getByText(/verification email was not sent/u)).toBeInTheDocument();
    expect(registerWithEmail).toHaveBeenCalledWith({
      displayName: "New Viewer",
      email: "new@example.com",
      password: "viewer-password",
    });
    expect(requests).not.toContain("/api/v1/auth/bootstrap");
  });

  it("routes unverified password sign-in to email verification without bootstrap", async () => {
    const user = userEvent.setup();
    const unverifiedUser = createFirebaseUser({ emailVerified: false });
    const fetchMock = vi.fn<typeof fetch>();
    vi.stubGlobal("fetch", fetchMock);
    const auth = renderWithMutableAuth(<AppRoutes />, {
      emailSignInUser: unverifiedUser,
      reloadUser: unverifiedUser,
      route: "/sign-in?next=%2Faccount",
    });

    await user.type(screen.getByLabelText("Email"), "viewer@example.com");
    await user.type(screen.getByLabelText("Password"), "prepared-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Verify your email" })).toBeInTheDocument();
    expect(auth.refreshCurrentToken).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("signs in, bootstraps, and returns to the intended destination", async () => {
    const user = userEvent.setup();
    const signedInUser = createFirebaseUser({ uid: "firebase-viewer", token: "fresh-viewer-token" });
    const requests = mockBootstrapFetch(VIEWER_ACCOUNT);
    const auth = renderWithMutableAuth(<AppRoutes />, {
      emailSignInUser: signedInUser,
      reloadUser: signedInUser,
      route: "/sign-in?next=%2Faccount",
    });

    await user.type(screen.getByLabelText("Email"), "viewer@example.com");
    await user.type(screen.getByLabelText("Password"), "prepared-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "My Account" })).toBeInTheDocument();
    });
    expect(screen.getByText("VIEWER")).toBeInTheDocument();
    expect(auth.signInWithEmail).toHaveBeenCalledWith("viewer@example.com", "prepared-password");
    expect(auth.refreshCurrentToken).not.toHaveBeenCalled();
    expect(requests.map((request) => request.path)).toEqual(["/api/v1/auth/bootstrap"]);
    expect(requests[0]?.headers.get("Authorization")).toBe("Bearer fresh-viewer-token");
    expect(screen.queryByRole("link", { name: "Guest" })).not.toBeInTheDocument();
  });

  it("resends verification email with a cooldown", async () => {
    const user = userEvent.setup();
    const sendCurrentUserVerificationEmail = vi.fn(() => Promise.resolve());

    renderWithProviders(<AppRoutes />, {
      auth: {
        sendCurrentUserVerificationEmail,
        user: createFirebaseUser({ email: "verify@example.com", emailVerified: false }),
      },
      route: "/verify-email",
    });

    await user.click(await screen.findByRole("button", { name: "Resend verification email" }));

    expect(sendCurrentUserVerificationEmail).toHaveBeenCalledTimes(1);
    expect(
      screen.getByText("Verification email sent. Check your inbox before continuing."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Resend in 60s" })).toBeDisabled();
  });

  it("keeps an unverified user on the verification page when Firebase has not updated", async () => {
    const user = userEvent.setup();
    const refreshCurrentToken = vi.fn(() => Promise.resolve());
    const unverifiedUser = createFirebaseUser({ emailVerified: false });

    renderWithProviders(<AppRoutes />, {
      auth: {
        refreshCurrentToken,
        reloadCurrentUser: () => Promise.resolve(unverifiedUser),
        user: unverifiedUser,
      },
      route: "/verify-email",
    });

    await user.click(await screen.findByRole("button", { name: "Check verification" }));

    expect(screen.getByText("Firebase still reports this email as unverified.")).toBeInTheDocument();
    expect(refreshCurrentToken).not.toHaveBeenCalled();
  });

  it("bootstraps and loads /me after email verification is confirmed", async () => {
    const user = userEvent.setup();
    const requests: string[] = [];
    const verifiedUser = createFirebaseUser({ emailVerified: true });
    const refreshCurrentToken = vi.fn(() => Promise.resolve());

    renderWithProviders(<AppRoutes />, {
      apiClient: accountApiClient(requests),
      auth: {
        refreshCurrentToken,
        reloadCurrentUser: () => Promise.resolve(verifiedUser),
        user: verifiedUser,
      },
      route: "/verify-email?next=%2Faccount",
    });

    await user.click(await screen.findByRole("button", { name: "Check verification" }));

    expect(await screen.findByRole("heading", { name: "My Account" })).toBeInTheDocument();
    expect(refreshCurrentToken).toHaveBeenCalledTimes(1);
    expect(requests).toContain("/api/v1/auth/bootstrap");
    expect(requests).not.toContain("/api/v1/me");
  });

  it("signs in with Google and bootstraps the backend account", async () => {
    const user = userEvent.setup();
    const googleUser = createFirebaseUser({
      providerId: "google.com",
      token: "fresh-google-token",
      uid: "firebase-google",
    });
    const requests = mockBootstrapFetch(VIEWER_ACCOUNT);
    const auth = renderWithMutableAuth(<AppRoutes />, {
      googleSignInUser: googleUser,
      reloadUser: googleUser,
      route: "/sign-in?next=%2Faccount",
    });

    await user.click(screen.getByRole("button", { name: "Sign in with Google" }));

    expect(await screen.findByRole("heading", { name: "My Account" })).toBeInTheDocument();
    expect(auth.signInWithGoogle).toHaveBeenCalledTimes(1);
    expect(auth.refreshCurrentToken).not.toHaveBeenCalled();
    expect(requests.map((request) => request.path)).toEqual(["/api/v1/auth/bootstrap"]);
    expect(requests[0]?.headers.get("Authorization")).toBe("Bearer fresh-google-token");
  });

  it("shows safe Google popup cancellation feedback", async () => {
    const user = userEvent.setup();
    const signInWithGoogle = vi.fn(() =>
      Promise.reject(firebaseTestError("auth/popup-closed-by-user")),
    );

    renderWithProviders(<AppRoutes />, {
      auth: { signInWithGoogle },
      route: "/sign-in",
    });

    await user.click(screen.getByRole("button", { name: "Sign in with Google" }));

    expect(screen.getByText("Google sign-in was cancelled before completion.")).toBeInTheDocument();
  });

  it("shows safe Google provider configuration feedback", async () => {
    const user = userEvent.setup();
    const signInWithGoogle = vi.fn(() =>
      Promise.reject(firebaseTestError("auth/operation-not-allowed")),
    );

    renderWithProviders(<AppRoutes />, {
      auth: { signInWithGoogle },
      route: "/sign-in",
    });

    await user.click(screen.getByRole("button", { name: "Sign in with Google" }));

    expect(
      screen.getByText("This sign-in method is not enabled for the Firebase project."),
    ).toBeInTheDocument();
  });

  it("requests password reset with neutral success feedback", async () => {
    const user = userEvent.setup();
    const sendPasswordResetEmail = vi.fn(() => Promise.resolve());

    renderWithProviders(<AppRoutes />, {
      auth: { sendPasswordResetEmail },
      route: "/forgot-password",
    });

    await user.type(screen.getByLabelText("Email"), "VIEWER@example.com");
    await user.click(screen.getByRole("button", { name: "Send reset link" }));

    expect(sendPasswordResetEmail).toHaveBeenCalledWith("viewer@example.com");
    expect(screen.getByText(/If that email can receive MINIS password reset mail/u)).toBeInTheDocument();
  });
});
