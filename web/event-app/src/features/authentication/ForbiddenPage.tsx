import { Link, useLocation } from "react-router-dom";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import type { ForbiddenRouteState } from "./forbiddenRouteState";

export function ForbiddenPage() {
  const location = useLocation();
  const state = isForbiddenRouteState(location.state) ? location.state : null;

  return (
    <section className="auth-panel" aria-labelledby="forbidden-title">
      <p className="section-kicker">Forbidden</p>
      <h1 id="forbidden-title">Access is blocked</h1>
      <ApiFailureNotice
        failure={
          state?.failure ?? {
            code: "ACCESS_DENIED",
            message: "The backend rejected this local account for the requested operation.",
            status: 403,
          }
        }
      />
      <p className="muted-link">
        <Link to="/">Return to guest mode</Link>
      </p>
    </section>
  );
}

function isForbiddenRouteState(value: unknown): value is ForbiddenRouteState {
  if (typeof value !== "object" || value === null || !("failure" in value)) {
    return false;
  }
  const failure = (value as ForbiddenRouteState).failure;
  return typeof failure.code === "string" && typeof failure.message === "string";
}
