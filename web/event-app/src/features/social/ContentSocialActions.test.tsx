import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import type { ApiClient, ApiRequestOptions } from "../../core/api/apiClient";
import type { BackendSessionContextValue } from "../account/BackendSessionContext";
import type { ContentResponse } from "../feed/feedApi";
import { renderWithProviders } from "../../test/renderWithProviders";
import { ContentSocialActions } from "./ContentSocialActions";

const content: ContentResponse = {
  id: "clip-1",
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
  likeCount: 3,
  liked: false,
  media: {
    contentType: "video/mp4",
    expiresAt: "2026-09-01T12:10:00Z",
    sizeBytes: 1024,
    url: "http://localhost:9000/minis-event-media/content/clip-1.mp4",
  },
  publishedAt: "2026-09-01T12:00:00Z",
  saved: false,
  title: "A useful clip",
};

describe("ContentSocialActions", () => {
  it("prompts guests to sign in and preserves the current destination", async () => {
    const user = userEvent.setup();
    renderWithProviders(<ContentSocialActions content={content} />, {
      route: "/content/clip-1?from=feed",
    });

    await user.click(screen.getByRole("button", { name: "Like 3" }));

    expect(screen.getByRole("link", { name: "Sign in" })).toHaveAttribute(
      "href",
      "/sign-in?next=%2Fcontent%2Fclip-1%3Ffrom%3Dfeed",
    );
    expect(screen.getByText("to like videos.")).toBeInTheDocument();
  });

  it("sends an idempotent like request for a ready account", async () => {
    const user = userEvent.setup();
    const requests: { path: string; method: string | undefined }[] = [];
    renderWithProviders(<ContentSocialActions content={content} />, {
      backendSession: readySession(),
      apiClient: {
        request: <T,>(path: string, options?: ApiRequestOptions): Promise<T> => {
          requests.push({ method: options?.method, path });
          return Promise.resolve(undefined as T);
        },
      } satisfies ApiClient,
    });

    await user.click(screen.getByRole("button", { name: "Like 3" }));

    await waitFor(() => {
      expect(requests).toEqual([{ method: "PUT", path: "/api/v1/content/clip-1/like" }]);
    });
  });
});

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
