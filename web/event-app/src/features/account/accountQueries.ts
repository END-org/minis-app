import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiClient } from "../../core/api/ApiClientContext";
import {
  accountQueryKey,
  accountQueryRootKey,
  bootstrapCurrentAccount,
  getCurrentAccount,
  updateCurrentProfile,
  type AccountResponse,
  type UpdateAccountProfileRequest,
} from "./accountApi";

export function useCurrentAccountQuery(firebaseUid: string | undefined, enabled = true) {
  const client = useApiClient();
  return useQuery({
    enabled: enabled && Boolean(firebaseUid),
    queryKey: firebaseUid ? accountQueryKey(firebaseUid) : accountQueryRootKey,
    queryFn: () => getCurrentAccount(client),
    retry: false,
  });
}

export function useBootstrapAccountMutation(firebaseUid: string) {
  const client = useApiClient();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => bootstrapCurrentAccount(client),
    onSuccess: (account: AccountResponse) => {
      queryClient.setQueryData(accountQueryKey(firebaseUid), account);
    },
    retry: false,
  });
}

export function useRefreshCurrentAccountMutation(firebaseUid: string) {
  const client = useApiClient();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => getCurrentAccount(client),
    onSuccess: (account: AccountResponse) => {
      queryClient.setQueryData(accountQueryKey(firebaseUid), account);
    },
    retry: false,
  });
}

export function useUpdateCurrentProfileMutation(firebaseUid: string | undefined) {
  const client = useApiClient();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpdateAccountProfileRequest) => updateCurrentProfile(client, request),
    onSuccess: (account: AccountResponse) => {
      if (firebaseUid) {
        queryClient.setQueryData(accountQueryKey(firebaseUid), account);
      }
    },
    retry: false,
  });
}
