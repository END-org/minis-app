import { useEffect, useMemo, useState, type SyntheticEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ApiClientError } from "../../core/api/apiClient";
import { isEmailPasswordUser, useAuthSession } from "../../core/auth/AuthSessionContext";
import { toFirebaseAuthMessage } from "../../core/auth/firebaseError";
import { useBackendSession } from "../account/BackendSessionContext";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { destinationSearch, intendedDestination } from "./intendedDestination";
import type { ForbiddenRouteState } from "./forbiddenRouteState";

interface SignInValidation {
  readonly email?: string | undefined;
  readonly password?: string | undefined;
}

export function SignInPage() {
  const auth = useAuthSession();
  const backendSession = useBackendSession();
  const location = useLocation();
  const navigate = useNavigate();
  const nextDestination = useMemo(() => intendedDestination(location.search), [location.search]);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [validation, setValidation] = useState<SignInValidation>({});
  const [failure, setFailure] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [pendingDestination, setPendingDestination] = useState<string | null>(null);

  useEffect(() => {
    if (!pendingDestination) {
      return;
    }

    if (backendSession.state.status === "ready") {
      queueMicrotask(() => {
        setPendingDestination(null);
        setSubmitting(false);
        void navigate(pendingDestination, { replace: true });
      });
      return;
    }

    if (backendSession.state.status === "verification-required") {
      queueMicrotask(() => {
        setPendingDestination(null);
        setSubmitting(false);
        void navigate(`/verify-email?${destinationSearch(pendingDestination)}`, { replace: true });
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
        setPendingDestination(null);
        setSubmitting(false);
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
        setPendingDestination(null);
        setSubmitting(false);
      });
    }
  }, [backendSession.state, navigate, pendingDestination]);

  if (auth.loading) {
    return <LoadingState message="Checking sign-in state" />;
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

  async function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    setFailure(null);
    auth.clearSessionNotice();
    const normalizedEmail = email.trim().toLowerCase();
    const nextValidation = validateSignIn(normalizedEmail, password);
    setValidation(nextValidation);
    if (nextValidation.email || nextValidation.password) {
      return;
    }

    setSubmitting(true);
    let waitingForBackend = false;
    try {
      await auth.signInWithEmail(normalizedEmail, password);
      const currentUser = await auth.reloadCurrentUser();
      if (isEmailPasswordUser(currentUser) && !currentUser.emailVerified) {
        void navigate(`/verify-email?${destinationSearch(nextDestination)}`, { replace: true });
        return;
      }
      waitingForBackend = true;
      setPendingDestination(nextDestination);
    } catch (error) {
      setFailure(toAuthenticationFailureMessage(error));
    } finally {
      if (!waitingForBackend) {
        setSubmitting(false);
      }
    }
  }

  async function handleGoogleSignIn() {
    setFailure(null);
    auth.clearSessionNotice();
    setSubmitting(true);
    let waitingForBackend = false;
    try {
      await auth.signInWithGoogle();
      await auth.reloadCurrentUser();
      waitingForBackend = true;
      setPendingDestination(nextDestination);
    } catch (error) {
      setFailure(toAuthenticationFailureMessage(error));
    } finally {
      if (!waitingForBackend) {
        setSubmitting(false);
      }
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="sign-in-title">
      <p className="section-kicker">Firebase account</p>
      <h1 id="sign-in-title">Sign in</h1>
      {auth.sessionNotice ? (
        <section className="notice" aria-live="polite">
          <h2>{auth.sessionNotice.title}</h2>
          <p>{auth.sessionNotice.message}</p>
        </section>
      ) : null}
      <form
        className="stack"
        onSubmit={(event) => {
          void handleSubmit(event);
        }}
        noValidate
      >
        <label>
          Email
          <input
            value={email}
            onChange={(event) => setEmail(event.currentTarget.value)}
            autoComplete="email"
            inputMode="email"
            type="email"
            aria-invalid={Boolean(validation.email)}
            aria-describedby={validation.email ? "email-error" : undefined}
          />
        </label>
        {validation.email ? (
          <p className="field-error" id="email-error">
            {validation.email}
          </p>
        ) : null}

        <label>
          Password
          <input
            value={password}
            onChange={(event) => setPassword(event.currentTarget.value)}
            autoComplete="current-password"
            type="password"
            aria-invalid={Boolean(validation.password)}
            aria-describedby={validation.password ? "password-error" : undefined}
          />
        </label>
        {validation.password ? (
          <p className="field-error" id="password-error">
            {validation.password}
          </p>
        ) : null}

        {failure ? (
          <p className="form-error" role="alert">
            {failure}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={submitting || backendSession.state.status === "provisioning"}
        >
          {submitting || backendSession.state.status === "provisioning" ? "Signing in" : "Sign in"}
        </button>
      </form>
      <div className="auth-divider" role="presentation">
        <span>or</span>
      </div>
      <button
        type="button"
        className="secondary-button"
        onClick={() => {
          void handleGoogleSignIn();
        }}
        disabled={submitting || backendSession.state.status === "provisioning"}
      >
        Sign in with Google
      </button>
      <p className="muted-link auth-links">
        <Link to={`/sign-up?${destinationSearch(nextDestination)}`}>Create account</Link>
        <Link to="/forgot-password">Forgot password</Link>
      </p>
      <p className="muted-link">
        <Link to="/">Continue as guest</Link>
      </p>
    </section>
  );
}

function validateSignIn(email: string, password: string): SignInValidation {
  const trimmedEmail = email.trim();
  return {
    email: trimmedEmail ? undefined : "Email is required.",
    password: password ? undefined : "Password is required.",
  };
}

function toAuthenticationFailureMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    return error.message;
  }
  return toFirebaseAuthMessage(error);
}
