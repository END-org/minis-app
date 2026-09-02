import type { ApiClient } from "../../core/api/apiClient";

export interface CreatorResponse {
  readonly userId: string;
  readonly slug: string;
  readonly displayName: string;
  readonly bio?: string;
  readonly avatarUrl?: string;
  readonly verified: boolean;
  readonly following: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreatorDirectoryResponse {
  readonly creators: readonly CreatorResponse[];
  readonly page: number;
  readonly size: number;
  readonly hasNext: boolean;
}

export const creatorsQueryKey = ["creators"] as const;

export function getCreators(client: ApiClient): Promise<CreatorDirectoryResponse> {
  return client.request<CreatorDirectoryResponse>("/api/v1/creators", {
    auth: "optional",
    method: "GET",
  });
}

export function getCreator(client: ApiClient, slug: string): Promise<CreatorResponse> {
  return client.request<CreatorResponse>(`/api/v1/creators/${encodeURIComponent(slug)}`, {
    auth: "optional",
    method: "GET",
  });
}

export function setCreatorFollow(
  client: ApiClient,
  slug: string,
  following: boolean,
): Promise<void> {
  return client
    .request<undefined>(`/api/v1/creators/${encodeURIComponent(slug)}/follow`, {
      auth: "required",
      method: following ? "PUT" : "DELETE",
      retryOnUnauthorized: false,
    })
    .then(() => undefined);
}
