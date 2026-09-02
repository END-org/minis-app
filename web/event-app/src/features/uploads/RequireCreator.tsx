import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { useBackendSession } from "../account/BackendSessionContext";
import type { ForbiddenRouteState } from "../authentication/forbiddenRouteState";

interface RequireCreatorProps {
  readonly children: ReactNode;
}

export function RequireCreator({ children }: RequireCreatorProps) {
  const { retryProvisioning, state } = useBackendSession();

  if (state.status === "firebase-loading" || state.status === "provisioning") {
    return <LoadingState message="Checking creator access" />;
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

  if (state.status !== "ready" || state.account?.role !== "CREATOR") {
    const routeState: ForbiddenRouteState = {
      failure: {
        code: "ACCESS_DENIED",
        message: "Only active creator accounts can upload event videos.",
        status: 403,
      },
    };
    return <Navigate to="/forbidden" replace state={routeState} />;
  }

  return children;
}
