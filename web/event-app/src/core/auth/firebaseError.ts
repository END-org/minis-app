export function toFirebaseAuthMessage(error: unknown): string {
  const code = firebaseErrorCode(error);
  switch (code) {
    case "auth/email-already-in-use":
      return "That email is already registered. Sign in or reset the password.";
    case "auth/weak-password":
      return "Choose a stronger password with at least six characters.";
    case "auth/invalid-email":
      return "Enter a valid email address.";
    case "auth/invalid-credential":
      return "The email or password is not correct.";
    case "auth/user-disabled":
      return "This Firebase account is disabled.";
    case "auth/too-many-requests":
      return "Too many attempts. Wait a moment and try again.";
    case "auth/network-request-failed":
      return "Firebase could not be reached. Check the connection and retry.";
    case "auth/popup-closed-by-user":
      return "Google sign-in was cancelled before completion.";
    case "auth/popup-blocked":
      return "The browser blocked the Google sign-in popup.";
    case "auth/unauthorized-domain":
      return "This domain is not authorized for Firebase sign-in.";
    case "auth/operation-not-allowed":
      return "This sign-in method is not enabled for the Firebase project.";
    case "auth/account-exists-with-different-credential":
      return "That email already uses another Firebase sign-in method.";
    case "auth/requires-recent-login":
      return "Sign in again before completing that account change.";
    default:
      return "Firebase authentication could not complete. Try again.";
  }
}

function firebaseErrorCode(error: unknown): string | null {
  if (!isRecord(error) || typeof error.code !== "string") {
    return null;
  }
  return error.code;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
