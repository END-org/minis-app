import { Link } from "react-router-dom";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { useCreatorContentQuery } from "./feedQueries";

interface CreatorContentSectionProps {
  readonly slug: string | undefined;
}

export function CreatorContentSection({ slug }: CreatorContentSectionProps) {
  const content = useCreatorContentQuery(slug);
  const items = content.data?.pages.flatMap((page) => page.items) ?? [];

  if (content.isLoading) {
    return <p className="empty-state">Loading creator videos.</p>;
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

  if (items.length === 0) {
    return (
      <section className="empty-state">
        <h2>No published videos</h2>
        <p>This creator does not have public event videos yet.</p>
      </section>
    );
  }

  return (
    <section className="creator-content" aria-labelledby="creator-content-title">
      <h2 id="creator-content-title">Published videos</h2>
      <ul className="creator-content-list">
        {items.map((item) => (
          <li key={item.id}>
            <Link to={`/content/${item.id}`}>
              <span>{item.title}</span>
              <strong>{item.categoryLabel}</strong>
            </Link>
          </li>
        ))}
      </ul>
      {content.hasNextPage ? (
        <button
          type="button"
          disabled={content.isFetchingNextPage}
          onClick={() => {
            void content.fetchNextPage();
          }}
        >
          {content.isFetchingNextPage ? "Loading" : "Load more"}
        </button>
      ) : null}
    </section>
  );
}
