import { useEffect, useMemo, useRef, useState } from "react";
import { toApiFailureView } from "../../core/errors/apiError";
import { ApiFailureNotice } from "../../shared/components/ApiFailureNotice";
import { LoadingState } from "../../shared/components/LoadingState";
import { FeedCard } from "./FeedCard";
import { useCategoriesQuery, useFeedQuery } from "./feedQueries";

const MEDIA_WINDOW_RADIUS = 1;

export function FeedPage() {
  const [category, setCategory] = useState<string | undefined>();
  const [activeIndex, setActiveIndex] = useState(0);
  const feed = useFeedQuery(category);
  const categories = useCategoriesQuery();
  const itemNodes = useRef(new Map<number, HTMLElement>());

  const items = useMemo(() => feed.data?.pages.flatMap((page) => page.items) ?? [], [feed.data]);
  const safeActiveIndex =
    items.length === 0 ? 0 : Math.min(activeIndex, Math.max(0, items.length - 1));

  useEffect(() => {
    if (!("IntersectionObserver" in globalThis)) {
      return undefined;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        const visibleEntries = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio);
        const firstVisible = visibleEntries[0];
        if (!firstVisible) {
          return;
        }
        const indexValue = firstVisible.target.getAttribute("data-feed-index");
        if (!indexValue) {
          return;
        }
        const nextIndex = Number(indexValue);
        if (!Number.isNaN(nextIndex)) {
          setActiveIndex(nextIndex);
        }
      },
      { threshold: [0.55, 0.75] },
    );

    for (const [index, node] of itemNodes.current.entries()) {
      node.setAttribute("data-feed-index", String(index));
      observer.observe(node);
    }

    return () => {
      observer.disconnect();
    };
  }, [items.length]);

  function registerItem(index: number, node: HTMLElement | null) {
    if (node) {
      node.setAttribute("data-feed-index", String(index));
      itemNodes.current.set(index, node);
      return;
    }
    itemNodes.current.delete(index);
  }

  async function refreshExpiredMedia() {
    await feed.refetch();
  }

  if (feed.isLoading) {
    return <LoadingState message="Loading event feed" />;
  }

  if (feed.error) {
    return (
      <ApiFailureNotice
        failure={toApiFailureView(feed.error)}
        onRetry={() => {
          void feed.refetch();
        }}
      />
    );
  }

  return (
    <section className="feed-view" aria-labelledby="feed-title">
      <div className="feed-toolbar">
        <div>
          <p className="section-kicker">Guest feed</p>
          <h1 id="feed-title">MINIS Event</h1>
        </div>
        <label className="feed-filter">
          <span>Category</span>
          <select
            value={category ?? ""}
            onChange={(event) => {
              setCategory(event.currentTarget.value || undefined);
              setActiveIndex(0);
            }}
          >
            <option value="">All</option>
            {categories.data?.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {items.length === 0 ? (
        <section className="empty-state">
          <h2>No event videos yet</h2>
          <p>Imported creator videos will appear here when they are published.</p>
          <button
            type="button"
            onClick={() => {
              void feed.refetch();
            }}
          >
            Retry
          </button>
        </section>
      ) : (
        <div className="feed-stack">
          {items.map((item, index) => (
            <FeedCard
              key={item.id}
              active={safeActiveIndex === index}
              content={item}
              index={index}
              mountedVideo={Math.abs(safeActiveIndex - index) <= MEDIA_WINDOW_RADIUS}
              onExpiredMedia={refreshExpiredMedia}
              registerItem={registerItem}
            />
          ))}
        </div>
      )}

      {feed.hasNextPage ? (
        <div className="feed-pagination">
          <button
            type="button"
            disabled={feed.isFetchingNextPage}
            onClick={() => {
              void feed.fetchNextPage();
            }}
          >
            {feed.isFetchingNextPage ? "Loading" : "Load more"}
          </button>
        </div>
      ) : null}
    </section>
  );
}
