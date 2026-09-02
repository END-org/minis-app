import { Link, useParams } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { useAdminUserQuery } from "./adminQueries";
import { AdminUserActions } from "./AdminUserActions";

export function AdminUserDetailPage() {
  const { userId } = useParams();
  const user = useAdminUserQuery(userId);

  if (user.isLoading) {
    return <LoadingState message="Loading account detail" />;
  }

  if (user.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(user.error)}
        onRetry={() => {
          void user.refetch();
        }}
      />
    );
  }

  if (!user.data || !userId) {
    return (
      <section className="notice">
        <h1>User unavailable</h1>
        <Link to="/admin">Back to admin</Link>
      </section>
    );
  }

  return (
    <section className="admin-view" aria-labelledby="admin-user-title">
      <p className="section-kicker">Event account</p>
      <h1 id="admin-user-title">
        {user.data.displayName ?? user.data.email ?? "Account"}
      </h1>
      <dl className="fact-list">
        <div>
          <dt>Email</dt>
          <dd>{user.data.email ?? "Not provided"}</dd>
        </div>
        <div>
          <dt>Role</dt>
          <dd>{user.data.role}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>{user.data.status}</dd>
        </div>
        <div>
          <dt>Creator slug</dt>
          <dd>{user.data.creatorProfile?.slug ?? "Not prepared"}</dd>
        </div>
        <div>
          <dt>Verified</dt>
          <dd>{user.data.creatorProfile?.verified ? "Yes" : "No"}</dd>
        </div>
      </dl>
      <AdminUserActions user={user.data} />
      <p className="muted-link">
        <Link to="/admin">Back to admin</Link>
      </p>
    </section>
  );
}
