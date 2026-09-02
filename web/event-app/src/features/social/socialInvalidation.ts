import type { QueryClient } from "@tanstack/react-query";

const SOCIAL_QUERY_ROOTS = new Set([
  "feed",
  "creator-content",
  "creators",
  "content-collection",
  "uploads",
]);

export function invalidateSocialSurfaces(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({
    predicate: (query) => SOCIAL_QUERY_ROOTS.has(String(query.queryKey[0])),
  });
}
