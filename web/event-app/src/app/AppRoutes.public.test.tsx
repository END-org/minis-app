import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "../test/renderWithProviders";
import { AppRoutes } from "./App";
import { feedApiClient, testApiClient } from "./AppRoutesTestSupport";

describe("AppRoutes public routes", () => {
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
});
