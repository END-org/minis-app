import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import { useBackendSession } from "../account/BackendSessionContext";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import type { ForbiddenRouteState } from "../authentication/forbiddenRouteState";

interface RequireAdminProps {
  readonly children: ReactNode;
}

export function RequireAdmin({ children }: RequireAdminProps) {
  const { retryProvisioning, state } = useBackendSession();

  if (state.status === "firebase-loading" || state.status === "provisioning") {
    return <LoadingState message="Checking admin access" />;
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

  if (state.status !== "ready" || state.account?.role !== "ADMIN") {
    const routeState: ForbiddenRouteState = {
      failure: {
        code: "ACCESS_DENIED",
        message: "Only event admins can prepare accounts.",
        status: 403,
      },
    };
    return <Navigate to="/forbidden" replace state={routeState} />;
  }

  return children;
}
