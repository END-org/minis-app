import { ApiClientError } from "../api/apiClient";

export interface ApiFailureView {
  readonly code: string;
  readonly correlationId?: string;
  readonly message: string;
  readonly status?: number;
}

export function toApiFailureView(error: unknown): ApiFailureView {
  if (error instanceof ApiClientError) {
    return {
      code: error.code ?? error.kind.toUpperCase(),
      ...(error.correlationId ? { correlationId: error.correlationId } : {}),
      message: error.message,
      ...(error.status ? { status: error.status } : {}),
    };
  }
  return {
    code: "CLIENT_ERROR",
    message: error instanceof Error ? error.message : "Something went wrong.",
  };
}
