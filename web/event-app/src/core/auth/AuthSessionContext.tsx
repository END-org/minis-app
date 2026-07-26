import { useQueryClient } from "@tanstack/react-query";
import {
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  onAuthStateChanged,
  reload,
  sendEmailVerification,
  sendPasswordResetEmail as firebaseSendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut as firebaseSignOut,
  updateProfile,
  type Auth,
  type User,
} from "firebase/auth";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { getFirebaseAuth } from "./firebaseClient";

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

interface AuthSessionProviderProps {
  readonly children: ReactNode;
}

export function AuthSessionProvider({ children }: AuthSessionProviderProps) {
  const queryClient = useQueryClient();
  const [authStartup] = useState(createAuthStartup);
  const auth = authStartup.auth;
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(auth !== null);
  const [sessionVersion, setSessionVersion] = useState(0);
  const setupError = authStartup.setupError;
  const [sessionNotice, setSessionNotice] = useState<SessionNotice | null>(null);

  useEffect(() => {
    if (!auth) {
      return undefined;
    }
    return onAuthStateChanged(auth, (nextUser) => {
      setUser(nextUser);
      setSessionVersion((current) => current + 1);
      setLoading(false);
      if (!nextUser) {
        queryClient.clear();
      }
    });
  }, [auth, queryClient]);

  const signInWithEmail = useCallback(
    async (email: string, password: string) => {
      if (!auth) {
        throw new Error("Authentication is still starting.");
      }
      const credential = await signInWithEmailAndPassword(auth, email, password);
      await reload(credential.user);
      setUser(credential.user);
      setSessionVersion((current) => current + 1);
      setSessionNotice(null);
      return credential.user;
    },
    [auth],
  );

  const registerWithEmail = useCallback(
    async (request: RegisterWithEmailRequest) => {
      if (!auth) {
        throw new Error("Authentication is still starting.");
      }
      const credential = await createUserWithEmailAndPassword(
        auth,
        request.email,
        request.password,
      );
      let displayNameUpdated = true;
      let verificationEmailSent = true;
      try {
        await updateProfile(credential.user, { displayName: request.displayName });
      } catch {
        displayNameUpdated = false;
      }
      try {
        await sendEmailVerification(credential.user);
      } catch {
        verificationEmailSent = false;
      }
      await reload(credential.user);
      setUser(credential.user);
      setSessionVersion((current) => current + 1);
      setSessionNotice(null);
      return { displayNameUpdated, verificationEmailSent };
    },
    [auth],
  );

  const signInWithGoogle = useCallback(async () => {
    if (!auth) {
      throw new Error("Authentication is still starting.");
    }
    const provider = new GoogleAuthProvider();
    const credential = await signInWithPopup(auth, provider);
    await reload(credential.user);
    setUser(credential.user);
    setSessionNotice(null);
    return credential.user;
  }, [auth]);

  const signOut = useCallback(async () => {
    queryClient.clear();
    setSessionNotice(null);
    if (auth) {
      await firebaseSignOut(auth);
    }
    setUser(null);
    setSessionVersion((current) => current + 1);
  }, [auth, queryClient]);

  const reloadCurrentUser = useCallback(async () => {
    if (!auth?.currentUser) {
      throw new Error("No Firebase user is signed in.");
    }
    await reload(auth.currentUser);
    setUser(auth.currentUser);
    setSessionVersion((current) => current + 1);
    return auth.currentUser;
  }, [auth]);

  const refreshCurrentToken = useCallback(async () => {
    if (!auth?.currentUser) {
      throw new Error("No Firebase user is signed in.");
    }
    await auth.currentUser.getIdToken(true);
  }, [auth]);

  const sendCurrentUserVerificationEmail = useCallback(async () => {
    if (!auth?.currentUser) {
      throw new Error("No Firebase user is signed in.");
    }
    await sendEmailVerification(auth.currentUser);
  }, [auth]);

  const sendPasswordResetEmail = useCallback(
    async (email: string) => {
      if (!auth) {
        throw new Error("Authentication is still starting.");
      }
      await firebaseSendPasswordResetEmail(auth, email);
    },
    [auth],
  );

  const expireAuthenticatedSession = useCallback(
    async (notice: SessionNotice) => {
      queryClient.clear();
      setSessionNotice(notice);
      if (auth?.currentUser) {
        await firebaseSignOut(auth);
      }
      setUser(null);
      setSessionVersion((current) => current + 1);
    },
    [auth, queryClient],
  );

  const clearSessionNotice = useCallback(() => {
    setSessionNotice(null);
  }, []);

  const currentUser = useCallback(() => auth?.currentUser ?? null, [auth]);

  const value = useMemo<AuthSessionContextValue>(
    () => ({
      user,
      loading,
      sessionVersion,
      setupError,
      sessionNotice,
      registerWithEmail,
      signInWithEmail,
      signInWithGoogle,
      signOut,
      reloadCurrentUser,
      refreshCurrentToken,
      sendCurrentUserVerificationEmail,
      sendPasswordResetEmail,
      expireAuthenticatedSession,
      clearSessionNotice,
      currentUser,
    }),
    [
      user,
      loading,
      sessionVersion,
      setupError,
      sessionNotice,
      registerWithEmail,
      signInWithEmail,
      signInWithGoogle,
      signOut,
      reloadCurrentUser,
      refreshCurrentToken,
      sendCurrentUserVerificationEmail,
      sendPasswordResetEmail,
      expireAuthenticatedSession,
      clearSessionNotice,
      currentUser,
    ],
  );

  return <AuthSessionContext.Provider value={value}>{children}</AuthSessionContext.Provider>;
}

export function useAuthSession(): AuthSessionContextValue {
  const value = useContext(AuthSessionContext);
  if (!value) {
    throw new Error("useAuthSession must be used inside AuthSessionProvider.");
  }
  return value;
}

function toErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Authentication could not start.";
}

function createAuthStartup(): { readonly auth: Auth | null; readonly setupError: string | null } {
  try {
    return { auth: getFirebaseAuth(), setupError: null };
  } catch (error) {
    return { auth: null, setupError: toErrorMessage(error) };
  }
}

export function isEmailPasswordUser(user: User): boolean {
  return user.providerData.some((provider) => provider.providerId === "password");
}
