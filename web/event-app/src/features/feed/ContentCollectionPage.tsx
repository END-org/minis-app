import { Link } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { ContentSocialActions } from "../social/ContentSocialActions";
import { useLikedContentQuery, useSavedContentQuery } from "./feedQueries";

interface ContentCollectionPageProps {
  readonly collection: ReturnType<typeof useLikedContentQuery>;
  readonly emptyMessage: string;
  readonly kicker: string;
  readonly title: string;
}

export function LikedContentPage() {
  return (
    <ContentCollectionPage
      collection={useLikedContentQuery()}
      emptyMessage="Videos you like will collect here."
      kicker="Your activity"
      title="Liked Videos"
    />
  );
}

export function SavedContentPage() {
  return (
    <ContentCollectionPage
      collection={useSavedContentQuery()}
      emptyMessage="Save event videos to build a private watch list."
      kicker="Your library"
      title="Saved Videos"
    />
  );
}

function ContentCollectionPage({
  collection,
  emptyMessage,
  kicker,
  title,
}: ContentCollectionPageProps) {
  const items = collection.data?.pages.flatMap((page) => page.items) ?? [];

  if (collection.isLoading) {
    return <LoadingState message={`Loading ${title.toLowerCase()}`} />;
  }

  if (collection.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(collection.error)}
        onRetry={() => {
          void collection.refetch();
        }}
      />
    );
  }

  return (
    <section className="collection-view" aria-labelledby="collection-title">
      <div className="page-heading">
        <p className="section-kicker">{kicker}</p>
        <h1 id="collection-title">{title}</h1>
      </div>
      {items.length === 0 ? (
        <section className="empty-state">
          <h2>Nothing here yet</h2>
          <p>{emptyMessage}</p>
          <Link to="/">Browse the feed</Link>
        </section>
      ) : (
        <ul className="collection-grid">
          {items.map((item) => (
            <li key={item.id} className="collection-card">
              <Link to={`/content/${item.id}`} className="collection-card__link">
                <span>{item.categoryLabel}</span>
                <strong>{item.title}</strong>
                <small>{item.creator.displayName}</small>
              </Link>
              <ContentSocialActions compact content={item} />
            </li>
          ))}
        </ul>
      )}
      {collection.hasNextPage ? (
        <div className="feed-pagination">
          <button
            type="button"
            disabled={collection.isFetchingNextPage}
            onClick={() => {
              void collection.fetchNextPage();
            }}
          >
            {collection.isFetchingNextPage ? "Loading" : "Load more"}
          </button>
        </div>
      ) : null}
    </section>
  );
}
