import { useMemo, useState, type SyntheticEvent } from "react";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import type { AdminUserResponse } from "./adminApi";
import {
  creatorRoleFormFromUser,
  type AdminCreatorRoleFormValue,
} from "./creatorRoleFormState";
import { AdminCreatorRoleForm } from "./AdminCreatorRoleForm";
import {
  useGrantCreatorRoleMutation,
  useReactivateUserMutation,
  useRevokeCreatorRoleMutation,
  useSuspendUserMutation,
  useUnverifyCreatorMutation,
  useVerifyCreatorMutation,
} from "./adminQueries";

interface AdminUserActionsProps {
  readonly user: AdminUserResponse;
}

export function AdminUserActions({ user }: AdminUserActionsProps) {
  const grantCreator = useGrantCreatorRoleMutation(user.id);
  const revokeCreator = useRevokeCreatorRoleMutation(user.id);
  const suspendUser = useSuspendUserMutation(user.id);
  const reactivateUser = useReactivateUserMutation(user.id);
  const verifyCreator = useVerifyCreatorMutation(user.id);
  const unverifyCreator = useUnverifyCreatorMutation(user.id);
  const [draft, setDraft] = useState<Partial<AdminCreatorRoleFormValue>>({});
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

  const serverForm = creatorRoleFormFromUser(user);
  const form = draftUserId === user.id ? { ...serverForm, ...draft } : serverForm;
  const pending =
    grantCreator.isPending ||
    revokeCreator.isPending ||
    suspendUser.isPending ||
    reactivateUser.isPending ||
    verifyCreator.isPending ||
    unverifyCreator.isPending;

  async function handleGrant(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
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

  return (
    <>
      <AdminCreatorRoleForm
        pending={pending}
        value={form}
        onChange={(field, value) => {
          setDraftUserId(user.id);
          setDraft((current) => ({ ...current, [field]: value }));
        }}
        onSubmit={(event) => {
          void handleGrant(event);
        }}
      />
      <div className="admin-action-grid">
        <button
          type="button"
          disabled={pending || user.role !== "CREATOR"}
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
          disabled={pending || !user.creatorProfile}
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
          disabled={pending || !user.creatorProfile?.verified}
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
          disabled={pending || user.status !== "ACTIVE"}
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
          disabled={pending || user.status !== "SUSPENDED"}
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
    </>
  );
}
