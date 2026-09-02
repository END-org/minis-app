import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import type { ApiClient } from "../../core/api/apiClient";
import type { BackendSessionContextValue } from "../account/BackendSessionContext";
import { renderWithProviders } from "../../test/renderWithProviders";
import type { ContentPageResponse, ContentResponse } from "./feedApi";
import { LikedContentPage } from "./ContentCollectionPage";

describe("LikedContentPage", () => {
  it("loads only liked content and paginates with the backend cursor", async () => {
    const user = userEvent.setup();
    const requests: string[] = [];
    const firstPage: ContentPageResponse = {
      items: [contentFixture("liked-1", "First liked video")],
      limit: 12,
      nextCursor: "cursor-2",
    };
    const secondPage: ContentPageResponse = {
      items: [contentFixture("liked-2", "Second liked video")],
      limit: 12,
    };

    renderWithProviders(<LikedContentPage />, {
      backendSession: readySession(),
      apiClient: {
        request: <T,>(path: string): Promise<T> => {
          requests.push(path);
          if (path === "/api/v1/me/liked-content?size=12") {
            return Promise.resolve(firstPage as T);
          }
          if (path === "/api/v1/me/liked-content?cursor=cursor-2&size=12") {
            return Promise.resolve(secondPage as T);
          }
          return Promise.reject(new Error(`Unexpected path ${path}`));
        },
      } satisfies ApiClient,
    });

    expect(await screen.findByText("First liked video")).toBeInTheDocument();
    expect(requests).toEqual(["/api/v1/me/liked-content?size=12"]);

    await user.click(screen.getByRole("button", { name: "Load more" }));

    expect(await screen.findByText("Second liked video")).toBeInTheDocument();
    expect(requests).toEqual([
      "/api/v1/me/liked-content?size=12",
      "/api/v1/me/liked-content?cursor=cursor-2&size=12",
    ]);
  });
});

function contentFixture(id: string, title: string): ContentResponse {
  return {
    id,
    category: "EDUCATION",
    categoryLabel: "Education",
    creator: {
      displayName: "Mira Mini",
      slug: "mira-mini",
      userId: "creator-1",
      verified: true,
    },
    description: "A public event video.",
    following: false,
    likeCount: 1,
    liked: true,
    media: {
      contentType: "video/mp4",
      expiresAt: "2026-09-01T12:10:00Z",
      sizeBytes: 1024,
      url: `http://localhost:9000/minis-event-media/content/${id}.mp4`,
    },
    publishedAt: "2026-09-01T12:00:00Z",
    saved: false,
    title,
  };
}

function readySession(): BackendSessionContextValue {
  return {
    replaceAccount: () => undefined,
    retryProvisioning: () => undefined,
    state: {
      account: {
        createdAt: "2026-09-01T12:00:00Z",
        emailVerified: true,
        id: "viewer-1",
        role: "VIEWER",
        status: "ACTIVE",
      },
      status: "ready",
    },
  };
}
