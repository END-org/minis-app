import { useMemo, useState, type SyntheticEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ApiClientError } from "../../core/api/apiClient";
import { useAuthSession } from "../../core/auth/AuthSessionContext";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { useBootstrapAccountMutation } from "../account/accountQueries";
import type { ForbiddenRouteState } from "./forbiddenRouteState";

interface SignInValidation {
  readonly email?: string | undefined;
  readonly password?: string | undefined;
}

export function SignInPage() {
  const auth = useAuthSession();
  const bootstrap = useBootstrapAccountMutation();
  const location = useLocation();
  const navigate = useNavigate();
  const nextDestination = useMemo(() => intendedDestination(location.search), [location.search]);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [validation, setValidation] = useState<SignInValidation>({});
  const [failure, setFailure] = useState<string | null>(null);

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
    const nextValidation = validateSignIn(email, password);
    setValidation(nextValidation);
    if (nextValidation.email || nextValidation.password) {
      return;
    }

    try {
      await auth.signInWithEmail(email.trim(), password);
      await auth.reloadCurrentUser();
      await bootstrap.mutateAsync();
      void navigate(nextDestination, { replace: true });
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 403) {
        const state: ForbiddenRouteState = { failure: toApiFailureView(error) };
        void navigate("/forbidden", { replace: true, state });
        return;
      }
      setFailure(error instanceof Error ? error.message : "Sign-in failed.");
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="sign-in-title">
      <p className="section-kicker">Prepared account</p>
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

        <button type="submit" disabled={bootstrap.isPending}>
          {bootstrap.isPending ? "Signing in" : "Sign in"}
        </button>
      </form>
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

function intendedDestination(search: string): string {
  const next = new URLSearchParams(search).get("next");
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.startsWith("/api/")) {
    return "/account";
  }
  return next;
}
