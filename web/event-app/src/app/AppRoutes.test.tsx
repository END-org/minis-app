import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { User } from "firebase/auth";
import { useCallback, useMemo, useRef, useState, type ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { ApiClientProvider } from "../core/api/ApiClientContext";
import { ApiClientError, type ApiClient, type ApiRequestOptions } from "../core/api/apiClient";
import {
  AuthSessionContext,
  type AuthSessionContextValue,
} from "../core/auth/AuthSessionContext";
import type { AdminUserResponse } from "../features/admin/adminApi";
import { BackendSessionProvider } from "../features/account/BackendSessionContext";
import type { AccountResponse } from "../features/account/accountApi";
import { AppRoutes } from "./App";
import { createFirebaseUser, renderWithProviders } from "../test/renderWithProviders";

const VIEWER_ACCOUNT: AccountResponse = {
  id: "9c6f08c8-7d56-4c56-b7f2-e7434dc87601",
  email: "viewer@example.com",
  emailVerified: true,
  role: "VIEWER",
  status: "ACTIVE",
  createdAt: "2026-07-17T12:00:00Z",
};

const ADMIN_ACCOUNT: AccountResponse = {
  ...VIEWER_ACCOUNT,
  id: "b9f04bea-ac9e-4980-9bfb-15542843e19a",
  email: "admin@example.com",
  role: "ADMIN",
};

const TARGET_USER: AdminUserResponse = {
  id: "e6db1418-8c80-4d4f-97bf-f54b42815ff9",
  email: "creator-target@example.com",
  role: "VIEWER",
  status: "ACTIVE",
  createdAt: "2026-07-17T12:00:00Z",
  updatedAt: "2026-07-17T12:00:00Z",
};

describe("AppRoutes", () => {
  it("renders the guest route", () => {
    renderWithProviders(<AppRoutes />, { route: "/guest" });

    expect(screen.getByRole("heading", { name: "MINIS Event" })).toBeInTheDocument();
    expect(screen.getByText("Guest access active")).toBeInTheDocument();
  });

  it("renders the public feed empty state from the backend", async () => {
    renderWithProviders(<AppRoutes />, {
      apiClient: feedApiClient(),
    });

    expect(await screen.findByRole("heading", { name: "MINIS Event" })).toBeInTheDocument();
    expect(screen.getByText("No event videos yet")).toBeInTheDocument();
  });

  it("redirects protected routes to sign-in while preserving the destination", async () => {
    renderWithProviders(<AppRoutes />, { route: "/account" });

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
  });

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

  it("loads the authenticated account role and status from the backend", async () => {
    renderWithProviders(<AppRoutes />, {
      apiClient: accountApiClient(),
      auth: { user: createFirebaseUser() },
      route: "/account",
    });

    expect(await screen.findByText("VIEWER")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE")).toBeInTheDocument();
  });

  it("keeps 403 forbidden distinct from sign-in", async () => {
    const signOut = vi.fn<() => Promise<void>>(() => Promise.resolve());
    renderWithProviders(<AppRoutes />, {
      apiClient: forbiddenApiClient(),
      auth: { signOut, user: createFirebaseUser() },
      route: "/account",
    });

    expect(await screen.findByText("ACCOUNT_DISABLED")).toBeInTheDocument();
    expect(screen.getByText("Correlation ID: disabled-correlation")).toBeInTheDocument();
    expect(signOut).not.toHaveBeenCalled();
  });

  it("signs out and removes authenticated application state", async () => {
    const user = userEvent.setup();
    const signOut = vi.fn<() => Promise<void>>(() => Promise.resolve());
    renderWithProviders(<AppRoutes />, {
      apiClient: accountApiClient(),
      auth: { signOut, user: createFirebaseUser() },
      route: "/account",
    });

    expect(await screen.findByRole("heading", { name: "My Account" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Sign out" }));

    expect(signOut).toHaveBeenCalledTimes(1);
    expect(await screen.findByText("No event videos yet")).toBeInTheDocument();
  });

  it("updates the current profile", async () => {
    const user = userEvent.setup();
    const requests: ApiRequestOptions[] = [];
    const apiClient = testApiClient((path, options) => {
      requests.push(options ?? {});
      if (path === "/api/v1/auth/bootstrap") {
        return Promise.resolve(VIEWER_ACCOUNT);
      }
      if (path === "/api/v1/me" && options?.method !== "PATCH") {
        return Promise.resolve(VIEWER_ACCOUNT);
      }
      if (path === "/api/v1/me/profile") {
        return Promise.resolve({
          ...VIEWER_ACCOUNT,
          bio: "Event bio",
          displayName: "Local Viewer",
        });
      }
      return Promise.reject(new Error(`Unexpected path ${path}`));
    });

    renderWithProviders(<AppRoutes />, {
      apiClient,
      auth: { user: createFirebaseUser() },
      route: "/account",
    });

    await user.type(await screen.findByLabelText("Display name"), "Local Viewer");
    await user.type(screen.getByLabelText("Bio"), "Event bio");
    await user.click(screen.getByRole("button", { name: "Save profile" }));

    expect(await screen.findByText("Profile saved.")).toBeInTheDocument();
    expect(requests.some((request) => request.method === "PATCH")).toBe(true);
  });

  it("loads public creators for guests", async () => {
    renderWithProviders(<AppRoutes />, {
      apiClient: testApiClient((path) => {
        if (path === "/api/v1/creators") {
          return Promise.resolve({
            creators: [
              {
                userId: "2032c46a-e0e3-47dc-8326-5f205966bce2",
                slug: "mira-mini",
                displayName: "Mira Mini",
                bio: "Quiet event films.",
                verified: true,
                createdAt: "2026-07-17T12:00:00Z",
                updatedAt: "2026-07-17T12:00:00Z",
              },
            ],
            page: 0,
            size: 20,
            hasNext: false,
          });
        }
        return Promise.reject(new Error(`Unexpected path ${path}`));
      }),
      route: "/creators",
    });

    expect(await screen.findByRole("heading", { name: "Creators" })).toBeInTheDocument();
    expect(screen.getByText("Mira Mini")).toBeInTheDocument();
    expect(screen.getByText("Verified")).toBeInTheDocument();
  });

  it("shows a real empty state when the backend returns no creators", async () => {
    renderWithProviders(<AppRoutes />, {
      apiClient: testApiClient((path) => {
        if (path === "/api/v1/creators") {
          return Promise.resolve({
            creators: [],
            page: 0,
            size: 20,
            hasNext: false,
          });
        }
        return Promise.reject(new Error(`Unexpected path ${path}`));
      }),
      route: "/creators",
    });

    expect(await screen.findByRole("heading", { name: "Creators" })).toBeInTheDocument();
    expect(screen.getByText("No active creators yet")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });

  it("shows forbidden instead of admin tools for non-admin users", async () => {
    renderWithProviders(<AppRoutes />, {
      apiClient: accountApiClient(),
      auth: { user: createFirebaseUser() },
      route: "/admin",
    });

    expect(await screen.findByRole("heading", { name: "Access is blocked" })).toBeInTheDocument();
    expect(screen.getByText("ACCESS_DENIED")).toBeInTheDocument();
  });

  it("allows an admin action to grant creator capability", async () => {
    const user = userEvent.setup();
    const apiClient = adminDetailApiClient({
      ...TARGET_USER,
      role: "CREATOR",
      creatorProfile: {
        userId: TARGET_USER.id,
        slug: "event-creator",
        displayName: "Event Creator",
        verified: false,
        createdAt: "2026-07-17T12:00:00Z",
        updatedAt: "2026-07-17T12:00:00Z",
      },
    });

    renderWithProviders(<AppRoutes />, {
      apiClient,
      auth: { user: createFirebaseUser() },
      route: `/admin/users/${TARGET_USER.id}`,
    });

    await user.type(await screen.findByLabelText("Public slug"), "Event Creator");
    await user.type(screen.getByLabelText("Public display name"), "Event Creator");
    await user.click(screen.getByRole("button", { name: "Grant creator" }));

    expect(await screen.findByText("Creator role saved.")).toBeInTheDocument();
    expect(screen.getByText("CREATOR")).toBeInTheDocument();
  });

  it("shows admin action failures while leaving the last loaded user visible", async () => {
    const user = userEvent.setup();
    const apiClient = adminDetailApiClient(
      TARGET_USER,
      new ApiClientError({
        kind: "api",
        status: 409,
        code: "RESOURCE_CONFLICT",
        message: "Resource conflict.",
        correlationId: "slug-conflict",
      }),
    );

    renderWithProviders(<AppRoutes />, {
      apiClient,
      auth: { user: createFirebaseUser() },
      route: `/admin/users/${TARGET_USER.id}`,
    });

    await user.type(await screen.findByLabelText("Public slug"), "Event Creator");
    await user.type(screen.getByLabelText("Public display name"), "Event Creator");
    await user.click(screen.getByRole("button", { name: "Grant creator" }));

    expect(await screen.findByText("RESOURCE_CONFLICT")).toBeInTheDocument();
    expect(screen.getByText("Correlation ID: slug-conflict")).toBeInTheDocument();
    expect(screen.getByText("VIEWER")).toBeInTheDocument();
  });
});

interface MutableAuthOptions {
  readonly emailSignInUser?: User;
  readonly googleSignInUser?: User;
  readonly initialUser?: User | null;
  readonly reloadUser?: User;
  readonly route?: string;
}

interface FetchRequestRecord {
  readonly headers: Headers;
  readonly method: string;
  readonly path: string;
}

function renderWithMutableAuth(ui: ReactNode, options: MutableAuthOptions = {}) {
  const controller = createMutableAuthController(options);
  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: { retry: false },
      queries: { retry: false, staleTime: Number.POSITIVE_INFINITY },
    },
  });

  render(
    <QueryClientProvider client={queryClient}>
      <MutableAuthProvider controller={controller}>
        <ApiClientProvider>
          <BackendSessionProvider>
            <MemoryRouter initialEntries={[options.route ?? "/"]}>{ui}</MemoryRouter>
          </BackendSessionProvider>
        </ApiClientProvider>
      </MutableAuthProvider>
    </QueryClientProvider>,
  );

  return controller;
}

function createMutableAuthController(options: MutableAuthOptions) {
  return {
    initialUser: options.initialUser ?? null,
    emailSignInUser: options.emailSignInUser ?? createFirebaseUser(),
    googleSignInUser:
      options.googleSignInUser ?? createFirebaseUser({ providerId: "google.com" }),
    reloadUser: options.reloadUser,
    signInWithEmail: vi.fn<(email: string, password: string) => void>(),
    signInWithGoogle: vi.fn<() => void>(),
    signOut: vi.fn<() => void>(),
    refreshCurrentToken: vi.fn<() => void>(),
  };
}

type MutableAuthController = ReturnType<typeof createMutableAuthController>;

interface MutableAuthProviderProps {
  readonly children: ReactNode;
  readonly controller: MutableAuthController;
}

function MutableAuthProvider({ children, controller }: MutableAuthProviderProps) {
  const [user, setUser] = useState<User | null>(controller.initialUser);
  const [sessionVersion, setSessionVersion] = useState(0);
  const currentUserRef = useRef<User | null>(controller.initialUser);

  const updateUser = useCallback((nextUser: User | null) => {
    currentUserRef.current = nextUser;
    setUser(nextUser);
    setSessionVersion((current) => current + 1);
  }, [currentUserRef]);

  const value = useMemo<AuthSessionContextValue>(
    () => ({
      user,
      loading: false,
      sessionVersion,
      setupError: null,
      sessionNotice: null,
      registerWithEmail: () =>
        Promise.resolve({ displayNameUpdated: true, verificationEmailSent: true }),
      signInWithEmail: (email: string, password: string) => {
        controller.signInWithEmail(email, password);
        updateUser(controller.emailSignInUser);
        return Promise.resolve(controller.emailSignInUser);
      },
      signInWithGoogle: () => {
        controller.signInWithGoogle();
        updateUser(controller.googleSignInUser);
        return Promise.resolve(controller.googleSignInUser);
      },
      signOut: () => {
        controller.signOut();
        updateUser(null);
        return Promise.resolve();
      },
      reloadCurrentUser: () => {
        const nextUser = controller.reloadUser ?? currentUserRef.current;
        if (!nextUser) {
          return Promise.reject(new Error("No Firebase user is signed in."));
        }
        updateUser(nextUser);
        return Promise.resolve(nextUser);
      },
      refreshCurrentToken: () => {
        controller.refreshCurrentToken();
        return currentUserRef.current?.getIdToken(true).then(() => undefined) ?? Promise.resolve();
      },
      sendCurrentUserVerificationEmail: () => Promise.resolve(),
      sendPasswordResetEmail: () => Promise.resolve(),
      expireAuthenticatedSession: () => {
        updateUser(null);
        return Promise.resolve();
      },
      clearSessionNotice: () => undefined,
      currentUser: () => currentUserRef.current,
    }),
    [controller, currentUserRef, sessionVersion, updateUser, user],
  );

  return <AuthSessionContext.Provider value={value}>{children}</AuthSessionContext.Provider>;
}

function mockBootstrapFetch(account: AccountResponse): FetchRequestRecord[] {
  const requests: FetchRequestRecord[] = [];
  const fetchMock = vi.fn<typeof fetch>((input, init) => {
    const url = new URL(fetchInputUrl(input), "http://localhost");
    const method = init?.method ?? "GET";
    requests.push({
      headers: new Headers(init?.headers),
      method,
      path: url.pathname,
    });
    if (url.pathname === "/api/v1/auth/bootstrap" && method === "POST") {
      return Promise.resolve(jsonFetchResponse(200, account));
    }
    return Promise.resolve(
      jsonFetchResponse(404, {
        status: 404,
        code: "NOT_FOUND",
        message: "Not found.",
      }),
    );
  });
  vi.stubGlobal("fetch", fetchMock);
  return requests;
}

function fetchInputUrl(input: Parameters<typeof fetch>[0]): string {
  if (typeof input === "string") {
    return input;
  }
  if (input instanceof URL) {
    return input.toString();
  }
  return input.url;
}

function jsonFetchResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
    status,
  });
}

