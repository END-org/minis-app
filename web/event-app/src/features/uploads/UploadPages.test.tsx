import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import type { ApiClient } from "../../core/api/apiClient";
import type { BackendSessionContextValue } from "../account/BackendSessionContext";
import { renderWithProviders } from "../../test/renderWithProviders";
import type { ContentUploadPageResponse } from "./uploadApi";
import { MyUploadsPage } from "./MyUploadsPage";
import { UploadPage } from "./UploadPage";

describe("UploadPage", () => {
  it("rejects an unsupported selected file before submitting a multipart request", async () => {
    const user = userEvent.setup();
    renderWithProviders(<UploadPage />, {
      backendSession: creatorSession(),
      apiClient: {
        request: <T,>(path: string): Promise<T> => {
          if (path === "/api/v1/categories") {
            return Promise.resolve([{ label: "Education", value: "EDUCATION" }] as T);
          }
          return Promise.reject(new Error(`Unexpected path ${path}`));
        },
      } satisfies ApiClient,
    });

    expect(await screen.findByRole("heading", { name: "Upload Event Video" })).toBeInTheDocument();
    const input = screen.getByLabelText("Choose MP4 video");
    await user.upload(input, new File(["not-mp4"], "clip.mp4", { type: "video/webm" }));

    expect(
      screen.getByText("The browser must identify the selected file as video/mp4."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Publish video" })).toBeDisabled();
  });
});

describe("MyUploadsPage", () => {
  it("shows terminal failures privately and links published uploads to their content", async () => {
    const uploads: ContentUploadPageResponse = {
      items: [
        {
          category: "EDUCATION",
          categoryLabel: "Education",
          createdAt: "2026-09-01T12:00:00Z",
          description: "Needs a new encode.",
          failureCode: "UNSUPPORTED_MEDIA_PROFILE",
          failureReason: "The video does not match the accepted MINIS media profile.",
          id: "failed-upload",
          status: "FAILED",
          title: "Rejected upload",
        },
        {
          category: "EDUCATION",
          categoryLabel: "Education",
          content: { id: "published-content" } as never,
          createdAt: "2026-09-01T12:01:00Z",
          description: "Ready to watch.",
          id: "published-upload",
          status: "PUBLISHED",
          title: "Published upload",
        },
      ],
      size: 10,
    };
    renderWithProviders(<MyUploadsPage />, {
      backendSession: creatorSession(),
      apiClient: {
        request: <T,>(path: string): Promise<T> => {
          if (path === "/api/v1/me/uploads?size=10") {
            return Promise.resolve(uploads as T);
          }
          return Promise.reject(new Error(`Unexpected path ${path}`));
        },
      } satisfies ApiClient,
    });

    expect(await screen.findByText("Rejected upload")).toBeInTheDocument();
    expect(
      screen.getByText("The video does not match the accepted MINIS media profile."),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Start a new attempt" })).toHaveAttribute(
      "href",
      "/upload",
    );
    expect(screen.getByRole("link", { name: "Open video" })).toHaveAttribute(
      "href",
      "/content/published-content",
    );
  });
});

function creatorSession(): BackendSessionContextValue {
  return {
    replaceAccount: () => undefined,
    retryProvisioning: () => undefined,
    state: {
      account: {
        createdAt: "2026-09-01T12:00:00Z",
        emailVerified: true,
        id: "creator-1",
        role: "CREATOR",
        status: "ACTIVE",
      },
      status: "ready",
    },
  };
}
