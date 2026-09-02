import { createContext, useContext } from "react";
import type { ApiFailureView } from "../../core/errors/apiError";
import type { AccountResponse } from "./accountApi";

export type BackendSessionStatus =
  | "firebase-loading"
  | "signed-out"
  | "verification-required"
  | "provisioning"
  | "ready"
  | "blocked"
  | "failed";

export interface BackendSessionState {
  readonly status: BackendSessionStatus;
  readonly firebaseUid?: string;
  readonly account?: AccountResponse;
  readonly failure?: ApiFailureView;
}

export interface BackendSessionContextValue {
  readonly state: BackendSessionState;
  readonly retryProvisioning: () => void;
  readonly replaceAccount: (account: AccountResponse) => void;
}

export const BackendSessionContext = createContext<BackendSessionContextValue | null>(null);

export function useBackendSession(): BackendSessionContextValue {
  const value = useContext(BackendSessionContext);
  if (!value) {
    throw new Error("useBackendSession must be used inside BackendSessionProvider.");
  }
  return value;
}

export function backendSessionQueryKey(state: BackendSessionState): string {
  if (state.status === "ready" && state.account) {
    return `account:${state.account.id}`;
  }
  if (state.status === "signed-out") {
    return "guest";
  }
  if (state.firebaseUid) {
    return `${state.status}:${state.firebaseUid}`;
  }
  return state.status;
}
