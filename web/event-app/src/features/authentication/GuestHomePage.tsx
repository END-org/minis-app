import { Link } from "react-router-dom";
import { useAuthSession } from "../../core/auth/AuthSessionContext";

export function GuestHomePage() {
  const { user } = useAuthSession();

  return (
    <section className="home-view" aria-labelledby="home-title">
      <p className="section-kicker">Guest mode</p>
      <h1 id="home-title">MINIS Event</h1>
      <p className="lead">A quiet event shell for prepared accounts and guest access.</p>
      <div className="action-row">
        {user ? (
          <Link className="button-link" to="/account">
            View account
          </Link>
        ) : (
          <Link className="button-link" to="/sign-in">
            Sign in
          </Link>
        )}
        <span className="guest-pill">Guest access active</span>
      </div>
    </section>
  );
}
