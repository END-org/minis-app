import { useEffect, useRef, useState } from "react";
import type { ContentResponse } from "./feedApi";
import { formatDuration, isNearOrPastExpiry } from "./mediaExpiry";

interface FeedVideoProps {
  readonly active: boolean;
  readonly content: ContentResponse;
  readonly onExpiredMedia: (contentId: string) => Promise<void>;
}

export function FeedVideo({ active, content, onExpiredMedia }: FeedVideoProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [autoplayBlocked, setAutoplayBlocked] = useState(false);
  const [broken, setBroken] = useState(false);
  const [currentSeconds, setCurrentSeconds] = useState(0);
  const [durationSeconds, setDurationSeconds] = useState(durationFromMetadata(content));
  const [manualPaused, setManualPaused] = useState(false);
  const [expiryRetryUsed, setExpiryRetryUsed] = useState(false);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) {
      return;
    }
    if (!active) {
      video.pause();
      return;
    }
    if (manualPaused) {
      return;
    }
    video.muted = true;
    void video
      .play()
      .then(() => {
        setAutoplayBlocked(false);
      })
      .catch(() => {
        setAutoplayBlocked(true);
      });
  }, [active, content.media.url, manualPaused]);

  function handlePause() {
    setManualPaused(true);
    videoRef.current?.pause();
  }

  function handlePlay() {
    setManualPaused(false);
    setAutoplayBlocked(false);
    void videoRef.current?.play().catch(() => {
      setAutoplayBlocked(true);
    });
  }

  function handleSeek(value: string) {
    const nextSeconds = Number(value);
    if (Number.isNaN(nextSeconds) || !videoRef.current) {
      return;
    }
    videoRef.current.currentTime = nextSeconds;
    setCurrentSeconds(nextSeconds);
  }

  async function handleMediaError() {
    if (!expiryRetryUsed && isNearOrPastExpiry(content.media.expiresAt)) {
      setExpiryRetryUsed(true);
      await onExpiredMedia(content.id);
      return;
    }
    setBroken(true);
  }

  const durationLabel = formatDuration(content.media.durationMs);

  return (
    <div className="feed-video-shell">
      <video
        ref={videoRef}
        aria-label={`Video: ${content.title}`}
        className="feed-video"
        controls={false}
        muted
        playsInline
        preload="metadata"
        src={content.media.url}
        onDurationChange={(event) => {
          const nextDuration = event.currentTarget.duration;
          if (Number.isFinite(nextDuration)) {
            setDurationSeconds(nextDuration);
          }
        }}
        onError={() => {
          void handleMediaError();
        }}
        onTimeUpdate={(event) => {
          setCurrentSeconds(event.currentTarget.currentTime);
        }}
      />
      {durationLabel ? <span className="feed-video-duration">{durationLabel}</span> : null}
      {autoplayBlocked ? (
        <div className="feed-video-overlay" role="status">
          <p>Autoplay was blocked.</p>
          <button type="button" onClick={handlePlay}>
            Play
          </button>
        </div>
      ) : null}
      {broken ? (
        <div className="feed-video-overlay" role="status">
          <p>This video could not be loaded.</p>
          <button
            type="button"
            onClick={() => {
              setBroken(false);
              videoRef.current?.load();
            }}
          >
            Retry
          </button>
        </div>
      ) : null}
      <div className="feed-video-controls">
        <button type="button" onClick={manualPaused ? handlePlay : handlePause}>
          {manualPaused ? "Play" : "Pause"}
        </button>
        <label>
          <span>Progress</span>
          <input
            aria-label={`Seek ${content.title}`}
            type="range"
            min="0"
            max={String(Math.max(1, Math.round(durationSeconds)))}
            value={String(Math.min(currentSeconds, durationSeconds || currentSeconds))}
            onChange={(event) => {
              handleSeek(event.currentTarget.value);
            }}
          />
        </label>
      </div>
    </div>
  );
}

function durationFromMetadata(content: ContentResponse): number {
  if (content.media.durationMs === undefined) {
    return 0;
  }
  return Math.max(0, content.media.durationMs / 1000);
}
