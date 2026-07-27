import type { ApiClient } from "../../core/api/apiClient";
import type { AccountRole, AccountStatus } from "../account/accountApi";

export interface AdminCreatorProfileResponse {
  readonly userId: string;
  readonly slug: string;
  readonly displayName: string;
  readonly bio?: string;
  readonly avatarUrl?: string;
  readonly verified: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface AdminUserResponse {
  readonly id: string;
  readonly email?: string;
  readonly displayName?: string;
  readonly avatarUrl?: string;
  readonly bio?: string;
  readonly role: AccountRole;
  readonly status: AccountStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly creatorProfile?: AdminCreatorProfileResponse;
}

export interface AdminUserListResponse {
  readonly users: readonly AdminUserResponse[];
  readonly page: number;
  readonly size: number;
  readonly hasNext: boolean;
}

export interface GrantCreatorRoleRequest {
  readonly slug: string;
  readonly displayName: string;
  readonly bio: string;
  readonly avatarUrl: string;
  readonly reason: string;
}

export interface AdminReasonRequest {
  readonly reason: string;
}

export const adminUsersQueryKey = ["admin", "users"] as const;

export function getAdminUsers(client: ApiClient, query: string): Promise<AdminUserListResponse> {
  const search = new URLSearchParams();
  if (query.trim()) {
    search.set("query", query.trim());
  }
  const suffix = search.toString() ? `?${search.toString()}` : "";
  return client.request<AdminUserListResponse>(`/api/v1/admin/users${suffix}`, {
    auth: "required",
    method: "GET",
  });
}

export function getAdminUser(client: ApiClient, userId: string): Promise<AdminUserResponse> {
  return client.request<AdminUserResponse>(`/api/v1/admin/users/${userId}`, {
    auth: "required",
    method: "GET",
  });
}

export function grantCreatorRole(
  client: ApiClient,
  userId: string,
  request: GrantCreatorRoleRequest,
): Promise<AdminUserResponse> {
  return client.request<AdminUserResponse>(`/api/v1/admin/users/${userId}/creator-role`, {
    auth: "required",
    body: request,
    method: "PUT",
    retryOnUnauthorized: false,
  });
}

export function revokeCreatorRole(
  client: ApiClient,
  userId: string,
  request: AdminReasonRequest,
): Promise<AdminUserResponse> {
  return client.request<AdminUserResponse>(`/api/v1/admin/users/${userId}/creator-role`, {
    auth: "required",
    body: request,
    method: "DELETE",
    retryOnUnauthorized: false,
  });
}

export function suspendUser(
  client: ApiClient,
  userId: string,
  request: AdminReasonRequest,
): Promise<AdminUserResponse> {
  return client.request<AdminUserResponse>(`/api/v1/admin/users/${userId}/suspension`, {
    auth: "required",
    body: request,
    method: "PUT",
    retryOnUnauthorized: false,
  });
}

export function reactivateUser(
  client: ApiClient,
  userId: string,
  request: AdminReasonRequest,
): Promise<AdminUserResponse> {
  return client.request<AdminUserResponse>(`/api/v1/admin/users/${userId}/suspension`, {
    auth: "required",
    body: request,
    method: "DELETE",
    retryOnUnauthorized: false,
  });
}

export function verifyCreator(
  client: ApiClient,
  userId: string,
  request: AdminReasonRequest,
): Promise<AdminUserResponse> {
  return client.request<AdminUserResponse>(`/api/v1/admin/creators/${userId}/verification`, {
    auth: "required",
    body: request,
    method: "PUT",
    retryOnUnauthorized: false,
  });
}

export function unverifyCreator(
  client: ApiClient,
  userId: string,
  request: AdminReasonRequest,
): Promise<AdminUserResponse> {
  return client.request<AdminUserResponse>(`/api/v1/admin/creators/${userId}/verification`, {
    auth: "required",
    body: request,
    method: "DELETE",
    retryOnUnauthorized: false,
  });
}
