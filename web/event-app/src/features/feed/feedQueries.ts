import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useApiClient } from "../../core/api/ApiClientContext";
import {
  categoriesQueryKey,
  creatorContentQueryKey,
  feedQueryKey,
  getCategories,
  getContent,
  getCreatorContent,
  getFeed,
} from "./feedApi";

export function useFeedQuery(category: string | undefined) {
  const client = useApiClient();
  return useInfiniteQuery({
    initialPageParam: undefined as string | undefined,
    queryKey: [...feedQueryKey, category],
    queryFn: ({ pageParam }) => getFeed(client, pageParam, category),
    getNextPageParam: (page) => page.nextCursor,
    retry: false,
    staleTime: 8 * 60 * 1000,
  });
}

export function useContentQuery(contentId: string | undefined) {
  const client = useApiClient();
  return useQuery({
    enabled: Boolean(contentId),
    queryKey: [...feedQueryKey, "content", contentId],
    queryFn: () => getContent(client, contentId ?? ""),
    retry: false,
    staleTime: 8 * 60 * 1000,
  });
}

export function useCreatorContentQuery(slug: string | undefined) {
  const client = useApiClient();
  return useInfiniteQuery({
    enabled: Boolean(slug),
    initialPageParam: undefined as string | undefined,
    queryKey: [...creatorContentQueryKey, slug],
    queryFn: ({ pageParam }) => getCreatorContent(client, slug ?? "", pageParam),
    getNextPageParam: (page) => page.nextCursor,
    retry: false,
    staleTime: 8 * 60 * 1000,
  });
}

export function useCategoriesQuery() {
  const client = useApiClient();
  return useQuery({
    queryKey: categoriesQueryKey,
    queryFn: () => getCategories(client),
    retry: false,
    staleTime: 30 * 60 * 1000,
  });
}
