import { Navigate } from "react-router-dom";
import { useState, type SyntheticEvent } from "react";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import type { ForbiddenRouteState } from "../authentication/forbiddenRouteState";
import { useBackendSession } from "./BackendSessionContext";
import { useUpdateCurrentProfileMutation } from "./accountQueries";

interface ProfileFormState {
  readonly displayName: string;
  readonly avatarUrl: string;
  readonly bio: string;
}

interface ProfileValidation {
  readonly displayName?: string;
  readonly avatarUrl?: string;
  readonly bio?: string;
}

export function AccountPage() {
  const backendSession = useBackendSession();
  const updateProfile = useUpdateCurrentProfileMutation(backendSession.state.firebaseUid);
  const [draft, setDraft] = useState<Partial<ProfileFormState>>({});
  const [draftAccountId, setDraftAccountId] = useState<string | null>(null);
  const [validation, setValidation] = useState<ProfileValidation>({});
  const [saved, setSaved] = useState(false);

  if (
    backendSession.state.status === "firebase-loading" ||
    backendSession.state.status === "provisioning"
  ) {
    return <LoadingState message="Loading MINIS account" />;
  }

  if (backendSession.state.status === "blocked") {
    const routeState: ForbiddenRouteState = {
      failure: backendSession.state.failure ?? {
        code: "ACCOUNT_BLOCKED",
        message: "The backend rejected this local account.",
        status: 403,
      },
    };
    return <Navigate to="/forbidden" replace state={routeState} />;
  }

  if (backendSession.state.status === "failed") {
    return (
      <ApiFailureNotice
        failure={
          backendSession.state.failure ?? {
            code: "BACKEND_SESSION_FAILED",
            message: "The MINIS backend account session could not be initialized.",
          }
        }
        onRetry={backendSession.retryProvisioning}
      />
    );
  }

  if (backendSession.state.status !== "ready") {
    return (
      <section className="notice">
        <h1>Account unavailable</h1>
        <p>The backend did not return an account for this session.</p>
        <button type="button" onClick={backendSession.retryProvisioning}>
          Retry
        </button>
      </section>
    );
  }

  const account = backendSession.state.account;
  if (!account) {
    return (
      <section className="notice">
        <h1>Account unavailable</h1>
        <p>The backend did not return an account for this session.</p>
        <button type="button" onClick={backendSession.retryProvisioning}>
          Retry
        </button>
      </section>
    );
  }
  const serverForm = formFromAccount(account);
  const form =
    draftAccountId === account.id ? { ...serverForm, ...draft } : serverForm;

  async function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaved(false);
    const nextValidation = validateProfile(form);
    setValidation(nextValidation);
    if (nextValidation.displayName || nextValidation.avatarUrl || nextValidation.bio) {
      return;
    }
    try {
      const updatedAccount = await updateProfile.mutateAsync({
        avatarUrl: form.avatarUrl.trim(),
        bio: form.bio.trim(),
        displayName: form.displayName.trim(),
      });
      backendSession.replaceAccount(updatedAccount);
      setDraft({});
      setDraftAccountId(null);
      setSaved(true);
    } catch {
      setSaved(false);
    }
  }

  const updateFailure = updateProfile.error ? toApiFailureView(updateProfile.error) : null;

  return (
    <section className="account-view" aria-labelledby="account-title">
      <p className="section-kicker">Backend account</p>
      <h1 id="account-title">My Account</h1>
      <dl className="fact-list">
        <div>
          <dt>Email</dt>
          <dd>{account.email ?? "Not provided"}</dd>
        </div>
        <div>
          <dt>Role</dt>
          <dd>{account.role}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>{account.status}</dd>
        </div>
        <div>
          <dt>Email verified</dt>
          <dd>{account.emailVerified ? "Yes" : "No"}</dd>
        </div>
      </dl>
      <form
        className="profile-form stack"
        onSubmit={(event) => {
          void handleSubmit(event);
        }}
        noValidate
      >
        <h2>Edit profile</h2>
        <label>
          Display name
          <input
            value={form.displayName}
            maxLength={120}
            onChange={(event) => {
              const value = event.currentTarget.value;
              setDraftAccountId(account.id);
              setDraft((current) => ({ ...current, displayName: value }));
            }}
            aria-invalid={Boolean(validation.displayName)}
            aria-describedby={validation.displayName ? "display-name-error" : undefined}
          />
        </label>
        {validation.displayName ? (
          <p className="field-error" id="display-name-error">
            {validation.displayName}
          </p>
        ) : null}
        <label>
          Avatar URL
          <input
            value={form.avatarUrl}
            maxLength={1024}
            onChange={(event) => {
              const value = event.currentTarget.value;
              setDraftAccountId(account.id);
              setDraft((current) => ({ ...current, avatarUrl: value }));
            }}
            aria-invalid={Boolean(validation.avatarUrl)}
            aria-describedby={validation.avatarUrl ? "avatar-url-error" : undefined}
          />
        </label>
        {validation.avatarUrl ? (
          <p className="field-error" id="avatar-url-error">
            {validation.avatarUrl}
          </p>
        ) : null}
        <label>
          Bio
          <textarea
            value={form.bio}
            maxLength={500}
            onChange={(event) => {
              const value = event.currentTarget.value;
              setDraftAccountId(account.id);
              setDraft((current) => ({ ...current, bio: value }));
            }}
            aria-invalid={Boolean(validation.bio)}
            aria-describedby={validation.bio ? "bio-error" : undefined}
          />
        </label>
        {validation.bio ? (
          <p className="field-error" id="bio-error">
            {validation.bio}
          </p>
        ) : null}
        {updateFailure ? <ApiFailureNotice failure={updateFailure} /> : null}
        {saved ? (
          <p className="success-message" role="status">
            Profile saved.
          </p>
        ) : null}
        <button type="submit" disabled={updateProfile.isPending}>
          {updateProfile.isPending ? "Saving" : "Save profile"}
        </button>
      </form>
    </section>
  );
}

function formFromAccount(account: {
  readonly avatarUrl?: string;
  readonly bio?: string;
  readonly displayName?: string;
}): ProfileFormState {
  return {
    avatarUrl: account.avatarUrl ?? "",
    bio: account.bio ?? "",
    displayName: account.displayName ?? "",
  };
}

function validateProfile(form: ProfileFormState): ProfileValidation {
  const trimmedAvatarUrl = form.avatarUrl.trim();
  return {
    ...(trimmedAvatarUrl && !/^https?:\/\/.+/i.test(trimmedAvatarUrl)
      ? { avatarUrl: "Avatar URL must begin with http:// or https://." }
      : {}),
    ...(form.bio.length > 500 ? { bio: "Bio must be 500 characters or fewer." } : {}),
    ...(form.displayName.length > 120
      ? { displayName: "Display name must be 120 characters or fewer." }
      : {}),
  };
}
