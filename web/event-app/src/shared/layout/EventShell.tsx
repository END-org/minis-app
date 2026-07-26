import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuthSession } from "../../core/auth/AuthSessionContext";
import type { ReactNode } from "react";

interface EventShellProps {
  readonly children: ReactNode;
}

export function EventShell({ children }: EventShellProps) {
  const navigate = useNavigate();
  const { loading, signOut, user } = useAuthSession();

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
          <NavLink to="/">Guest</NavLink>
          {user ? <NavLink to="/account">Account</NavLink> : <NavLink to="/sign-in">Sign in</NavLink>}
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
