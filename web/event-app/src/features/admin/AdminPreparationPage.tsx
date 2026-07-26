import { useState } from "react";
import { Link } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { useAdminUsersQuery } from "./adminQueries";

export function AdminPreparationPage() {
  const [query, setQuery] = useState("");
  const users = useAdminUsersQuery(query);

  if (users.isLoading) {
    return <LoadingState message="Loading event accounts" />;
  }

  return (
    <section className="admin-view" aria-labelledby="admin-title">
      <p className="section-kicker">Event preparation</p>
      <h1 id="admin-title">Admin</h1>
      <label className="search-field">
        Search users
        <input
          value={query}
          onChange={(event) => setQuery(event.currentTarget.value)}
          maxLength={120}
          placeholder="Email, display name, or slug"
        />
      </label>
      {users.error ? (
        <ApiFailureNotice
          failure={toApiFailureView(users.error)}
          onRetry={() => {
            void users.refetch();
          }}
        />
      ) : null}
      {users.data?.users.length === 0 ? (
        <div className="empty-state">
          <h2>No accounts found</h2>
          <p>Prepared Firebase identities appear here after backend bootstrap.</p>
        </div>
      ) : null}
      {users.data && users.data.users.length > 0 ? (
        <ul className="admin-user-list">
          {users.data.users.map((user) => (
            <li key={user.id}>
              <Link to={`/admin/users/${user.id}`}>
                <span>{user.displayName ?? user.email ?? "Unnamed account"}</span>
                <small>
                  {user.role} / {user.status}
                </small>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
