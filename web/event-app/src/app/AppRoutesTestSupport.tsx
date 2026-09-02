import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { User } from "firebase/auth";
import { useCallback, useMemo, useRef, useState, type ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { ApiClientProvider } from "../core/api/ApiClientProvider";
import { ApiClientError, type ApiClient, type ApiRequestOptions } from "../core/api/apiClient";
import {
  AuthSessionContext,
  type AuthSessionContextValue,
} from "../core/auth/AuthSessionContext";
import { BackendSessionProvider } from "../features/account/BackendSessionProvider";
import type { AccountResponse } from "../features/account/accountApi";
import type { AdminUserResponse } from "../features/admin/adminApi";
import { createFirebaseUser } from "../test/renderWithProviders";

export const VIEWER_ACCOUNT: AccountResponse = {
  id: "9c6f08c8-7d56-4c56-b7f2-e7434dc87601",
  email: "viewer@example.com",
  emailVerified: true,
  role: "VIEWER",
  status: "ACTIVE",
  createdAt: "2026-07-17T12:00:00Z",
};

export const ADMIN_ACCOUNT: AccountResponse = {
  ...VIEWER_ACCOUNT,
  id: "b9f04bea-ac9e-4980-9bfb-15542843e19a",
  email: "admin@example.com",
  role: "ADMIN",
};

export const TARGET_USER: AdminUserResponse = {
  id: "e6db1418-8c80-4d4f-97bf-f54b42815ff9",
  email: "creator-target@example.com",
  role: "VIEWER",
  status: "ACTIVE",
  createdAt: "2026-07-17T12:00:00Z",
  updatedAt: "2026-07-17T12:00:00Z",
};

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

export function renderWithMutableAuth(ui: ReactNode, options: MutableAuthOptions = {}) {
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

// Test harnesses intentionally keep their provider and controller factory together.
// eslint-disable-next-line react-refresh/only-export-components
function MutableAuthProvider({ children, controller }: MutableAuthProviderProps) {
  const [user, setUser] = useState<User | null>(controller.initialUser);
  const [sessionVersion, setSessionVersion] = useState(0);
  const currentUserRef = useRef<User | null>(controller.initialUser);

  const updateUser = useCallback((nextUser: User | null) => {
    currentUserRef.current = nextUser;
    setUser(nextUser);
    setSessionVersion((current) => current + 1);
  }, []);

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
    [controller, sessionVersion, updateUser, user],
  );

  return <AuthSessionContext.Provider value={value}>{children}</AuthSessionContext.Provider>;
}

export function mockBootstrapFetch(account: AccountResponse): FetchRequestRecord[] {
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

export function testApiClient(
  handler: (path: string, options?: ApiRequestOptions) => Promise<unknown>,
): ApiClient {
  return {
    request: async <T,>(path: string, options?: ApiRequestOptions): Promise<T> =>
      (await handler(path, options)) as T,
  };
}

export function accountApiClient(requests: string[] = []): ApiClient {
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

export function feedApiClient(): ApiClient {
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

export function forbiddenApiClient(): ApiClient {
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

export function adminDetailApiClient(
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

export function firebaseTestError(code: string): Error & { code: string } {
  const error = new Error("Firebase auth failed.") as Error & { code: string };
  error.code = code;
  return error;
}
