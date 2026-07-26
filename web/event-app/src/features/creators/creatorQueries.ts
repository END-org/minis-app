import { useQuery } from "@tanstack/react-query";
import { useApiClient } from "../../core/api/ApiClientContext";
import { creatorsQueryKey, getCreator, getCreators } from "./creatorApi";

export function useCreatorsQuery() {
  const client = useApiClient();
  return useQuery({
    queryKey: creatorsQueryKey,
    queryFn: () => getCreators(client),
    retry: false,
  });
}

export function useCreatorQuery(slug: string | undefined) {
  const client = useApiClient();
  return useQuery({
    enabled: Boolean(slug),
    queryKey: [...creatorsQueryKey, slug],
    queryFn: () => getCreator(client, slug ?? ""),
    retry: false,
  });
}
