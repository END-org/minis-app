import type { ApiClient } from "../../core/api/apiClient";

export interface ContentCreatorResponse {
  readonly userId: string;
  readonly slug: string;
  readonly displayName: string;
  readonly avatarUrl?: string;
  readonly verified: boolean;
}

export interface ContentMediaResponse {
  readonly url: string;
  readonly expiresAt: string;
  readonly contentType: string;
  readonly sizeBytes: number;
  readonly durationMs?: number;
}

export interface ContentResponse {
  readonly id: string;
  readonly creator: ContentCreatorResponse;
  readonly title: string;
  readonly description: string;
  readonly category: string;
  readonly categoryLabel: string;
  readonly media: ContentMediaResponse;
  readonly publishedAt: string;
  readonly likeCount: number;
  readonly liked: boolean;
  readonly saved: boolean;
  readonly following: boolean;
}

export interface ContentPageResponse {
  readonly items: readonly ContentResponse[];
  readonly nextCursor?: string;
  readonly limit: number;
}

export interface CategoryResponse {
  readonly value: string;
  readonly label: string;
}

export const feedQueryKey = ["feed"] as const;
export const categoriesQueryKey = ["categories"] as const;
export const creatorContentQueryKey = ["creator-content"] as const;
export const likedContentQueryKey = ["content-collection", "liked"] as const;
export const savedContentQueryKey = ["content-collection", "saved"] as const;

export function getFeed(
  client: ApiClient,
  cursor: string | undefined,
  category: string | undefined,
): Promise<ContentPageResponse> {
  return client.request<ContentPageResponse>(
    toPath("/api/v1/feed", {
      category,
      cursor,
      limit: "10",
    }),
    { auth: "optional", method: "GET" },
  );
}

export function getContent(client: ApiClient, contentId: string): Promise<ContentResponse> {
  return client.request<ContentResponse>(`/api/v1/content/${encodeURIComponent(contentId)}`, {
    auth: "optional",
    method: "GET",
  });
}

export function getCreatorContent(
  client: ApiClient,
  slug: string,
  cursor: string | undefined,
): Promise<ContentPageResponse> {
  return client.request<ContentPageResponse>(
    toPath(`/api/v1/creators/${encodeURIComponent(slug)}/content`, {
      cursor,
      limit: "12",
    }),
    { auth: "optional", method: "GET" },
  );
}

export function getCategories(client: ApiClient): Promise<readonly CategoryResponse[]> {
  return client.request<readonly CategoryResponse[]>("/api/v1/categories", {
    auth: "none",
    method: "GET",
  });
}

export function getLikedContent(
  client: ApiClient,
  cursor: string | undefined,
): Promise<ContentPageResponse> {
  return client.request<ContentPageResponse>(
    toPath("/api/v1/me/liked-content", {
      cursor,
      size: "12",
    }),
    { auth: "required", method: "GET" },
  );
}

export function getSavedContent(
  client: ApiClient,
  cursor: string | undefined,
): Promise<ContentPageResponse> {
  return client.request<ContentPageResponse>(
    toPath("/api/v1/me/saved-content", {
      cursor,
      size: "12",
    }),
    { auth: "required", method: "GET" },
  );
}

export function setContentLike(
  client: ApiClient,
  contentId: string,
  liked: boolean,
): Promise<void> {
  return client
    .request<undefined>(`/api/v1/content/${encodeURIComponent(contentId)}/like`, {
      auth: "required",
      method: liked ? "PUT" : "DELETE",
      retryOnUnauthorized: false,
    })
    .then(() => undefined);
}

export function setContentSave(
  client: ApiClient,
  contentId: string,
  saved: boolean,
): Promise<void> {
  return client
    .request<undefined>(`/api/v1/content/${encodeURIComponent(contentId)}/save`, {
      auth: "required",
      method: saved ? "PUT" : "DELETE",
      retryOnUnauthorized: false,
    })
    .then(() => undefined);
}

function toPath(path: string, params: Record<string, string | undefined>): string {
  const searchParams = new URLSearchParams();
  for (const [name, value] of Object.entries(params)) {
    if (value !== undefined && value.trim().length > 0) {
      searchParams.set(name, value);
    }
  }
  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
}
