import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { useBackendSession } from "../account/BackendSessionContext";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { destinationSearch } from "./intendedDestination";
import type { ForbiddenRouteState } from "./forbiddenRouteState";

interface RequireAuthenticatedProps {
  readonly children: ReactNode;
}

export function RequireAuthenticated({ children }: RequireAuthenticatedProps) {
  const { retryProvisioning, state } = useBackendSession();
  const location = useLocation();

  if (state.status === "firebase-loading" || state.status === "provisioning") {
    return <LoadingState message="Checking account access" />;
  }

  if (state.status === "signed-out") {
    const next = `${location.pathname}${location.search}`;
    return <Navigate to={`/sign-in?${destinationSearch(next)}`} replace />;
  }

  if (state.status === "verification-required" && location.pathname !== "/verify-email") {
    const next = `${location.pathname}${location.search}`;
    return <Navigate to={`/verify-email?${destinationSearch(next)}`} replace />;
  }

  if (state.status === "blocked") {
    const routeState: ForbiddenRouteState = {
      failure: state.failure ?? {
        code: "ACCOUNT_BLOCKED",
        message: "The backend rejected this local account.",
        status: 403,
      },
    };
    return <Navigate to="/forbidden" replace state={routeState} />;
  }

  if (state.status === "failed") {
    return (
      <ApiFailureNotice
        failure={
          state.failure ?? {
            code: "BACKEND_SESSION_FAILED",
            message: "The MINIS backend account session could not be initialized.",
          }
        }
        onRetry={retryProvisioning}
      />
    );
  }

  return children;
}
