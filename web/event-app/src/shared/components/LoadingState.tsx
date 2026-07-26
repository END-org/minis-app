interface LoadingStateProps {
  readonly message?: string;
}

export function LoadingState({ message = "Starting MINIS" }: LoadingStateProps) {
  return (
    <div className="loading-state" role="status" aria-live="polite">
      <img src="/minis-mark.svg" alt="" className="loading-state__mark" />
      <span>{message}</span>
    </div>
  );
}