function testApiClient(
  handler: (path: string, options?: ApiRequestOptions) => Promise<unknown>,
): ApiClient {
  return {
    request: async <T,>(path: string, options?: ApiRequestOptions): Promise<T> =>
      (await handler(path, options)) as T,
  };
}

function accountApiClient(requests: string[] = []): ApiClient {
  return testApiClient((path) => {
    requests.push(path);
    if (path === "/api/v1/auth/bootstrap" || path === "/api/v1/me") {
      return Promise.resolve(VIEWER_ACCOUNT);
    }
    if (path === "/api/v1/feed?limit=10") {
      return Promise.resolve({ items: [], limit: 10 });
    }
    if (path === "/api/v1/categories") {
      return Promise.resolve([]);
    }
    return Promise.reject(new Error(`Unexpected path ${path}`));
  });
}

function feedApiClient(): ApiClient {
  return testApiClient((path) => {
    if (path === "/api/v1/feed?limit=10") {
      return Promise.resolve({ items: [], limit: 10 });
    }
    if (path === "/api/v1/categories") {
      return Promise.resolve([]);
    }
    return Promise.reject(new Error(`Unexpected path ${path}`));
  });
}

function forbiddenApiClient(): ApiClient {
  return testApiClient(() =>
    Promise.reject(
      new ApiClientError({
        kind: "api",
        status: 403,
        code: "ACCOUNT_DISABLED",
        message: "MINIS account is disabled.",
        correlationId: "disabled-correlation",
      }),
    ),
  );
}

function adminDetailApiClient(
  actionResult: AdminUserResponse,
  actionFailure?: ApiClientError,
): ApiClient {
  let currentDetail = TARGET_USER;
  return testApiClient((path, options) => {
    if (path === "/api/v1/auth/bootstrap") {
      return Promise.resolve(ADMIN_ACCOUNT);
    }
    if (path === `/api/v1/admin/users/${TARGET_USER.id}`) {
      return Promise.resolve(currentDetail);
    }
    if (path === `/api/v1/admin/users/${TARGET_USER.id}/creator-role`) {
      if (actionFailure) {
        return Promise.reject(actionFailure);
      }
      currentDetail = actionResult;
      return Promise.resolve(actionResult);
    }
    return Promise.reject(
      new Error(`Unexpected path ${path} with method ${options?.method ?? "GET"}`),
    );
  });
}

function firebaseTestError(code: string): Error & { code: string } {
  const error = new Error("Firebase auth failed.") as Error & { code: string };
  error.code = code;
  return error;
}
