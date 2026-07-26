import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { User } from "firebase/auth";
import { useState, type ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthSessionProvider, useAuthSession } from "./AuthSessionContext";

const firebaseMocks = vi.hoisted(() => {
  const passwordUser = {
    email: "new@example.com",
    emailVerified: false,
    getIdToken: vi.fn(() => Promise.resolve("firebase-token")),
    providerData: [{ providerId: "password" }],
  } as unknown as User;
  const googleUser = {
    email: "google@example.com",
    emailVerified: true,
    getIdToken: vi.fn(() => Promise.resolve("google-token")),
    providerData: [{ providerId: "google.com" }],
  } as unknown as User;
  const auth: { currentUser: User | null } = { currentUser: null };
  return {
    auth,
    createUserWithEmailAndPassword: vi.fn(),
    firebaseSendPasswordResetEmail: vi.fn(),
    googleProvider: vi.fn(),
    googleUser,
    onAuthStateChanged: vi.fn(),
    passwordUser,
    reload: vi.fn(),
    sendEmailVerification: vi.fn(),
    signInWithEmailAndPassword: vi.fn(),
    signInWithPopup: vi.fn(),
    signOut: vi.fn(),
    updateProfile: vi.fn(),
  };
});

vi.mock("./firebaseClient", () => ({
  getFirebaseAuth: () => firebaseMocks.auth,
}));

vi.mock("firebase/auth", () => ({
  createUserWithEmailAndPassword: firebaseMocks.createUserWithEmailAndPassword,
  GoogleAuthProvider: firebaseMocks.googleProvider,
  onAuthStateChanged: firebaseMocks.onAuthStateChanged,
  reload: firebaseMocks.reload,
  sendEmailVerification: firebaseMocks.sendEmailVerification,
  sendPasswordResetEmail: firebaseMocks.firebaseSendPasswordResetEmail,
  signInWithEmailAndPassword: firebaseMocks.signInWithEmailAndPassword,
  signInWithPopup: firebaseMocks.signInWithPopup,
  signOut: firebaseMocks.signOut,
  updateProfile: firebaseMocks.updateProfile,
}));

describe("AuthSessionProvider", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    firebaseMocks.auth.currentUser = null;
    firebaseMocks.onAuthStateChanged.mockImplementation(
      (_auth: unknown, callback: (user: User | null) => void) => {
      callback(firebaseMocks.auth.currentUser);
      return vi.fn();
      },
    );
    firebaseMocks.createUserWithEmailAndPassword.mockResolvedValue({
      user: firebaseMocks.passwordUser,
    });
    firebaseMocks.firebaseSendPasswordResetEmail.mockResolvedValue(undefined);
    firebaseMocks.reload.mockResolvedValue(undefined);
    firebaseMocks.sendEmailVerification.mockResolvedValue(undefined);
    firebaseMocks.signInWithPopup.mockResolvedValue({ user: firebaseMocks.googleUser });
    firebaseMocks.signOut.mockResolvedValue(undefined);
    firebaseMocks.updateProfile.mockResolvedValue(undefined);
  });

  it("creates a password account, updates display name, and sends verification email", async () => {
    const user = userEvent.setup();
    renderWithAuthProvider(<RegisterProbe />);

    await user.click(await screen.findByRole("button", { name: "Register" }));

    expect(await screen.findByText("registered:true:true")).toBeInTheDocument();
    expect(firebaseMocks.createUserWithEmailAndPassword).toHaveBeenCalledWith(
      firebaseMocks.auth,
      "new@example.com",
      "viewer-password",
    );
    expect(firebaseMocks.updateProfile).toHaveBeenCalledWith(firebaseMocks.passwordUser, {
      displayName: "New Viewer",
    });
    expect(firebaseMocks.sendEmailVerification).toHaveBeenCalledWith(firebaseMocks.passwordUser);
  });

  it("reports partial registration state when profile update and verification send fail", async () => {
    const user = userEvent.setup();
    firebaseMocks.updateProfile.mockRejectedValue(new Error("profile failed"));
    firebaseMocks.sendEmailVerification.mockRejectedValue(new Error("mail failed"));
    renderWithAuthProvider(<RegisterProbe />);

    await user.click(await screen.findByRole("button", { name: "Register" }));

    expect(await screen.findByText("registered:false:false")).toBeInTheDocument();
  });

  it("uses Google popup sign-in and Firebase password reset", async () => {
    const user = userEvent.setup();
    renderWithAuthProvider(<GoogleAndResetProbe />);

    await user.click(await screen.findByRole("button", { name: "Google" }));
    await user.click(screen.getByRole("button", { name: "Reset" }));

    await waitFor(() => {
      expect(firebaseMocks.signInWithPopup).toHaveBeenCalledTimes(1);
    });
    expect(firebaseMocks.firebaseSendPasswordResetEmail).toHaveBeenCalledWith(
      firebaseMocks.auth,
      "viewer@example.com",
    );
    expect(screen.getByText("done")).toBeInTheDocument();
  });
});

function RegisterProbe() {
  const auth = useAuthSession();
  const [result, setResult] = useState("idle");

  return (
    <>
      <button
        type="button"
        onClick={() => {
          void auth
            .registerWithEmail({
              displayName: "New Viewer",
              email: "new@example.com",
              password: "viewer-password",
            })
            .then((registration) => {
              setResult(
                `registered:${String(registration.displayNameUpdated)}:${String(
                  registration.verificationEmailSent,
                )}`,
              );
            });
        }}
      >
        Register
      </button>
      <p>{result}</p>
    </>
  );
}

function GoogleAndResetProbe() {
  const auth = useAuthSession();
  const [done, setDone] = useState(false);

  return (
    <>
      <button
        type="button"
        onClick={() => {
          void auth.signInWithGoogle();
        }}
      >
        Google
      </button>
      <button
        type="button"
        onClick={() => {
          void auth.sendPasswordResetEmail("viewer@example.com").then(() => setDone(true));
        }}
      >
        Reset
      </button>
      {done ? <p>done</p> : null}
    </>
  );
}

function renderWithAuthProvider(children: ReactNode) {
  const queryClient = new QueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthSessionProvider>{children}</AuthSessionProvider>
    </QueryClientProvider>,
  );
}
