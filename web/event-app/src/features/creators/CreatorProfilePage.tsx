import { Link, useParams } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { useCreatorQuery } from "./creatorQueries";

export function CreatorProfilePage() {
  const { slug } = useParams();
  const creator = useCreatorQuery(slug);

  if (creator.isLoading) {
    return <LoadingState message="Loading creator profile" />;
  }

  if (creator.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(creator.error)}
        onRetry={() => {
          void creator.refetch();
        }}
      />
    );
  }

  if (!creator.data) {
    return (
      <section className="notice">
        <h1>Creator unavailable</h1>
        <p>This creator profile is not available for public viewing.</p>
        <Link to="/creators">Back to creators</Link>
      </section>
    );
  }

  return (
    <section className="creator-profile" aria-labelledby="creator-title">
      <p className="section-kicker">Creator profile</p>
      {creator.data.avatarUrl ? (
        <img className="avatar-preview" src={creator.data.avatarUrl} alt="" />
      ) : null}
      <h1 id="creator-title">{creator.data.displayName}</h1>
      {creator.data.verified ? <p className="status-pill">Verified creator</p> : null}
      {creator.data.bio ? <p className="lead">{creator.data.bio}</p> : null}
      <p className="muted-link">
        <Link to="/creators">Back to creators</Link>
      </p>
    </section>
  );
}
