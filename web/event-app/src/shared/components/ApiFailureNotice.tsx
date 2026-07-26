import type { ApiFailureView } from "../../core/errors/apiError";

interface ApiFailureNoticeProps {
  readonly failure: ApiFailureView;
  readonly onRetry?: () => void;
}

export function ApiFailureNotice({ failure, onRetry }: ApiFailureNoticeProps) {
  return (
    <section className="notice notice--error" aria-live="polite">
      <p className="notice__eyebrow">
        {failure.status ? `HTTP ${String(failure.status)}` : failure.code}
      </p>
      <h2>{failure.code}</h2>
      <p>{failure.message}</p>
      {failure.correlationId ? (
        <p className="support-code">Correlation ID: {failure.correlationId}</p>
      ) : null}
      {onRetry ? (
        <button type="button" onClick={onRetry}>
          Retry
        </button>
      ) : null}
    </section>
  );
}
