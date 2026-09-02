import { useMemo, type ReactNode } from "react";
import { useAuthSession } from "../auth/AuthSessionContext";
import { eventAppConfig } from "../config/environment";
import { createApiClient } from "./apiClient";
import { ApiClientContext } from "./ApiClientContext";

interface ApiClientProviderProps {
  readonly children: ReactNode;
}

export function ApiClientProvider({ children }: ApiClientProviderProps) {
  const { currentUser, expireAuthenticatedSession } = useAuthSession();
  const client = useMemo(
    () =>
      createApiClient(eventAppConfig.apiBaseUrl, {
        currentUser,
        expireAuthenticatedSession,
      }),
    [currentUser, expireAuthenticatedSession],
  );

  return <ApiClientContext.Provider value={client}>{children}</ApiClientContext.Provider>;
}
