import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, type RenderResult } from "@testing-library/react";
import type { User } from "firebase/auth";
import { type ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";
import { ApiClientContext } from "../core/api/ApiClientContext";
import type { ApiClient } from "../core/api/apiClient";
import {
  AuthSessionContext,
  type AuthSessionContextValue,
} from "../core/auth/AuthSessionContext";
import {
  BackendSessionContext,
  type BackendSessionContextValue,
} from "../features/account/BackendSessionContext";
import { BackendSessionProvider } from "../features/account/BackendSessionProvider";

interface RenderOptions {
  readonly apiClient?: ApiClient;
  readonly auth?: Partial<AuthSessionContextValue>;
  readonly backendSession?: BackendSessionContextValue;
  readonly route?: string;
}

export function renderWithProviders(
  ui: ReactElement,
  {
    apiClient = createNoopApiClient(),
    auth = {},
    backendSession,
    route = "/",
  }: RenderOptions = {},
): RenderResult {
  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: { retry: false },
      queries: { retry: false, staleTime: Number.POSITIVE_INFINITY },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthSessionContext.Provider value={createAuthSession(auth)}>
        <ApiClientContext.Provider value={apiClient}>
          {backendSession ? (
            <BackendSessionContext.Provider value={backendSession}>
              <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
            </BackendSessionContext.Provider>
          ) : (
            <BackendSessionProvider>
              <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
            </BackendSessionProvider>
          )}
        </ApiClientContext.Provider>
      </AuthSessionContext.Provider>
    </QueryClientProvider>,
  );
}

export function createAuthSession(
  overrides: Partial<AuthSessionContextValue> = {},
): AuthSessionContextValue {
  const currentUser = overrides.currentUser ?? (() => overrides.user ?? null);
  return {
    user: null,
    loading: false,
    sessionVersion: 0,
    setupError: null,
    sessionNotice: null,
    registerWithEmail: () =>
      Promise.resolve({ displayNameUpdated: true, verificationEmailSent: true }),
    signInWithEmail: () => Promise.resolve(createFirebaseUser()),
    signInWithGoogle: () => Promise.resolve(createFirebaseUser()),
    signOut: () => Promise.resolve(),
    reloadCurrentUser: () => Promise.resolve(createFirebaseUser()),
    refreshCurrentToken: () => Promise.resolve(),
    sendCurrentUserVerificationEmail: () => Promise.resolve(),
    sendPasswordResetEmail: () => Promise.resolve(),
    expireAuthenticatedSession: () => Promise.resolve(),
    clearSessionNotice: () => undefined,
    currentUser,
    ...overrides,
  };
}

interface CreateFirebaseUserOptions {
  readonly email?: string | null;
  readonly emailVerified?: boolean;
  readonly providerId?: string;
  readonly token?: string;
  readonly uid?: string;
}

export function createFirebaseUser({
  email = "viewer@example.com",
  emailVerified = true,
  providerId = "password",
  token = "test-token",
  uid = "firebase-viewer",
}: CreateFirebaseUserOptions = {}): User {
  return {
    email,
    emailVerified,
    getIdToken: () => Promise.resolve(token),
    providerData: [{ providerId }],
    uid,
  } as unknown as User;
}

function createNoopApiClient(): ApiClient {
  return {
    request: <T,>() => Promise.reject<T>(new Error("Test did not provide an API client.")),
  };
}
