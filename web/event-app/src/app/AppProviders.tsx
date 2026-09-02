import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { ApiClientProvider } from "../core/api/ApiClientProvider";
import { AuthSessionProvider } from "../core/auth/AuthSessionProvider";
import { BackendSessionProvider } from "../features/account/BackendSessionProvider";

interface AppProvidersProps {
  readonly children: ReactNode;
}

export function AppProviders({ children }: AppProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          mutations: {
            retry: false,
          },
          queries: {
            refetchOnWindowFocus: false,
            retry: false,
            staleTime: 30_000,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      <AuthSessionProvider>
        <ApiClientProvider>
          <BackendSessionProvider>{children}</BackendSessionProvider>
        </ApiClientProvider>
      </AuthSessionProvider>
    </QueryClientProvider>
  );
}
