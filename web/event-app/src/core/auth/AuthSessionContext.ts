import type { User } from "firebase/auth";
import { createContext, useContext } from "react";

export interface SessionNotice {
  readonly title: string;
  readonly message: string;
}

export interface RegisterWithEmailRequest {
  readonly displayName: string;
  readonly email: string;
  readonly password: string;
}

export interface RegisterWithEmailResult {
  readonly displayNameUpdated: boolean;
  readonly verificationEmailSent: boolean;
}

export interface AuthSessionContextValue {
  readonly user: User | null;
  readonly loading: boolean;
  readonly sessionVersion: number;
  readonly setupError: string | null;
  readonly sessionNotice: SessionNotice | null;
  readonly registerWithEmail: (
    request: RegisterWithEmailRequest,
  ) => Promise<RegisterWithEmailResult>;
  readonly signInWithEmail: (email: string, password: string) => Promise<User>;
  readonly signInWithGoogle: () => Promise<User>;
  readonly signOut: () => Promise<void>;
  readonly reloadCurrentUser: () => Promise<User>;
  readonly refreshCurrentToken: () => Promise<void>;
  readonly sendCurrentUserVerificationEmail: () => Promise<void>;
  readonly sendPasswordResetEmail: (email: string) => Promise<void>;
  readonly expireAuthenticatedSession: (notice: SessionNotice) => Promise<void>;
  readonly clearSessionNotice: () => void;
  readonly currentUser: () => User | null;
}

export const AuthSessionContext = createContext<AuthSessionContextValue | null>(null);

export function useAuthSession(): AuthSessionContextValue {
  const value = useContext(AuthSessionContext);
  if (!value) {
    throw new Error("useAuthSession must be used inside AuthSessionProvider.");
  }
  return value;
}

export function isEmailPasswordUser(user: User): boolean {
  return user.providerData.some((provider) => provider.providerId === "password");
}
