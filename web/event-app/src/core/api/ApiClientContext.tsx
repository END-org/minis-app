import { createContext, useContext, useMemo, type ReactNode } from "react";
import { useAuthSession } from "../auth/AuthSessionContext";
import { eventAppConfig } from "../config/environment";
import { createApiClient, type ApiClient } from "./apiClient";

export const ApiClientContext = createContext<ApiClient | null>(null);

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

export function useApiClient(): ApiClient {
  const client = useContext(ApiClientContext);
  if (!client) {
    throw new Error("useApiClient must be used inside ApiClientProvider.");
  }
  return client;
}
