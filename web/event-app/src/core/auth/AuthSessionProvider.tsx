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
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  AuthSessionContext,
  type AuthSessionContextValue,
  type RegisterWithEmailRequest,
  type SessionNotice,
} from "./AuthSessionContext";
import { getFirebaseAuth } from "./firebaseClient";

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
