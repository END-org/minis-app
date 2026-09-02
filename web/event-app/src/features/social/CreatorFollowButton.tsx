import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useApiClient } from "../../core/api/ApiClientContext";
import { toApiFailureView, type ApiFailureView } from "../../core/errors/apiError";
import { useBackendSession } from "../account/BackendSessionContext";
import { setCreatorFollow, type CreatorResponse } from "../creators/creatorApi";
import { SocialAuthPrompt } from "./SocialAuthPrompt";
import { invalidateSocialSurfaces } from "./socialInvalidation";

interface CreatorFollowButtonProps {
  readonly creator: CreatorResponse;
}

export function CreatorFollowButton({ creator }: CreatorFollowButtonProps) {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const { state } = useBackendSession();
  const [showPrompt, setShowPrompt] = useState(false);
  const [failure, setFailure] = useState<ApiFailureView | null>(null);
  const isReady = state.status === "ready";

  const follow = useMutation({
    mutationFn: (nextFollowing: boolean) => setCreatorFollow(client, creator.slug, nextFollowing),
    onSuccess: () => {
      setFailure(null);
      setShowPrompt(false);
      invalidateSocialSurfaces(queryClient);
    },
    onError: (error) => {
      setFailure(toApiFailureView(error));
    },
  });

  if (isReady && state.account?.id === creator.userId) {
    return <span className="social-button social-button--static">Your creator profile</span>;
  }

  return (
    <div className="creator-follow">
      <button
        type="button"
        className={creator.following ? "social-button is-active" : "social-button"}
        disabled={follow.isPending}
        aria-pressed={creator.following}
        onClick={() => {
          if (!isReady) {
            setFailure(null);
            setShowPrompt(true);
            return;
          }
          follow.mutate(!creator.following);
        }}
      >
        <span>{creator.following ? "Following" : "Follow"}</span>
      </button>
      {showPrompt ? <SocialAuthPrompt action="follow creators" /> : null}
      {failure ? (
        <p className="social-feedback" role="alert">
          {failure.message}
        </p>
      ) : null}
    </div>
  );
}
