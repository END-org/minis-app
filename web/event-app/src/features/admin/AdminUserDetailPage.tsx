import { useMemo, useState, type SyntheticEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import {
  useAdminUserQuery,
  useGrantCreatorRoleMutation,
  useReactivateUserMutation,
  useRevokeCreatorRoleMutation,
  useSuspendUserMutation,
  useUnverifyCreatorMutation,
  useVerifyCreatorMutation,
} from "./adminQueries";
import type { AdminUserResponse } from "./adminApi";

interface GrantFormState {
  readonly slug: string;
  readonly displayName: string;
  readonly bio: string;
  readonly avatarUrl: string;
  readonly reason: string;
}

export function AdminUserDetailPage() {
  const { userId } = useParams();
  const user = useAdminUserQuery(userId);
  const grantCreator = useGrantCreatorRoleMutation(userId ?? "");
  const revokeCreator = useRevokeCreatorRoleMutation(userId ?? "");
  const suspendUser = useSuspendUserMutation(userId ?? "");
  const reactivateUser = useReactivateUserMutation(userId ?? "");
  const verifyCreator = useVerifyCreatorMutation(userId ?? "");
  const unverifyCreator = useUnverifyCreatorMutation(userId ?? "");
  const [draft, setDraft] = useState<Partial<GrantFormState>>({});
  const [draftUserId, setDraftUserId] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const failure = useMemo(
    () =>
      [
        grantCreator.error,
        revokeCreator.error,
        suspendUser.error,
        reactivateUser.error,
        verifyCreator.error,
        unverifyCreator.error,
      ].find(Boolean) ?? null,
    [
      grantCreator.error,
      reactivateUser.error,
      revokeCreator.error,
      suspendUser.error,
      unverifyCreator.error,
      verifyCreator.error,
    ],
  );

  if (user.isLoading) {
    return <LoadingState message="Loading account detail" />;
  }

  if (user.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(user.error)}
        onRetry={() => {
          void user.refetch();
        }}
      />
    );
  }

  if (!user.data || !userId) {
    return (
      <section className="notice">
        <h1>User unavailable</h1>
        <Link to="/admin">Back to admin</Link>
      </section>
    );
  }

  const serverForm = formFromAdminUser(user.data);
  const form = draftUserId === user.data.id ? { ...serverForm, ...draft } : serverForm;

  async function handleGrant(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!userId) {
      return;
    }
    setActionMessage(null);
    try {
      await grantCreator.mutateAsync({
        avatarUrl: form.avatarUrl.trim(),
        bio: form.bio.trim(),
        displayName: form.displayName.trim(),
        reason: form.reason.trim(),
        slug: form.slug.trim(),
      });
      setDraft({});
      setDraftUserId(null);
      setActionMessage("Creator role saved.");
    } catch {
      setActionMessage(null);
    }
  }

  async function runConfirmedAction(label: string, action: () => Promise<unknown>) {
    if (!window.confirm(label)) {
      return;
    }
    setActionMessage(null);
    try {
      await action();
      setActionMessage("Admin action saved.");
    } catch {
      setActionMessage(null);
    }
  }

  const pending =
    grantCreator.isPending ||
    revokeCreator.isPending ||
    suspendUser.isPending ||
    reactivateUser.isPending ||
    verifyCreator.isPending ||
    unverifyCreator.isPending;

  return (
    <section className="admin-view" aria-labelledby="admin-user-title">
      <p className="section-kicker">Event account</p>
      <h1 id="admin-user-title">{user.data.displayName ?? user.data.email ?? "Account"}</h1>
      <dl className="fact-list">
        <div>
          <dt>Email</dt>
          <dd>{user.data.email ?? "Not provided"}</dd>
        </div>
        <div>
          <dt>Role</dt>
          <dd>{user.data.role}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>{user.data.status}</dd>
        </div>
        <div>
          <dt>Creator slug</dt>
          <dd>{user.data.creatorProfile?.slug ?? "Not prepared"}</dd>
        </div>
        <div>
          <dt>Verified</dt>
          <dd>{user.data.creatorProfile?.verified ? "Yes" : "No"}</dd>
        </div>
      </dl>
      <form
        className="profile-form stack"
        onSubmit={(event) => {
          void handleGrant(event);
        }}
      >
        <h2>Creator role</h2>
        <label>
          Public slug
          <input
            value={form.slug}
            maxLength={120}
            onChange={(event) => {
              const value = event.currentTarget.value;
              setDraftUserId(user.data.id);
              setDraft((current) => ({ ...current, slug: value }));
            }}
            required
          />
        </label>
        <label>
          Public display name
          <input
            value={form.displayName}
            maxLength={120}
            onChange={(event) => {
              const value = event.currentTarget.value;
              setDraftUserId(user.data.id);
              setDraft((current) => ({ ...current, displayName: value }));
            }}
            required
          />
        </label>
        <label>
          Public bio
          <textarea
            value={form.bio}
            maxLength={500}
            onChange={(event) => {
              const value = event.currentTarget.value;
              setDraftUserId(user.data.id);
              setDraft((current) => ({ ...current, bio: value }));
            }}
          />
        </label>
        <label>
          Avatar URL
          <input
            value={form.avatarUrl}
            maxLength={1024}
            onChange={(event) => {
              const value = event.currentTarget.value;
              setDraftUserId(user.data.id);
              setDraft((current) => ({ ...current, avatarUrl: value }));
            }}
          />
        </label>
        <label>
          Audit reason
          <input
            value={form.reason}
            maxLength={300}
            onChange={(event) => {
              const value = event.currentTarget.value;
              setDraftUserId(user.data.id);
              setDraft((current) => ({ ...current, reason: value }));
            }}
          />
        </label>
        <button type="submit" disabled={pending}>
          Grant creator
        </button>
      </form>
      <div className="admin-action-grid">
        <button
          type="button"
          disabled={pending || user.data.role !== "CREATOR"}
          onClick={() => {
            void runConfirmedAction("Revoke creator capability?", () =>
              revokeCreator.mutateAsync({ reason: form.reason.trim() }),
            );
          }}
        >
          Revoke creator
        </button>
        <button
          type="button"
          disabled={pending || !user.data.creatorProfile}
          onClick={() => {
            void runConfirmedAction("Verify this creator?", () =>
              verifyCreator.mutateAsync({ reason: form.reason.trim() }),
            );
          }}
        >
          Verify creator
        </button>
        <button
          type="button"
          disabled={pending || !user.data.creatorProfile?.verified}
          onClick={() => {
            void runConfirmedAction("Remove creator verification?", () =>
              unverifyCreator.mutateAsync({ reason: form.reason.trim() }),
            );
          }}
        >
          Unverify creator
        </button>
        <button
          type="button"
          disabled={pending || user.data.status !== "ACTIVE"}
          onClick={() => {
            void runConfirmedAction("Suspend this account?", () =>
              suspendUser.mutateAsync({ reason: form.reason.trim() }),
            );
          }}
        >
          Suspend account
        </button>
        <button
          type="button"
          disabled={pending || user.data.status !== "SUSPENDED"}
          onClick={() => {
            void runConfirmedAction("Reactivate this account?", () =>
              reactivateUser.mutateAsync({ reason: form.reason.trim() }),
            );
          }}
        >
          Reactivate account
        </button>
      </div>
      {failure ? <ApiFailureNotice failure={toApiFailureView(failure)} /> : null}
      {actionMessage ? (
        <p className="success-message" role="status">
          {actionMessage}
        </p>
      ) : null}
      <p className="muted-link">
        <Link to="/admin">Back to admin</Link>
      </p>
    </section>
  );
}

function formFromAdminUser(user: AdminUserResponse): GrantFormState {
  return {
    avatarUrl: user.creatorProfile?.avatarUrl ?? "",
    bio: user.creatorProfile?.bio ?? "",
    displayName: user.creatorProfile?.displayName ?? user.displayName ?? user.email ?? "",
    reason: "",
    slug: user.creatorProfile?.slug ?? "",
  };
}
