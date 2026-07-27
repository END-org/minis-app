import { describe, expect, it } from "vitest";
import { toFirebaseAuthMessage } from "./firebaseError";

describe("toFirebaseAuthMessage", () => {
  it("maps configured Firebase errors to safe user messages", () => {
    expect(toFirebaseAuthMessage({ code: "auth/email-already-in-use" })).toContain(
      "already registered",
    );
    expect(toFirebaseAuthMessage({ code: "auth/popup-blocked" })).toContain("popup");
    expect(toFirebaseAuthMessage({ code: "auth/unauthorized-domain" })).toContain(
      "not authorized",
    );
  });

  it("does not render raw unknown Firebase error details", () => {
    expect(toFirebaseAuthMessage({ code: "auth/internal-error", message: "secret-token" })).toBe(
      "Firebase authentication could not complete. Try again.",
    );
  });
});
