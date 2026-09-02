import { Link } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { useMyUploadsQuery } from "./uploadQueries";
import type { ContentUploadResponse, ContentUploadStatus } from "./uploadApi";

export function MyUploadsPage() {
  const uploads = useMyUploadsQuery();
  const items = uploads.data?.pages.flatMap((page) => page.items) ?? [];

  if (uploads.isLoading) {
    return <LoadingState message="Loading uploads" />;
  }

  if (uploads.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(uploads.error)}
        onRetry={() => {
          void uploads.refetch();
        }}
      />
    );
  }

  return (
    <section className="uploads-view" aria-labelledby="uploads-title">
      <div className="page-heading page-heading--with-action">
        <div>
          <p className="section-kicker">Creator studio</p>
          <h1 id="uploads-title">My Uploads</h1>
        </div>
        <Link to="/upload">Upload video</Link>
      </div>

      {items.length === 0 ? (
        <section className="empty-state">
          <h2>No uploads yet</h2>
          <p>Your creator uploads will appear here with processing and failure status.</p>
          <Link to="/upload">Upload your first video</Link>
        </section>
      ) : (
        <ul className="uploads-list">
          {items.map((item) => (
            <UploadListItem key={item.id} item={item} />
          ))}
        </ul>
      )}

      {uploads.hasNextPage ? (
        <div className="feed-pagination">
          <button
            type="button"
            disabled={uploads.isFetchingNextPage}
            onClick={() => {
              void uploads.fetchNextPage();
            }}
          >
            {uploads.isFetchingNextPage ? "Loading" : "Load more"}
          </button>
        </div>
      ) : null}
    </section>
  );
}

function UploadListItem({ item }: { readonly item: ContentUploadResponse }) {
  return (
    <li className="upload-list-item">
      <div>
        <span className={statusClass(item.status)}>{item.status}</span>
        <h2>{item.title}</h2>
        <p>{item.description}</p>
        <small>
          {item.categoryLabel} - {formatDate(item.createdAt)}
        </small>
        {item.failureReason ? (
          <p className="form-error" role="status">
            {item.failureReason}
          </p>
        ) : null}
      </div>
      <div className="upload-list-item__actions">
        {item.content ? <Link to={`/content/${item.content.id}`}>Open video</Link> : null}
        {item.status === "FAILED" ? <Link to="/upload">Start a new attempt</Link> : null}
      </div>
    </li>
  );
}

function statusClass(status: ContentUploadStatus): string {
  return `status-pill upload-status upload-status--${status.toLowerCase()}`;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
