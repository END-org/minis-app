import { Navigate } from "react-router-dom";
import { ApiClientError } from "../../core/api/apiClient";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import type { ForbiddenRouteState } from "../authentication/forbiddenRouteState";
import { useCurrentAccountQuery } from "./accountQueries";

export function AccountPage() {
  const account = useCurrentAccountQuery();

  if (account.isLoading) {
    return <LoadingState message="Loading MINIS account" />;
  }

  if (account.error) {
    if (account.error instanceof ApiClientError && account.error.status === 403) {
      const state: ForbiddenRouteState = { failure: toApiFailureView(account.error) };
      return <Navigate to="/forbidden" replace state={state} />;
    }
    return (
      <ApiFailureNotice
        failure={toApiFailureView(account.error)}
        onRetry={() => {
          void account.refetch();
        }}
      />
    );
  }

  if (!account.data) {
    return (
      <section className="notice">
        <h1>Account unavailable</h1>
        <p>The backend did not return an account for this session.</p>
        <button
          type="button"
          onClick={() => {
            void account.refetch();
          }}
        >
          Retry
        </button>
      </section>
    );
  }

  return (
    <section className="account-view" aria-labelledby="account-title">
      <p className="section-kicker">Backend account</p>
      <h1 id="account-title">Account</h1>
      <dl className="fact-list">
        <div>
          <dt>Email</dt>
          <dd>{account.data.email ?? "Not provided"}</dd>
        </div>
        <div>
          <dt>Role</dt>
          <dd>{account.data.role}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>{account.data.status}</dd>
        </div>
        <div>
          <dt>Email verified</dt>
          <dd>{account.data.emailVerified ? "Yes" : "No"}</dd>
        </div>
      </dl>
    </section>
  );
}
