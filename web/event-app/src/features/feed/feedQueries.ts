import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useApiClient } from "../../core/api/ApiClientContext";
import { backendSessionQueryKey, useBackendSession } from "../account/BackendSessionContext";
import {
  categoriesQueryKey,
  creatorContentQueryKey,
  feedQueryKey,
  getCategories,
  getContent,
  getCreatorContent,
  getFeed,
  getLikedContent,
  getSavedContent,
  likedContentQueryKey,
  savedContentQueryKey,
} from "./feedApi";

export function useFeedQuery(category: string | undefined) {
  const client = useApiClient();
  const { state } = useBackendSession();
  return useInfiniteQuery({
    initialPageParam: undefined as string | undefined,
    queryKey: [...feedQueryKey, backendSessionQueryKey(state), category],
    queryFn: ({ pageParam }) => getFeed(client, pageParam, category),
    getNextPageParam: (page) => page.nextCursor,
    retry: false,
    staleTime: 8 * 60 * 1000,
  });
}

export function useContentQuery(contentId: string | undefined) {
  const client = useApiClient();
  const { state } = useBackendSession();
  return useQuery({
    enabled: Boolean(contentId),
    queryKey: [...feedQueryKey, backendSessionQueryKey(state), "content", contentId],
    queryFn: () => getContent(client, contentId ?? ""),
    retry: false,
    staleTime: 8 * 60 * 1000,
  });
}

export function useCreatorContentQuery(slug: string | undefined) {
  const client = useApiClient();
  const { state } = useBackendSession();
  return useInfiniteQuery({
    enabled: Boolean(slug),
    initialPageParam: undefined as string | undefined,
    queryKey: [...creatorContentQueryKey, backendSessionQueryKey(state), slug],
    queryFn: ({ pageParam }) => getCreatorContent(client, slug ?? "", pageParam),
    getNextPageParam: (page) => page.nextCursor,
    retry: false,
    staleTime: 8 * 60 * 1000,
  });
}

export function useLikedContentQuery() {
  const client = useApiClient();
  const { state } = useBackendSession();
  return useInfiniteQuery({
    initialPageParam: undefined as string | undefined,
    queryKey: [...likedContentQueryKey, backendSessionQueryKey(state)],
    queryFn: ({ pageParam }) => getLikedContent(client, pageParam),
    getNextPageParam: (page) => page.nextCursor,
    retry: false,
    staleTime: 2 * 60 * 1000,
  });
}

export function useSavedContentQuery() {
  const client = useApiClient();
  const { state } = useBackendSession();
  return useInfiniteQuery({
    initialPageParam: undefined as string | undefined,
    queryKey: [...savedContentQueryKey, backendSessionQueryKey(state)],
    queryFn: ({ pageParam }) => getSavedContent(client, pageParam),
    getNextPageParam: (page) => page.nextCursor,
    retry: false,
    staleTime: 2 * 60 * 1000,
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
