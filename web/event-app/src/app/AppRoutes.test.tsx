import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiClientError, type ApiClient } from "../core/api/apiClient";
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

describe("AppRoutes", () => {
  it("renders the guest route", () => {
    renderWithProviders(<AppRoutes />);

    expect(screen.getByRole("heading", { name: "MINIS Event" })).toBeInTheDocument();
    expect(screen.getByText("Guest access active")).toBeInTheDocument();
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

  it("signs in, bootstraps, and returns to the intended destination", async () => {
    const user = userEvent.setup();
    const signInWithEmail = vi.fn<() => Promise<void>>(() => Promise.resolve());
    const apiClient = accountApiClient();

    renderWithProviders(<AppRoutes />, {
      apiClient,
      auth: {
        signInWithEmail,
        reloadCurrentUser: () => Promise.resolve(),
        user: createFirebaseUser(),
      },
      route: "/sign-in?next=%2Faccount",
    });

    await user.type(screen.getByLabelText("Email"), "viewer@example.com");
    await user.type(screen.getByLabelText("Password"), "prepared-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Account" })).toBeInTheDocument();
    });
    expect(screen.getByText("VIEWER")).toBeInTheDocument();
    expect(signInWithEmail).toHaveBeenCalledWith("viewer@example.com", "prepared-password");
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

    expect(await screen.findByRole("heading", { name: "Account" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Sign out" }));

    expect(signOut).toHaveBeenCalledTimes(1);
    expect(screen.getByText("Guest access active")).toBeInTheDocument();
  });
});

function accountApiClient(): ApiClient {
  return {
    request: <T,>(path: string): Promise<T> => {
      if (path === "/api/v1/auth/bootstrap" || path === "/api/v1/me") {
        return Promise.resolve(VIEWER_ACCOUNT as T);
      }
      return Promise.reject(new Error(`Unexpected path ${path}`));
    },
  };
}

function forbiddenApiClient(): ApiClient {
  return {
    request: <T,>(): Promise<T> =>
      Promise.reject(
        new ApiClientError({
          kind: "api",
          status: 403,
          code: "ACCOUNT_DISABLED",
          message: "MINIS account is disabled.",
          correlationId: "disabled-correlation",
        }),
      ),
  };
}
