import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { ApiClientProvider } from "../core/api/ApiClientContext";
import { AuthSessionProvider } from "../core/auth/AuthSessionContext";

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
        <ApiClientProvider>{children}</ApiClientProvider>
      </AuthSessionProvider>
    </QueryClientProvider>
  );
}
