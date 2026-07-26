import type { SessionNotice } from "../auth/AuthSessionContext";

export type ApiAuthMode = "none" | "optional" | "required";
export type HttpMethod = "DELETE" | "GET" | "PATCH" | "POST" | "PUT";

export interface ApiRequestOptions {
  readonly auth?: ApiAuthMode;
  readonly body?: unknown;
  readonly method?: HttpMethod;
  readonly retryOnUnauthorized?: boolean;
}

export interface ApiClient {
  request<T>(path: string, options?: ApiRequestOptions): Promise<T>;
}

export interface TokenUser {
  getIdToken(forceRefresh?: boolean): Promise<string>;
}

export interface TokenSource {
  currentUser(): TokenUser | null;
  expireAuthenticatedSession(notice: SessionNotice): Promise<void>;
}

export interface ApiErrorBody {
  readonly timestamp?: string;
  readonly status: number;
  readonly code: string;
  readonly message: string;
  readonly path?: string;
  readonly correlationId?: string;
}

export type ApiClientErrorKind = "api" | "auth" | "network";

interface ApiClientErrorArgs {
  readonly kind: ApiClientErrorKind;
  readonly message: string;
  readonly status?: number | undefined;
  readonly code?: string | undefined;
  readonly correlationId?: string | undefined;
  readonly path?: string | undefined;
  readonly cause?: unknown;
}

const IDEMPOTENT_METHODS = new Set<HttpMethod>(["GET"]);

export class ApiClientError extends Error {
  readonly kind: ApiClientErrorKind;
  readonly status: number | undefined;
  readonly code: string | undefined;
  readonly correlationId: string | undefined;
  readonly path: string | undefined;

  constructor(args: ApiClientErrorArgs) {
    super(args.message);
    this.name = "ApiClientError";
    this.kind = args.kind;
    this.status = args.status;
    this.code = args.code;
    this.correlationId = args.correlationId;
    this.path = args.path;
    if (args.cause) {
      this.cause = args.cause;
    }
  }
}

export function createApiClient(baseUrl: string, tokenSource: TokenSource): ApiClient {
  return {
    async request<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
      const method = options.method ?? "GET";
      const auth = options.auth ?? "optional";
      const canRetry = canRetryUnauthorized(method, options.retryOnUnauthorized);

      const firstResponse = await sendRequest(baseUrl, path, tokenSource, options, false);
      if (firstResponse.status !== 401) {
        return parseResponse<T>(firstResponse);
      }

      const currentUser = tokenSource.currentUser();
      if (auth === "none" || !currentUser) {
        throw await errorFromResponse(firstResponse);
      }

      if (!canRetry) {
        const error = await errorFromResponse(firstResponse);
        await expireSession(tokenSource);
        throw error;
      }

      let secondResponse: Response;
      try {
        secondResponse = await sendRequest(baseUrl, path, tokenSource, options, true);
      } catch (error) {
        await expireSession(tokenSource);
        throw authError("Authentication refresh failed. Please sign in again.", error);
      }

      if (secondResponse.status === 401) {
        const error = await errorFromResponse(secondResponse);
        await expireSession(tokenSource);
        throw error;
      }

      return parseResponse<T>(secondResponse);
    },
  };
}

async function sendRequest(
  baseUrl: string,
  path: string,
  tokenSource: TokenSource,
  options: ApiRequestOptions,
  forceRefresh: boolean,
): Promise<Response> {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  headers.set("X-Correlation-ID", createCorrelationId());

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (options.auth !== "none") {
    const user = tokenSource.currentUser();
    if (user) {
      const token = await user.getIdToken(forceRefresh);
      headers.set("Authorization", `Bearer ${token}`);
    } else if (options.auth === "required") {
      throw authError("Sign in is required before calling this endpoint.");
    }
  }

  try {
    const requestInit: RequestInit = {
      headers,
      method: options.method ?? "GET",
    };
    if (options.body !== undefined) {
      requestInit.body = JSON.stringify(options.body);
    }
    return await fetch(toUrl(baseUrl, path), requestInit);
  } catch (error) {
    throw new ApiClientError({
      kind: "network",
      message: "The MINIS API could not be reached. Check the connection and retry.",
      cause: error,
    });
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw await errorFromResponse(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const contentType = response.headers.get("Content-Type") ?? "";
  if (!contentType.includes("application/json")) {
    throw new ApiClientError({
      code: "NON_JSON_API_RESPONSE",
      kind: "network",
      message:
        "The MINIS API returned a non-JSON response. Check the API base URL and local proxy.",
      status: response.status,
    });
  }
  return (await response.json()) as T;
}

async function errorFromResponse(response: Response): Promise<ApiClientError> {
  const body = await readErrorBody(response);
  const correlationId = response.headers.get("X-Correlation-ID") ?? body?.correlationId;
  return new ApiClientError({
    kind: "api",
    status: response.status,
    code: body?.code ?? `HTTP_${String(response.status)}`,
    message: body?.message ?? fallbackMessage(response.status),
    path: body?.path,
    correlationId,
  });
}

async function readErrorBody(response: Response): Promise<ApiErrorBody | null> {
  const contentType = response.headers.get("Content-Type") ?? "";
  if (!contentType.includes("application/json")) {
    return null;
  }
  const value = (await response.json()) as unknown;
  return isApiErrorBody(value) ? value : null;
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (!isRecord(value)) {
    return false;
  }
  return (
    typeof value.status === "number" &&
    typeof value.code === "string" &&
    typeof value.message === "string"
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function canRetryUnauthorized(method: HttpMethod, retryOnUnauthorized: boolean | undefined): boolean {
  if (retryOnUnauthorized !== undefined) {
    return retryOnUnauthorized;
  }
  return IDEMPOTENT_METHODS.has(method);
}

async function expireSession(tokenSource: TokenSource): Promise<void> {
  await tokenSource.expireAuthenticatedSession({
    title: "Session expired",
    message: "Your Firebase session could not be refreshed. Please sign in again.",
  });
}

function authError(message: string, cause?: unknown): ApiClientError {
  const args = {
    kind: "auth" as const,
    status: 401,
    code: "MISSING_AUTH_TOKEN",
    message,
  };
  if (cause === undefined) {
    return new ApiClientError(args);
  }
  return new ApiClientError({
    ...args,
    cause,
  });
}

function fallbackMessage(status: number): string {
  if (status === 401) {
    return "Authentication is required.";
  }
  if (status === 403) {
    return "This MINIS account is not allowed to perform that action.";
  }
  return "The MINIS API returned an unexpected response.";
}

function toUrl(baseUrl: string, path: string): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${baseUrl}${normalizedPath}`;
}

function createCorrelationId(): string {
  return globalThis.crypto.randomUUID();
}
