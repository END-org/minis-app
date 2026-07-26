export function intendedDestination(search: string, fallback = "/account"): string {
  const next = new URLSearchParams(search).get("next");
  if (!next || !isSafeInternalDestination(next)) {
    return fallback;
  }
  return next;
}

export function destinationSearch(destination: string): string {
  return `next=${encodeURIComponent(destination)}`;
}

export function isSafeInternalDestination(value: string): boolean {
  return value.startsWith("/") && !value.startsWith("//") && !value.startsWith("/api/");
}
