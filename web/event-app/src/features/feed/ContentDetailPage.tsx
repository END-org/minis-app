import { Link, useParams } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { FeedVideo } from "./FeedVideo";
import { useContentQuery } from "./feedQueries";

export function ContentDetailPage() {
  const { contentId } = useParams();
  const content = useContentQuery(contentId);

  if (content.isLoading) {
    return <LoadingState message="Loading event video" />;
  }

  if (content.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(content.error)}
        onRetry={() => {
          void content.refetch();
        }}
      />
    );
  }

  if (!content.data) {
    return (
      <section className="notice">
        <h1>Video unavailable</h1>
        <p>This event video is not available for public viewing.</p>
        <Link to="/">Back to feed</Link>
      </section>
    );
  }

  return (
    <section className="content-detail" aria-labelledby="content-title">
      <FeedVideo
        active
        content={content.data}
        onExpiredMedia={() => content.refetch().then(() => undefined)}
      />
      <div className="content-detail__copy">
        <p className="section-kicker">{content.data.categoryLabel}</p>
        <h1 id="content-title">{content.data.title}</h1>
        <p className="lead">{content.data.description}</p>
        <p>
          <Link to={`/creators/${content.data.creator.slug}`}>
            {content.data.creator.displayName}
          </Link>
        </p>
      </div>
    </section>
  );
}
