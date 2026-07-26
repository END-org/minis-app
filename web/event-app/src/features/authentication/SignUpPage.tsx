import { useMemo, useState, type SyntheticEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuthSession } from "../../core/auth/AuthSessionContext";
import { toFirebaseAuthMessage } from "../../core/auth/firebaseError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { destinationSearch, intendedDestination } from "./intendedDestination";

interface SignUpValidation {
  readonly displayName?: string | undefined;
  readonly email?: string | undefined;
  readonly password?: string | undefined;
  readonly passwordConfirmation?: string | undefined;
}

export interface VerificationRouteState {
  readonly displayNameUpdated?: boolean;
  readonly verificationEmailSent?: boolean;
}

const DISPLAY_NAME_MAX = 120;
const PASSWORD_MIN = 6;

export function SignUpPage() {
  const auth = useAuthSession();
  const location = useLocation();
  const navigate = useNavigate();
  const nextDestination = useMemo(() => intendedDestination(location.search), [location.search]);
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [validation, setValidation] = useState<SignUpValidation>({});
  const [failure, setFailure] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (auth.loading) {
    return <LoadingState message="Checking sign-up state" />;
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
    const normalizedEmail = email.trim().toLowerCase();
    const trimmedName = displayName.trim();
    const nextValidation = validateSignUp(
      trimmedName,
      normalizedEmail,
      password,
      passwordConfirmation,
    );
    setValidation(nextValidation);
    if (
      nextValidation.displayName ||
      nextValidation.email ||
      nextValidation.password ||
      nextValidation.passwordConfirmation
    ) {
      return;
    }

    setSubmitting(true);
    try {
      const result = await auth.registerWithEmail({
        displayName: trimmedName,
        email: normalizedEmail,
        password,
      });
      const state: VerificationRouteState = result;
      void navigate(`/verify-email?${destinationSearch(nextDestination)}`, {
        replace: true,
        state,
      });
    } catch (error) {
      setFailure(toFirebaseAuthMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="sign-up-title">
      <p className="section-kicker">Create account</p>
      <h1 id="sign-up-title">Sign up</h1>
      <form
        className="stack"
        onSubmit={(event) => {
          void handleSubmit(event);
        }}
        noValidate
      >
        <label>
          Display name
          <input
            value={displayName}
            onChange={(event) => setDisplayName(event.currentTarget.value)}
            autoComplete="name"
            maxLength={DISPLAY_NAME_MAX}
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
            autoComplete="new-password"
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

        <label>
          Confirm password
          <input
            value={passwordConfirmation}
            onChange={(event) => setPasswordConfirmation(event.currentTarget.value)}
            autoComplete="new-password"
            type="password"
            aria-invalid={Boolean(validation.passwordConfirmation)}
            aria-describedby={
              validation.passwordConfirmation ? "password-confirmation-error" : undefined
            }
          />
        </label>
        {validation.passwordConfirmation ? (
          <p className="field-error" id="password-confirmation-error">
            {validation.passwordConfirmation}
          </p>
        ) : null}

        {failure ? (
          <p className="form-error" role="alert">
            {failure}
          </p>
        ) : null}

        <button type="submit" disabled={submitting}>
          {submitting ? "Creating account" : "Create account"}
        </button>
      </form>
      <p className="muted-link auth-links">
        <Link to={`/sign-in?${destinationSearch(nextDestination)}`}>Sign in</Link>
        <Link to="/">Continue as guest</Link>
      </p>
    </section>
  );
}

function validateSignUp(
  displayName: string,
  email: string,
  password: string,
  passwordConfirmation: string,
): SignUpValidation {
  return {
    displayName: validateDisplayName(displayName),
    email: validateEmail(email),
    password: validatePassword(password),
    passwordConfirmation:
      password === passwordConfirmation ? undefined : "Passwords must match.",
  };
}

function validateDisplayName(displayName: string): string | undefined {
  if (!displayName) {
    return "Display name is required.";
  }
  if (displayName.length > DISPLAY_NAME_MAX) {
    return `Display name must be ${String(DISPLAY_NAME_MAX)} characters or fewer.`;
  }
  return undefined;
}

function validateEmail(email: string): string | undefined {
  if (!email) {
    return "Email is required.";
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/u.test(email)) {
    return "Enter a valid email address.";
  }
  return undefined;
}

function validatePassword(password: string): string | undefined {
  if (!password) {
    return "Password is required.";
  }
  if (password.length < PASSWORD_MIN) {
    return `Password must be at least ${String(PASSWORD_MIN)} characters.`;
  }
  return undefined;
}
