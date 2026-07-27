import { Link } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { useCreatorsQuery } from "./creatorQueries";

export function CreatorsDirectoryPage() {
  const creators = useCreatorsQuery();

  if (creators.isLoading) {
    return <LoadingState message="Loading creators" />;
  }

  if (creators.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(creators.error)}
        onRetry={() => {
          void creators.refetch();
        }}
      />
    );
  }

  const creatorList = creators.data?.creators ?? [];

  return (
    <section className="directory-view" aria-labelledby="creators-title">
      <p className="section-kicker">Public directory</p>
      <h1 id="creators-title">Creators</h1>
      {creatorList.length === 0 ? (
        <div className="empty-state">
          <h2>No active creators yet</h2>
          <p>Event creators will appear here after an admin prepares their profiles.</p>
          <button
            type="button"
            onClick={() => {
              void creators.refetch();
            }}
          >
            Retry
          </button>
        </div>
      ) : (
        <ul className="creator-list">
          {creatorList.map((creator) => (
            <li key={creator.userId}>
              <Link to={`/creators/${creator.slug}`}>
                <span>{creator.displayName}</span>
                {creator.verified ? <strong>Verified</strong> : null}
              </Link>
              {creator.bio ? <p>{creator.bio}</p> : null}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
