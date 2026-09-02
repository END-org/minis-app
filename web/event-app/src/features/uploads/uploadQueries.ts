import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiClientError } from "../../core/api/apiClient";
import { useApiClient } from "../../core/api/ApiClientContext";
import { backendSessionQueryKey, useBackendSession } from "../account/BackendSessionContext";
import { useAuthSession } from "../../core/auth/AuthSessionContext";
import { invalidateSocialSurfaces } from "../social/socialInvalidation";
import {
  getMyUploads,
  uploadContentWithProgress,
  uploadsQueryKey,
  type ContentUploadResponse,
  type UploadContentMetadata,
  type UploadProgress,
} from "./uploadApi";

export interface UploadMutationInput {
  readonly file: File;
  readonly idempotencyKey: string;
  readonly metadata: UploadContentMetadata;
}

export function useMyUploadsQuery() {
  const client = useApiClient();
  const { state } = useBackendSession();
  return useInfiniteQuery({
    initialPageParam: undefined as string | undefined,
    queryKey: [...uploadsQueryKey, backendSessionQueryKey(state)],
    queryFn: ({ pageParam }) => getMyUploads(client, pageParam),
    getNextPageParam: (page) => page.nextCursor,
    retry: false,
    staleTime: 60 * 1000,
  });
}

export function useUploadContentMutation(onProgress: (progress: UploadProgress) => void) {
  const auth = useAuthSession();
  const queryClient = useQueryClient();
  return useMutation<ContentUploadResponse, unknown, UploadMutationInput>({
    mutationFn: async ({ file, idempotencyKey, metadata }) => {
      const user = auth.currentUser();
      if (!user) {
        throw new ApiClientError({
          code: "MISSING_AUTH_TOKEN",
          kind: "auth",
          message: "Sign in is required before uploading.",
          status: 401,
        });
      }
      try {
        return await uploadContentWithProgress({
          file,
          idempotencyKey,
          metadata,
          onProgress,
          token: await user.getIdToken(),
        });
      } catch (error) {
        if (!(error instanceof ApiClientError) || error.status !== 401) {
          throw error;
        }
        const refreshed = await user.getIdToken(true);
        const response = await uploadContentWithProgress({
          file,
          idempotencyKey,
          metadata,
          onProgress,
          token: refreshed,
        });
        return response;
      }
    },
    onSuccess: () => {
      invalidateSocialSurfaces(queryClient);
    },
  });
}
