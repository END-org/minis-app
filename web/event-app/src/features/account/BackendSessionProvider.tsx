import { useQueryClient, type QueryClient } from "@tanstack/react-query";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
  type SetStateAction,
} from "react";
import { ApiClientError, type ApiClient } from "../../core/api/apiClient";
import { useApiClient } from "../../core/api/ApiClientContext";
import { isEmailPasswordUser, useAuthSession } from "../../core/auth/AuthSessionContext";
import { toApiFailureView } from "../../core/errors/apiError";
import {
  accountQueryKey,
  bootstrapCurrentAccount,
  type AccountResponse,
} from "./accountApi";
import {
  BackendSessionContext,
  type BackendSessionContextValue,
  type BackendSessionState,
} from "./BackendSessionContext";

interface BackendSessionProviderProps {
  readonly children: ReactNode;
}

interface BootstrapRequest {
  readonly firebaseUid: string;
  readonly promise: Promise<AccountResponse>;
}

export function BackendSessionProvider({ children }: BackendSessionProviderProps) {
  const auth = useAuthSession();
  const client = useApiClient();
  const queryClient = useQueryClient();
  const [state, setState] = useState<BackendSessionState>({ status: "firebase-loading" });
  const [retryCount, setRetryCount] = useState(0);
  const lastFirebaseUid = useRef<string | null>(null);
  const sequence = useRef(0);
  const request = useRef<BootstrapRequest | null>(null);
  const stateRef = useRef(state);

  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  useEffect(() => {
    sequence.current += 1;
    const currentSequence = sequence.current;
    let active = true;

    function setActiveState(update: SetStateAction<BackendSessionState>) {
      queueMicrotask(() => {
        if (active) {
          setState(update);
        }
      });
    }

    if (auth.loading) {
      setActiveState({ status: "firebase-loading" });
      return () => {
        active = false;
      };
    }

    const firebaseUser = auth.currentUser() ?? auth.user;
    if (!firebaseUser) {
      request.current = null;
      lastFirebaseUid.current = null;
      removeSessionScopedQueries(queryClient);
      setActiveState({ status: "signed-out" });
      return () => {
        active = false;
      };
    }

    const firebaseUid = firebaseUser.uid;
    if (lastFirebaseUid.current !== firebaseUid) {
      request.current = null;
      lastFirebaseUid.current = firebaseUid;
      removeSessionScopedQueries(queryClient);
    }

    if (isEmailPasswordUser(firebaseUser) && !firebaseUser.emailVerified) {
      request.current = null;
      setActiveState({ firebaseUid, status: "verification-required" });
      return () => {
        active = false;
      };
    }

    const currentState = stateRef.current;
    if (
      currentState.status === "ready" &&
      currentState.firebaseUid === firebaseUid &&
      currentState.account
    ) {
      return () => {
        active = false;
      };
    }

    setActiveState((current) => {
      if (
        current.status === "ready" &&
        current.firebaseUid === firebaseUid &&
        current.account
      ) {
        return current;
      }
      return { firebaseUid, status: "provisioning" };
    });

    const activeRequest = bootstrapRequest(firebaseUid, request.current, client);
    request.current = activeRequest;

    activeRequest.promise
      .then((account) => {
        if (!active || sequence.current !== currentSequence) {
          return;
        }
        queryClient.setQueryData(accountQueryKey(firebaseUid), account);
        setState({ account, firebaseUid, status: "ready" });
      })
      .catch((error: unknown) => {
        if (!active || sequence.current !== currentSequence) {
          return;
        }
        setState(stateFromProvisioningFailure(firebaseUid, error));
      })
      .finally(() => {
        if (request.current?.promise === activeRequest.promise) {
          request.current = null;
        }
      });

    return () => {
      active = false;
    };
  }, [
    auth,
    auth.loading,
    auth.sessionVersion,
    auth.user,
    client,
    queryClient,
    retryCount,
  ]);

  const retryProvisioning = useCallback(() => {
    request.current = null;
    setRetryCount((current) => current + 1);
  }, []);

  const replaceAccount = useCallback(
    (account: AccountResponse) => {
      const firebaseUid = auth.currentUser()?.uid ?? stateRef.current.firebaseUid;
      if (!firebaseUid) {
        return;
      }
      queryClient.setQueryData(accountQueryKey(firebaseUid), account);
      setState({ account, firebaseUid, status: "ready" });
    },
    [auth, queryClient],
  );

  const value = useMemo<BackendSessionContextValue>(
    () => ({ replaceAccount, retryProvisioning, state }),
    [replaceAccount, retryProvisioning, state],
  );

  return (
    <BackendSessionContext.Provider value={value}>{children}</BackendSessionContext.Provider>
  );
}

function bootstrapRequest(
  firebaseUid: string,
  currentRequest: BootstrapRequest | null,
  client: ApiClient,
): BootstrapRequest {
  if (currentRequest?.firebaseUid === firebaseUid) {
    return currentRequest;
  }
  return {
    firebaseUid,
    promise: bootstrapCurrentAccount(client),
  };
}

function stateFromProvisioningFailure(
  firebaseUid: string,
  error: unknown,
): BackendSessionState {
  if (error instanceof ApiClientError) {
    if (error.code === "EMAIL_NOT_VERIFIED") {
      return { firebaseUid, status: "verification-required" };
    }
    if (error.code === "ACCOUNT_SUSPENDED" || error.code === "ACCOUNT_DISABLED") {
      return {
        failure: toApiFailureView(error),
        firebaseUid,
        status: "blocked",
      };
    }
  }

  return {
    failure: toApiFailureView(error),
    firebaseUid,
    status: "failed",
  };
}

function removeSessionScopedQueries(queryClient: QueryClient): void {
  void queryClient.cancelQueries({ predicate: isSessionScopedQuery });
  queryClient.removeQueries({ predicate: isSessionScopedQuery });
}

function isSessionScopedQuery(query: { readonly queryKey: readonly unknown[] }): boolean {
  const firstKey = query.queryKey[0];
  return (
    firstKey === "account" ||
    firstKey === "admin" ||
    firstKey === "feed" ||
    firstKey === "creator-content" ||
    firstKey === "creators" ||
    firstKey === "content-collection" ||
    firstKey === "uploads"
  );
}
