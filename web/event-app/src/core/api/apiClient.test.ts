import { describe, expect, it, vi, type Mock } from "vitest";
import { ApiClientError, createApiClient, type TokenSource, type TokenUser } from "./apiClient";

interface TestTokenUser extends TokenUser {
  readonly getIdTokenMock: Mock<(forceRefresh?: boolean) => Promise<string>>;
}

describe("createApiClient", () => {
  it("attaches the current Firebase token", async () => {
    const fetchMock = mockFetch(jsonResponse(200, { role: "VIEWER" }));
    const user = tokenUser(["viewer-token"]);
    const client = createApiClient("", tokenSource(user));

    await client.request("/api/v1/me", { auth: "required" });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(requestHeaders(fetchMock).get("Authorization")).toBe("Bearer viewer-token");
    expect(requestHeaders(fetchMock).get("X-Correlation-ID")).toBeTruthy();
  });

  it("parses API errors and correlation IDs", async () => {
    mockFetch(
      jsonResponse(
        403,
        {
          status: 403,
          code: "ACCOUNT_DISABLED",
          message: "MINIS account is disabled.",
          path: "/api/v1/me",
          correlationId: "body-correlation",
        },
        "header-correlation",
      ),
    );
    const client = createApiClient("", tokenSource(tokenUser(["viewer-token"])));

    await expect(client.request("/api/v1/me", { auth: "required" })).rejects.toMatchObject({
      code: "ACCOUNT_DISABLED",
      correlationId: "header-correlation",
      status: 403,
    });
  });

  it("force-refreshes and retries the first safe 401 exactly once", async () => {
    const fetchMock = mockFetch(
      jsonResponse(401, {
        status: 401,
        code: "EXPIRED_AUTH_TOKEN",
        message: "Authentication token has expired.",
      }),
      jsonResponse(200, { role: "VIEWER" }),
    );
    const user = tokenUser(["stale-token", "fresh-token"]);
    const clearSession = vi.fn<() => Promise<void>>(() => Promise.resolve());
    const client = createApiClient("", tokenSource(user, clearSession));

    await expect(client.request("/api/v1/me", { auth: "required" })).resolves.toEqual({
      role: "VIEWER",
    });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(user.getIdTokenMock).toHaveBeenNthCalledWith(1, false);
    expect(user.getIdTokenMock).toHaveBeenNthCalledWith(2, true);
    expect(clearSession).not.toHaveBeenCalled();
  });

  it("ends the authenticated session when the retried request still returns 401", async () => {
    mockFetch(
      jsonResponse(401, {
        status: 401,
        code: "EXPIRED_AUTH_TOKEN",
        message: "Authentication token has expired.",
      }),
      jsonResponse(401, {
        status: 401,
        code: "INVALID_AUTH_TOKEN",
        message: "Authentication token is invalid.",
      }),
    );
    const user = tokenUser(["stale-token", "fresh-token"]);
    const clearSession = vi.fn<() => Promise<void>>(() => Promise.resolve());
    const client = createApiClient("", tokenSource(user, clearSession));

    await expect(client.request("/api/v1/me", { auth: "required" })).rejects.toMatchObject({
      code: "INVALID_AUTH_TOKEN",
      status: 401,
    });

    expect(clearSession).toHaveBeenCalledTimes(1);
    expect(user.getIdTokenMock).toHaveBeenCalledTimes(2);
  });

  it("does not automatically retry unsafe non-idempotent mutations", async () => {
    const fetchMock = mockFetch(
      jsonResponse(401, {
        status: 401,
        code: "EXPIRED_AUTH_TOKEN",
        message: "Authentication token has expired.",
      }),
    );
    const user = tokenUser(["stale-token"]);
    const clearSession = vi.fn<() => Promise<void>>(() => Promise.resolve());
    const client = createApiClient("", tokenSource(user, clearSession));

    await expect(
      client.request("/api/v1/something", { auth: "required", method: "POST" }),
    ).rejects.toBeInstanceOf(ApiClientError);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(user.getIdTokenMock).toHaveBeenCalledTimes(1);
    expect(clearSession).toHaveBeenCalledTimes(1);
  });

  it("does not refresh tokens or sign out on 403", async () => {
    const fetchMock = mockFetch(
      jsonResponse(403, {
        status: 403,
        code: "ACCOUNT_SUSPENDED",
        message: "MINIS account is suspended.",
      }),
    );
    const user = tokenUser(["viewer-token"]);
    const clearSession = vi.fn<() => Promise<void>>(() => Promise.resolve());
    const client = createApiClient("", tokenSource(user, clearSession));

    await expect(client.request("/api/v1/me", { auth: "required" })).rejects.toMatchObject({
      code: "ACCOUNT_SUSPENDED",
      status: 403,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(user.getIdTokenMock).toHaveBeenCalledWith(false);
    expect(clearSession).not.toHaveBeenCalled();
  });
});

function mockFetch(...responses: Response[]) {
  const fetchMock = vi.fn<typeof fetch>();
  for (const response of responses) {
    fetchMock.mockResolvedValueOnce(response);
  }
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function jsonResponse(status: number, body: unknown, correlationId?: string): Response {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (correlationId) {
    headers.set("X-Correlation-ID", correlationId);
  }
  return new Response(JSON.stringify(body), { headers, status });
}

function tokenUser(tokens: readonly string[]): TestTokenUser {
  const getIdToken = vi.fn<(forceRefresh?: boolean) => Promise<string>>();
  for (const token of tokens) {
    getIdToken.mockResolvedValueOnce(token);
  }
  return { getIdToken, getIdTokenMock: getIdToken };
}

function tokenSource(
  user: TokenUser,
  expireAuthenticatedSession = vi.fn<() => Promise<void>>(() => Promise.resolve()),
): TokenSource {
  return {
    currentUser: () => user,
    expireAuthenticatedSession,
  };
}

function requestHeaders(fetchMock: ReturnType<typeof vi.fn<typeof fetch>>): Headers {
  const firstCall = fetchMock.mock.calls.at(0);
  if (!firstCall) {
    throw new Error("No fetch call was recorded.");
  }
  const init = firstCall[1];
  return new Headers(init?.headers);
}
