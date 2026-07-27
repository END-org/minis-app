import { useEffect, useMemo, useState } from "react";
import type { User } from "firebase/auth";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { ApiClientError } from "../../core/api/apiClient";
import { isEmailPasswordUser, useAuthSession } from "../../core/auth/AuthSessionContext";
import { toFirebaseAuthMessage } from "../../core/auth/firebaseError";
import { useBackendSession } from "../account/BackendSessionContext";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { destinationSearch, intendedDestination } from "./intendedDestination";
import type { VerificationRouteState } from "./SignUpPage";
import type { ForbiddenRouteState } from "./forbiddenRouteState";

const RESEND_COOLDOWN_SECONDS = 60;

export function VerifyEmailPage() {
  const auth = useAuthSession();
  const location = useLocation();
  const navigate = useNavigate();
  const backendSession = useBackendSession();
  const nextDestination = useMemo(() => intendedDestination(location.search), [location.search]);
  const routeState = isVerificationRouteState(location.state) ? location.state : null;
  const [failure, setFailure] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [resending, setResending] = useState(false);
  const [cooldownSeconds, setCooldownSeconds] = useState(0);
  const [verificationAccepted, setVerificationAccepted] = useState(false);

  useEffect(() => {
    if (cooldownSeconds <= 0) {
      return undefined;
    }
    const timeout = window.setTimeout(() => {
      setCooldownSeconds((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearTimeout(timeout);
  }, [cooldownSeconds]);

  useEffect(() => {
    if (!verificationAccepted) {
      return;
    }

    if (backendSession.state.status === "ready") {
      queueMicrotask(() => {
        setVerificationAccepted(false);
        setChecking(false);
        void navigate(nextDestination, { replace: true });
      });
      return;
    }

    if (backendSession.state.status === "blocked") {
      const routeState: ForbiddenRouteState = {
        failure: backendSession.state.failure ?? {
          code: "ACCOUNT_BLOCKED",
          message: "The backend rejected this local account.",
          status: 403,
        },
      };
      queueMicrotask(() => {
        setVerificationAccepted(false);
        setChecking(false);
        void navigate("/forbidden", { replace: true, state: routeState });
      });
      return;
    }

    if (backendSession.state.status === "failed") {
      queueMicrotask(() => {
        setFailure(
          backendSession.state.failure?.message ??
            "The MINIS backend account session could not be initialized.",
        );
        setVerificationAccepted(false);
        setChecking(false);
      });
    }
  }, [backendSession.state, navigate, nextDestination, verificationAccepted]);

  if (auth.loading) {
    return <LoadingState message="Checking verification state" />;
  }

  if (auth.setupError) {
    return (
      <ApiFailureNotice
        failure={{
          code: "FIREBASE_CONFIG_ERROR",
          message: auth.setupError,
        }}
      />
    );
  }

  if (!auth.user) {
    return <Navigate to={`/sign-in?${destinationSearch(nextDestination)}`} replace />;
  }

  if (!isEmailPasswordUser(auth.user)) {
    return <Navigate to={`/sign-in?${destinationSearch(nextDestination)}`} replace />;
  }

  async function handleResend() {
    setFailure(null);
    setSuccess(null);
    setResending(true);
    try {
      await auth.sendCurrentUserVerificationEmail();
      setCooldownSeconds(RESEND_COOLDOWN_SECONDS);
      setSuccess("Verification email sent. Check your inbox before continuing.");
    } catch (error) {
      setFailure(toFirebaseAuthMessage(error));
    } finally {
      setResending(false);
    }
  }

  async function handleCheckVerification() {
    setFailure(null);
    setSuccess(null);
    setChecking(true);
    let waitingForBackend = false;
    try {
      const currentUser = await auth.reloadCurrentUser();
      if (!currentUser.emailVerified) {
        setFailure("Firebase still reports this email as unverified.");
        return;
      }
      await auth.refreshCurrentToken();
      waitingForBackend = true;
      setVerificationAccepted(true);
    } catch (error) {
      setFailure(toVerificationFailureMessage(error));
    } finally {
      if (!waitingForBackend) {
        setChecking(false);
      }
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="verify-email-title">
      <p className="section-kicker">Email verification</p>
      <h1 id="verify-email-title">Verify your email</h1>
      <p className="lead">
        Check the inbox for {displayEmail(auth.user)} and open the verification link before entering
        the MINIS account area.
      </p>
      {routeState?.displayNameUpdated === false ? (
        <p className="form-error" role="alert">
          The Firebase account was created, but the display name was not saved. You can update the
          MINIS profile after verification.
        </p>
      ) : null}
      {routeState?.verificationEmailSent === false ? (
        <p className="form-error" role="alert">
          The account was created, but the verification email was not sent. Use resend below.
        </p>
      ) : null}
      {failure ? (
        <p className="form-error" role="alert">
          {failure}
        </p>
      ) : null}
      {success ? (
        <p className="success-message" role="status">
          {success}
        </p>
      ) : null}
      <div className="action-row">
        <button
          type="button"
          onClick={() => {
            void handleCheckVerification();
          }}
          disabled={checking || backendSession.state.status === "provisioning"}
        >
          {checking || backendSession.state.status === "provisioning"
            ? "Checking"
            : "Check verification"}
        </button>
        <button
          type="button"
          className="secondary-button"
          onClick={() => {
            void handleResend();
          }}
          disabled={resending || cooldownSeconds > 0}
        >
          {resending ? "Sending" : resendLabel(cooldownSeconds)}
        </button>
      </div>
      <p className="muted-link auth-links">
        <button
          type="button"
          className="link-button"
          onClick={() => {
            void auth.signOut();
          }}
        >
          Sign out
        </button>
        <Link to="/">Continue as guest</Link>
      </p>
    </section>
  );
}

function displayEmail(user: User): string {
  return user.email ?? "your Firebase account";
}

function resendLabel(cooldownSeconds: number): string {
  if (cooldownSeconds > 0) {
    return `Resend in ${String(cooldownSeconds)}s`;
  }
  return "Resend verification email";
}

function toVerificationFailureMessage(error: unknown): string {
  if (error instanceof ApiClientError && error.code === "EMAIL_NOT_VERIFIED") {
    return "Firebase still reports this email as unverified.";
  }
  if (error instanceof ApiClientError) {
    return error.message;
  }
  return toFirebaseAuthMessage(error);
}

function isVerificationRouteState(value: unknown): value is VerificationRouteState {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const state = value as VerificationRouteState;
  return (
    (state.displayNameUpdated === undefined || typeof state.displayNameUpdated === "boolean") &&
    (state.verificationEmailSent === undefined || typeof state.verificationEmailSent === "boolean")
  );
}
