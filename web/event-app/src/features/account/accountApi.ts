import type { ApiClient } from "../../core/api/apiClient";

export type AccountRole = "ADMIN" | "CREATOR" | "VIEWER";
export type AccountStatus = "ACTIVE" | "DISABLED" | "SUSPENDED";

export interface AccountResponse {
  readonly id: string;
  readonly email?: string;
  readonly emailVerified: boolean;
  readonly displayName?: string;
  readonly avatarUrl?: string;
  readonly bio?: string;
  readonly role: AccountRole;
  readonly status: AccountStatus;
  readonly createdAt: string;
}

export const accountQueryKey = ["account", "current"] as const;

export function bootstrapCurrentAccount(client: ApiClient): Promise<AccountResponse> {
  return client.request<AccountResponse>("/api/v1/auth/bootstrap", {
    auth: "required",
    method: "POST",
    retryOnUnauthorized: true,
  });
}

export function getCurrentAccount(client: ApiClient): Promise<AccountResponse> {
  return client.request<AccountResponse>("/api/v1/me", {
    auth: "required",
    method: "GET",
  });
}
