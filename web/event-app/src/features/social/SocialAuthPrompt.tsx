import { Link, useLocation } from "react-router-dom";
import { destinationSearch } from "../authentication/intendedDestination";
import { useBackendSession } from "../account/BackendSessionContext";

interface SocialAuthPromptProps {
  readonly action: string;
}

export function SocialAuthPrompt({ action }: SocialAuthPromptProps) {
  const location = useLocation();
  const { state } = useBackendSession();
  const next = `${location.pathname}${location.search}`;

  if (state.status === "signed-out") {
    return (
      <p className="social-feedback">
        <Link to={`/sign-in?${destinationSearch(next)}`}>Sign in</Link> to {action}.
      </p>
    );
  }

  if (state.status === "verification-required") {
    return <p className="social-feedback">Verify your email before using social actions.</p>;
  }

  if (state.status === "blocked") {
    return <p className="social-feedback">This account cannot use social actions right now.</p>;
  }

  return <p className="social-feedback">Finish account setup before using social actions.</p>;
}
