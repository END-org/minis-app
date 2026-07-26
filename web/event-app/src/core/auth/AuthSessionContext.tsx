import { useQueryClient } from "@tanstack/react-query";
import {
  onAuthStateChanged,
  reload,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
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

export interface AuthSessionContextValue {
  readonly user: User | null;
  readonly loading: boolean;
  readonly setupError: string | null;
  readonly sessionNotice: SessionNotice | null;
  readonly signInWithEmail: (email: string, password: string) => Promise<void>;
  readonly signOut: () => Promise<void>;
  readonly reloadCurrentUser: () => Promise<void>;
  readonly expireAuthenticatedSession: (notice: SessionNotice) => Promise<void>;
  readonly clearSessionNotice: () => void;
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
  const setupError = authStartup.setupError;
  const [sessionNotice, setSessionNotice] = useState<SessionNotice | null>(null);

  useEffect(() => {
    if (!auth) {
      return undefined;
    }
    return onAuthStateChanged(auth, (nextUser) => {
      setUser(nextUser);
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
      setSessionNotice(null);
    },
    [auth],
  );

  const signOut = useCallback(async () => {
    queryClient.clear();
    setSessionNotice(null);
    if (auth) {
      await firebaseSignOut(auth);
    }
    setUser(null);
  }, [auth, queryClient]);

  const reloadCurrentUser = useCallback(async () => {
    if (!auth?.currentUser) {
      throw new Error("No Firebase user is signed in.");
    }
    await reload(auth.currentUser);
    setUser(auth.currentUser);
  }, [auth]);

  const expireAuthenticatedSession = useCallback(
    async (notice: SessionNotice) => {
      queryClient.clear();
      setSessionNotice(notice);
      if (auth?.currentUser) {
        await firebaseSignOut(auth);
      }
      setUser(null);
    },
    [auth, queryClient],
  );

  const clearSessionNotice = useCallback(() => {
    setSessionNotice(null);
  }, []);

  const value = useMemo<AuthSessionContextValue>(
    () => ({
      user,
      loading,
      setupError,
      sessionNotice,
      signInWithEmail,
      signOut,
      reloadCurrentUser,
      expireAuthenticatedSession,
      clearSessionNotice,
    }),
    [
      user,
      loading,
      setupError,
      sessionNotice,
      signInWithEmail,
      signOut,
      reloadCurrentUser,
      expireAuthenticatedSession,
      clearSessionNotice,
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
