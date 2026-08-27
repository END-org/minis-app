import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ApiClient } from "../../core/api/apiClient";
import { renderWithProviders } from "../../test/renderWithProviders";
import { CreatorContentSection } from "./CreatorContentSection";
import type { ContentPageResponse, ContentResponse } from "./feedApi";
import { FeedPage } from "./FeedPage";

describe("FeedPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(HTMLMediaElement.prototype, "play", {
      configurable: true,
      value: vi.fn(() => Promise.resolve()),
    });
    Object.defineProperty(HTMLMediaElement.prototype, "pause", {
      configurable: true,
      value: vi.fn(),
    });
  });

  it("loads a real empty feed projection", async () => {
    renderFeed(<FeedPage />, {
      "/api/v1/feed?limit=10": { items: [], limit: 10 },
      "/api/v1/categories": [],
    });

    expect(await screen.findByRole("heading", { name: "MINIS Event" })).toBeInTheDocument();
    expect(screen.getByText("No event videos yet")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });

  it("loads the next cursor page without duplicate clicks while a page is loading", async () => {
    const firstPage: ContentPageResponse = {
      items: [contentFixture("one", "First")],
      limit: 10,
      nextCursor: "next-page",
    };
    const secondPage: ContentPageResponse = {
      items: [contentFixture("two", "Second")],
      limit: 10,
    };
    const requests: string[] = [];
    const user = userEvent.setup();

    renderFeed(<FeedPage />, {
      "/api/v1/feed?limit=10": firstPage,
      "/api/v1/feed?cursor=next-page&limit=10": secondPage,
      "/api/v1/categories": [],
    }, requests);

    expect(await screen.findByText("First")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Load more" }));

    expect(await screen.findByText("Second")).toBeInTheDocument();
    expect(requests.filter((path) => path.includes("cursor=next-page"))).toHaveLength(1);
  });

  it("mounts only the bounded video window around the active item", async () => {
    renderFeed(<FeedPage />, {
      "/api/v1/feed?limit=10": {
        items: [
          contentFixture("one", "First"),
          contentFixture("two", "Second"),
          contentFixture("three", "Third"),
        ],
        limit: 10,
      },
      "/api/v1/categories": [],
    });

    expect(await screen.findByText("First")).toBeInTheDocument();
    expect(screen.getAllByLabelText(/^Video:/u)).toHaveLength(2);
    expect(screen.getByText("Third")).toBeInTheDocument();
  });
});

describe("CreatorContentSection", () => {
  it("shows creator content from the public creator projection", async () => {
    renderFeed(<CreatorContentSection slug="mira-mini" />, {
      "/api/v1/creators/mira-mini/content?limit=12": {
        items: [contentFixture("one", "Creator video")],
        limit: 12,
      },
    });

    expect(await screen.findByText("Published videos")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Creator video/u })).toHaveAttribute(
      "href",
      "/content/one",
    );
  });

  it("shows the creator empty state", async () => {
    renderFeed(<CreatorContentSection slug="mira-mini" />, {
      "/api/v1/creators/mira-mini/content?limit=12": { items: [], limit: 12 },
    });

    expect(await screen.findByText("No published videos")).toBeInTheDocument();
  });
});

function renderFeed(
  ui: ReactElement,
  responses: Record<string, unknown>,
  requests: string[] = [],
): ReturnType<typeof render> {
  return renderWithProviders(ui, {
    apiClient: {
      request: <T,>(path: string): Promise<T> => {
        requests.push(path);
        if (path in responses) {
          return Promise.resolve(responses[path] as T);
        }
        return Promise.reject(new Error(`Unexpected path ${path}`));
      },
    } satisfies ApiClient,
  });
}

function contentFixture(id: string, title: string): ContentResponse {
  return {
    id,
    category: "EDUCATION",
    categoryLabel: "Education",
    creator: {
      displayName: "Mira Mini",
      slug: "mira-mini",
      userId: "8093c031-2a2e-4a67-8427-58ea6f2c5060",
      verified: true,
    },
    description: "A short public event video.",
    media: {
      contentType: "video/mp4",
      durationMs: 1000,
      expiresAt: "2026-07-17T12:10:00Z",
      sizeBytes: 1024,
      url: `http://localhost:9000/minis-event-media/content/imports/${id}.mp4`,
    },
    publishedAt: "2026-07-17T12:00:00Z",
    title,
  };
}
