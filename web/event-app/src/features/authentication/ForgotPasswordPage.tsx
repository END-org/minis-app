import { useState, type SyntheticEvent } from "react";
import { Link } from "react-router-dom";
import { useAuthSession } from "../../core/auth/AuthSessionContext";
import { toFirebaseAuthMessage } from "../../core/auth/firebaseError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";

interface ForgotPasswordValidation {
  readonly email?: string | undefined;
}

export function ForgotPasswordPage() {
  const auth = useAuthSession();
  const [email, setEmail] = useState("");
  const [validation, setValidation] = useState<ForgotPasswordValidation>({});
  const [failure, setFailure] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  if (auth.loading) {
    return <LoadingState message="Preparing password reset" />;
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
    setSuccess(false);
    const normalizedEmail = email.trim().toLowerCase();
    const nextValidation = validateForgotPassword(normalizedEmail);
    setValidation(nextValidation);
    if (nextValidation.email) {
      return;
    }

    setSubmitting(true);
    try {
      await auth.sendPasswordResetEmail(normalizedEmail);
      setSuccess(true);
    } catch (error) {
      setFailure(toFirebaseAuthMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-panel" aria-labelledby="forgot-password-title">
      <p className="section-kicker">Password reset</p>
      <h1 id="forgot-password-title">Reset password</h1>
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
            aria-describedby={validation.email ? "reset-email-error" : undefined}
          />
        </label>
        {validation.email ? (
          <p className="field-error" id="reset-email-error">
            {validation.email}
          </p>
        ) : null}
        {failure ? (
          <p className="form-error" role="alert">
            {failure}
          </p>
        ) : null}
        {success ? (
          <p className="success-message" role="status">
            If that email can receive MINIS password reset mail, Firebase will send instructions.
          </p>
        ) : null}
        <button type="submit" disabled={submitting}>
          {submitting ? "Sending reset link" : "Send reset link"}
        </button>
      </form>
      <p className="muted-link">
        <Link to="/sign-in">Return to sign in</Link>
      </p>
    </section>
  );
}

function validateForgotPassword(email: string): ForgotPasswordValidation {
  if (!email) {
    return { email: "Email is required." };
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/u.test(email)) {
    return { email: "Enter a valid email address." };
  }
  return {};
}
