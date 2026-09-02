import { Link } from "react-router-dom";
import { ContentSocialActions } from "../social/ContentSocialActions";
import type { ContentResponse } from "./feedApi";
import { FeedVideo } from "./FeedVideo";

interface FeedCardProps {
  readonly active: boolean;
  readonly content: ContentResponse;
  readonly index: number;
  readonly mountedVideo: boolean;
  readonly onExpiredMedia: (contentId: string) => Promise<void>;
  readonly registerItem: (index: number, node: HTMLElement | null) => void;
}

export function FeedCard({
  active,
  content,
  index,
  mountedVideo,
  onExpiredMedia,
  registerItem,
}: FeedCardProps) {
  return (
    <article
      ref={(node) => {
        registerItem(index, node);
      }}
      className="feed-card"
      aria-label={content.title}
    >
      {mountedVideo ? (
        <FeedVideo
          key={content.media.url}
          active={active}
          content={content}
          onExpiredMedia={onExpiredMedia}
        />
      ) : (
        <div className="feed-video-placeholder" aria-hidden="true" />
      )}
      <div className="feed-card__copy">
        <p className="section-kicker">{content.categoryLabel}</p>
        <h1>{content.title}</h1>
        <p className="feed-description">{content.description}</p>
        <Link className="feed-creator-link" to={`/creators/${content.creator.slug}`}>
          {content.creator.avatarUrl ? (
            <img src={content.creator.avatarUrl} alt="" className="feed-creator-avatar" />
          ) : null}
          <span>{content.creator.displayName}</span>
          {content.creator.verified ? <strong>Verified</strong> : null}
        </Link>
        <ContentSocialActions compact content={content} />
      </div>
    </article>
  );
}
