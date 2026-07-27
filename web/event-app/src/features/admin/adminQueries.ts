import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useApiClient } from "../../core/api/ApiClientContext";
import {
  adminUsersQueryKey,
  getAdminUser,
  getAdminUsers,
  grantCreatorRole,
  reactivateUser,
  revokeCreatorRole,
  suspendUser,
  unverifyCreator,
  verifyCreator,
  type AdminReasonRequest,
  type AdminUserResponse,
  type GrantCreatorRoleRequest,
} from "./adminApi";

export function useAdminUsersQuery(query: string) {
  const client = useApiClient();
  return useQuery({
    queryKey: [...adminUsersQueryKey, query],
    queryFn: () => getAdminUsers(client, query),
    retry: false,
  });
}

export function useAdminUserQuery(userId: string | undefined) {
  const client = useApiClient();
  return useQuery({
    enabled: Boolean(userId),
    queryKey: [...adminUsersQueryKey, "detail", userId],
    queryFn: () => getAdminUser(client, userId ?? ""),
    retry: false,
  });
}

export function useGrantCreatorRoleMutation(userId: string) {
  const client = useApiClient();
  return useAdminUserMutation(userId, (request: GrantCreatorRoleRequest) =>
    grantCreatorRole(client, userId, request),
  );
}

export function useRevokeCreatorRoleMutation(userId: string) {
  const client = useApiClient();
  return useAdminUserMutation(userId, (request: AdminReasonRequest) =>
    revokeCreatorRole(client, userId, request),
  );
}

export function useSuspendUserMutation(userId: string) {
  const client = useApiClient();
  return useAdminUserMutation(userId, (request: AdminReasonRequest) =>
    suspendUser(client, userId, request),
  );
}

export function useReactivateUserMutation(userId: string) {
  const client = useApiClient();
  return useAdminUserMutation(userId, (request: AdminReasonRequest) =>
    reactivateUser(client, userId, request),
  );
}

export function useVerifyCreatorMutation(userId: string) {
  const client = useApiClient();
  return useAdminUserMutation(userId, (request: AdminReasonRequest) =>
    verifyCreator(client, userId, request),
  );
}

export function useUnverifyCreatorMutation(userId: string) {
  const client = useApiClient();
  return useAdminUserMutation(userId, (request: AdminReasonRequest) =>
    unverifyCreator(client, userId, request),
  );
}

function useAdminUserMutation<TRequest>(
  userId: string,
  mutationFn: (request: TRequest) => Promise<AdminUserResponse>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (user: AdminUserResponse) => {
      queryClient.setQueryData([...adminUsersQueryKey, "detail", userId], user);
      void queryClient.invalidateQueries({ queryKey: adminUsersQueryKey });
    },
    retry: false,
  });
}
