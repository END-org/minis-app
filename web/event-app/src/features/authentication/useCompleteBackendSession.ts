import { useBackendSession } from "../account/BackendSessionContext";

export function useCompleteBackendSession() {
  const backendSession = useBackendSession();

  function complete(destination: string): Promise<void> {
    void destination;
    backendSession.retryProvisioning();
    return Promise.resolve();
  }

  return {
    complete,
    isPending: backendSession.state.status === "provisioning",
  };
}
