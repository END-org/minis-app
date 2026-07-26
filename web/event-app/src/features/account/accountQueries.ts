import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiClient } from "../../core/api/ApiClientContext";
import {
  accountQueryKey,
  bootstrapCurrentAccount,
  getCurrentAccount,
  type AccountResponse,
} from "./accountApi";

export function useCurrentAccountQuery() {
  const client = useApiClient();
  return useQuery({
    queryKey: accountQueryKey,
    queryFn: () => getCurrentAccount(client),
    retry: false,
  });
}

export function useBootstrapAccountMutation() {
  const client = useApiClient();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => bootstrapCurrentAccount(client),
    onSuccess: (account: AccountResponse) => {
      queryClient.setQueryData(accountQueryKey, account);
    },
    retry: false,
  });
}
