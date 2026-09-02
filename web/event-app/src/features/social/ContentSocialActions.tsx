import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useApiClient } from "../../core/api/ApiClientContext";
import { toApiFailureView, type ApiFailureView } from "../../core/errors/apiError";
import { useBackendSession } from "../account/BackendSessionContext";
import { setCreatorFollow } from "../creators/creatorApi";
import { setContentLike, setContentSave, type ContentResponse } from "../feed/feedApi";
import { SocialAuthPrompt } from "./SocialAuthPrompt";
import { invalidateSocialSurfaces } from "./socialInvalidation";

interface ContentSocialActionsProps {
  readonly content: ContentResponse;
  readonly compact?: boolean;
}

export function ContentSocialActions({ compact = false, content }: ContentSocialActionsProps) {
  const client = useApiClient();
  const queryClient = useQueryClient();
  const { state } = useBackendSession();
  const [authAction, setAuthAction] = useState<string | null>(null);
  const [failure, setFailure] = useState<ApiFailureView | null>(null);
  const isReady = state.status === "ready";
  const isOwnCreator = isReady && state.account?.id === content.creator.userId;

  const like = useMutation({
    mutationFn: (nextLiked: boolean) => setContentLike(client, content.id, nextLiked),
    onSuccess: () => {
      setFailure(null);
      invalidateSocialSurfaces(queryClient);
    },
    onError: (error) => {
      setFailure(toApiFailureView(error));
    },
  });
  const save = useMutation({
    mutationFn: (nextSaved: boolean) => setContentSave(client, content.id, nextSaved),
    onSuccess: () => {
      setFailure(null);
      invalidateSocialSurfaces(queryClient);
    },
    onError: (error) => {
      setFailure(toApiFailureView(error));
    },
  });
  const follow = useMutation({
    mutationFn: (nextFollowing: boolean) =>
      setCreatorFollow(client, content.creator.slug, nextFollowing),
    onSuccess: () => {
      setFailure(null);
      invalidateSocialSurfaces(queryClient);
    },
    onError: (error) => {
      setFailure(toApiFailureView(error));
    },
  });

  function requireReady(action: string): boolean {
    if (isReady) {
      setAuthAction(null);
      return true;
    }
    setFailure(null);
    setAuthAction(action);
    return false;
  }

  const disabled = like.isPending || save.isPending || follow.isPending;

  return (
    <div className={compact ? "social-actions social-actions--compact" : "social-actions"}>
      <div className="social-actions__row" aria-label={`Social actions for ${content.title}`}>
        <button
          type="button"
          className={content.liked ? "social-button is-active" : "social-button"}
          disabled={disabled}
          aria-label={`${content.liked ? "Liked" : "Like"} ${formatCount(content.likeCount)}`}
          aria-pressed={content.liked}
          onClick={() => {
            if (requireReady("like videos")) {
              like.mutate(!content.liked);
            }
          }}
        >
          <span>{content.liked ? "Liked" : "Like"}</span>
          <span>{formatCount(content.likeCount)}</span>
        </button>
        <button
          type="button"
          className={content.saved ? "social-button is-active" : "social-button"}
          disabled={disabled}
          aria-label={content.saved ? "Saved video" : "Save video"}
          aria-pressed={content.saved}
          onClick={() => {
            if (requireReady("save videos")) {
              save.mutate(!content.saved);
            }
          }}
        >
          <span>{content.saved ? "Saved" : "Save"}</span>
        </button>
        {isOwnCreator ? (
          <span className="social-button social-button--static">Your profile</span>
        ) : (
          <button
            type="button"
            className={content.following ? "social-button is-active" : "social-button"}
            disabled={disabled}
            aria-label={content.following ? "Following creator" : "Follow creator"}
            aria-pressed={content.following}
            onClick={() => {
              if (requireReady("follow creators")) {
                follow.mutate(!content.following);
              }
            }}
          >
            <span>{content.following ? "Following" : "Follow"}</span>
          </button>
        )}
      </div>
      {authAction ? <SocialAuthPrompt action={authAction} /> : null}
      {failure ? (
        <p className="social-feedback" role="alert">
          {failure.message}
        </p>
      ) : null}
    </div>
  );
}

function formatCount(value: number): string {
  return new Intl.NumberFormat(undefined, { notation: "compact" }).format(value);
}
