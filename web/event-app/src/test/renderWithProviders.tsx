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

interface RenderOptions {
  readonly apiClient?: ApiClient;
  readonly auth?: Partial<AuthSessionContextValue>;
  readonly route?: string;
}

export function renderWithProviders(
  ui: ReactElement,
  { apiClient = createNoopApiClient(), auth = {}, route = "/" }: RenderOptions = {},
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
          <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
        </ApiClientContext.Provider>
      </AuthSessionContext.Provider>
    </QueryClientProvider>,
  );
}

export function createAuthSession(
  overrides: Partial<AuthSessionContextValue> = {},
): AuthSessionContextValue {
  return {
    user: null,
    loading: false,
    setupError: null,
    sessionNotice: null,
    signInWithEmail: () => Promise.resolve(),
    signOut: () => Promise.resolve(),
    reloadCurrentUser: () => Promise.resolve(),
    expireAuthenticatedSession: () => Promise.resolve(),
    clearSessionNotice: () => undefined,
    ...overrides,
  };
}

export function createFirebaseUser(): User {
  return {
    getIdToken: () => Promise.resolve("test-token"),
  } as unknown as User;
}

function createNoopApiClient(): ApiClient {
  return {
    request: <T,>() => Promise.reject<T>(new Error("Test did not provide an API client.")),
  };
}
