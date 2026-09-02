import { useQuery } from "@tanstack/react-query";
import { useApiClient } from "../../core/api/ApiClientContext";
import { backendSessionQueryKey, useBackendSession } from "../account/BackendSessionContext";
import { creatorsQueryKey, getCreator, getCreators } from "./creatorApi";

export function useCreatorsQuery() {
  const client = useApiClient();
  const { state } = useBackendSession();
  return useQuery({
    queryKey: [...creatorsQueryKey, backendSessionQueryKey(state)],
    queryFn: () => getCreators(client),
    retry: false,
  });
}

export function useCreatorQuery(slug: string | undefined) {
  const client = useApiClient();
  const { state } = useBackendSession();
  return useQuery({
    enabled: Boolean(slug),
    queryKey: [...creatorsQueryKey, backendSessionQueryKey(state), slug],
    queryFn: () => getCreator(client, slug ?? ""),
    retry: false,
  });
}
