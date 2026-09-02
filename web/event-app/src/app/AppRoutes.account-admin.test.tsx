import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiClientError, type ApiRequestOptions } from "../core/api/apiClient";
import { createFirebaseUser, renderWithProviders } from "../test/renderWithProviders";
import { AppRoutes } from "./App";
import {
  TARGET_USER,
  VIEWER_ACCOUNT,
  accountApiClient,
  adminDetailApiClient,
  forbiddenApiClient,
  testApiClient,
} from "./AppRoutesTestSupport";

describe("AppRoutes account routes", () => {
  it("redirects protected routes to sign-in while preserving the destination", async () => {
    renderWithProviders(<AppRoutes />, { route: "/account" });

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
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
});

describe("AppRoutes administration routes", () => {
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
