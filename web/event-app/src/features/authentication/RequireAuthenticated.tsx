import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { useAuthSession } from "../../core/auth/AuthSessionContext";
import { LoadingState } from "../../shared/components/LoadingState";

interface RequireAuthenticatedProps {
  readonly children: ReactNode;
}

export function RequireAuthenticated({ children }: RequireAuthenticatedProps) {
  const { loading, user } = useAuthSession();
  const location = useLocation();

  if (loading) {
    return <LoadingState message="Checking account access" />;
  }

  if (!user) {
    const next = encodeURIComponent(`${location.pathname}${location.search}`);
    return <Navigate to={`/sign-in?next=${next}`} replace />;
  }

  return children;
}
