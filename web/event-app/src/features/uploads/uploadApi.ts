import { ApiClientError, type ApiClient, type ApiErrorBody } from "../../core/api/apiClient";
import { eventAppConfig } from "../../core/config/environment";
import type { ContentResponse } from "../feed/feedApi";

export type ContentUploadStatus = "FAILED" | "PUBLISHED" | "REMOVED" | "UPLOADING";

export interface ContentUploadMediaResponse {
  readonly contentType?: string;
  readonly sizeBytes?: number;
  readonly durationMs?: number;
}

export interface ContentUploadResponse {
  readonly id: string;
  readonly title: string;
  readonly description: string;
  readonly category: string;
  readonly categoryLabel: string;
  readonly status: ContentUploadStatus;
  readonly failureCode?: string;
  readonly failureReason?: string;
  readonly createdAt: string;
  readonly uploadStartedAt?: string;
  readonly uploadCompletedAt?: string;
  readonly publishedAt?: string;
  readonly media?: ContentUploadMediaResponse;
  readonly content?: ContentResponse;
}

export interface ContentUploadPageResponse {
  readonly items: readonly ContentUploadResponse[];
  readonly nextCursor?: string;
  readonly size: number;
}

export interface UploadContentMetadata {
  readonly title: string;
  readonly description: string;
  readonly category: string;
}

export interface UploadContentRequest {
  readonly file: File;
  readonly idempotencyKey: string;
  readonly metadata: UploadContentMetadata;
  readonly onProgress?: (progress: UploadProgress) => void;
  readonly token: string;
}

export interface UploadProgress {
  readonly loaded: number;
  readonly percent: number;
  readonly total: number;
}

export const uploadsQueryKey = ["uploads"] as const;
export const MAX_UPLOAD_BYTES = 200 * 1024 * 1024;

export function getMyUploads(
  client: ApiClient,
  cursor: string | undefined,
): Promise<ContentUploadPageResponse> {
  const searchParams = new URLSearchParams();
  searchParams.set("size", "10");
  if (cursor) {
    searchParams.set("cursor", cursor);
  }
  return client.request<ContentUploadPageResponse>(`/api/v1/me/uploads?${searchParams}`, {
    auth: "required",
    method: "GET",
  });
}

export function uploadContentWithProgress({
  file,
  idempotencyKey,
  metadata,
  onProgress,
  token,
}: UploadContentRequest): Promise<ContentUploadResponse> {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open("POST", toUrl("/api/v1/content"));
    request.setRequestHeader("Accept", "application/json");
    request.setRequestHeader("Authorization", `Bearer ${token}`);
    request.setRequestHeader("Idempotency-Key", idempotencyKey);
    request.setRequestHeader("X-Correlation-ID", globalThis.crypto.randomUUID());
    request.upload.addEventListener("progress", (event) => {
      if (!event.lengthComputable) {
        return;
      }
      onProgress?.({
        loaded: event.loaded,
        percent: Math.round((event.loaded / event.total) * 100),
        total: event.total,
      });
    });
    request.addEventListener("load", () => {
      const contentType = request.getResponseHeader("Content-Type") ?? "";
      if (request.status >= 200 && request.status < 300) {
        if (!contentType.includes("application/json")) {
          reject(nonJsonError(request.status));
          return;
        }
        resolve(JSON.parse(request.responseText) as ContentUploadResponse);
        return;
      }
      reject(errorFromUploadResponse(request));
    });
    request.addEventListener("error", () => {
      reject(
        new ApiClientError({
          kind: "network",
          message: "The video upload could not reach the MINIS API.",
        }),
      );
    });
    request.addEventListener("abort", () => {
      reject(
        new ApiClientError({
          code: "UPLOAD_ABORTED",
          kind: "network",
          message: "The video upload was canceled.",
        }),
      );
    });

    const body = new FormData();
    body.append(
      "metadata",
      new Blob([JSON.stringify(metadata)], { type: "application/json" }),
      "metadata.json",
    );
    body.append("video", file, file.name);
    request.send(body);
  });
}

function errorFromUploadResponse(request: XMLHttpRequest): ApiClientError {
  const body = parseErrorBody(request.responseText);
  return new ApiClientError({
    code: body?.code ?? `HTTP_${String(request.status)}`,
    correlationId: request.getResponseHeader("X-Correlation-ID") ?? body?.correlationId,
    kind: "api",
    message: body?.message ?? fallbackMessage(request.status),
    path: body?.path,
    status: request.status,
  });
}

function parseErrorBody(responseText: string): ApiErrorBody | null {
  if (!responseText) {
    return null;
  }
  try {
    const value = JSON.parse(responseText) as Partial<ApiErrorBody>;
    if (typeof value.code === "string" && typeof value.message === "string") {
      return {
        code: value.code,
        message: value.message,
        status: typeof value.status === "number" ? value.status : 0,
        ...(typeof value.correlationId === "string"
          ? { correlationId: value.correlationId }
          : {}),
        ...(typeof value.path === "string" ? { path: value.path } : {}),
        ...(typeof value.timestamp === "string" ? { timestamp: value.timestamp } : {}),
      };
    }
  } catch {
    return null;
  }
  return null;
}

function nonJsonError(status: number): ApiClientError {
  return new ApiClientError({
    code: "NON_JSON_API_RESPONSE",
    kind: "network",
    message: "The MINIS API returned a non-JSON upload response.",
    status,
  });
}

function fallbackMessage(status: number): string {
  if (status === 413) {
    return "The selected video is larger than the MINIS event limit.";
  }
  if (status === 401) {
    return "Sign in is required before uploading.";
  }
  if (status === 403) {
    return "This MINIS account is not allowed to upload videos.";
  }
  return "The MINIS API could not finish the upload.";
}

function toUrl(path: string): string {
  return `${eventAppConfig.apiBaseUrl}${path}`;
}
