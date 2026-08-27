import { describe, expect, it } from "vitest";
import { isNearOrPastExpiry } from "./mediaExpiry";

describe("isNearOrPastExpiry", () => {
  it("treats media as refreshable inside the bounded expiry window", () => {
    expect(
      isNearOrPastExpiry(
        "2026-07-17T12:00:59Z",
        new Date("2026-07-17T12:00:00Z"),
      ),
    ).toBe(true);
  });

  it("does not refresh well before expiration", () => {
    expect(
      isNearOrPastExpiry(
        "2026-07-17T12:02:00Z",
        new Date("2026-07-17T12:00:00Z"),
      ),
    ).toBe(false);
  });
});
