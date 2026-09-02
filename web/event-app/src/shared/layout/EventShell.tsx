import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuthSession } from "../../core/auth/AuthSessionContext";
import { useBackendSession } from "../../features/account/BackendSessionContext";
import type { ReactNode } from "react";

interface EventShellProps {
  readonly children: ReactNode;
}

export function EventShell({ children }: EventShellProps) {
  const navigate = useNavigate();
  const { loading, signOut, user } = useAuthSession();
  const { state } = useBackendSession();
  const isReady = state.status === "ready";
  const isCreator = isReady && state.account?.role === "CREATOR";

  async function handleSignOut() {
    await signOut();
    void navigate("/", { replace: true });
  }

  return (
    <div className="app-shell">
      <header className="top-bar">
        <Link to="/" className="brand" aria-label="MINIS event home">
          <img src="/minis-mark.svg" alt="" className="brand__mark" />
          <span>MINIS Event</span>
        </Link>
        <nav className="top-nav" aria-label="Primary navigation">
          <NavLink to="/">Feed</NavLink>
          <NavLink to="/creators">Creators</NavLink>
          {isReady ? <NavLink to="/saved">Saved</NavLink> : null}
          {isReady ? <NavLink to="/liked">Liked</NavLink> : null}
          {isCreator ? <NavLink to="/upload">Upload</NavLink> : null}
          {isCreator ? <NavLink to="/uploads">My Uploads</NavLink> : null}
          {isReady ? (
            <NavLink to="/account">Account</NavLink>
          ) : null}
          {state.status === "signed-out" ? (
            <>
              <NavLink to="/sign-in">Sign in</NavLink>
              <NavLink to="/sign-up">Sign up</NavLink>
            </>
          ) : null}
          {isReady && state.account?.role === "ADMIN" ? (
            <NavLink to="/admin">Admin</NavLink>
          ) : null}
          {user ? (
            <button
              type="button"
              onClick={() => {
                void handleSignOut();
              }}
              disabled={loading}
            >
              Sign out
            </button>
          ) : null}
        </nav>
      </header>
      <main className="main-surface">{children}</main>
    </div>
  );
}
