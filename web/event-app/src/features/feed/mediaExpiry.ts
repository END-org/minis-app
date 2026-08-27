const EXPIRY_RETRY_WINDOW_MS = 60_000;

export function isNearOrPastExpiry(expiresAt: string, now: Date = new Date()): boolean {
  const expiresAtMs = Date.parse(expiresAt);
  if (Number.isNaN(expiresAtMs)) {
    return false;
  }
  return expiresAtMs - now.getTime() <= EXPIRY_RETRY_WINDOW_MS;
}

export function formatDuration(durationMs: number | undefined): string | null {
  if (durationMs === undefined) {
    return null;
  }
  const totalSeconds = Math.max(0, Math.round(durationMs / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes)}:${String(seconds).padStart(2, "0")}`;
}
