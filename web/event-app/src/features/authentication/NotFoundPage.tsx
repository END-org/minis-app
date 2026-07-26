import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="auth-panel" aria-labelledby="not-found-title">
      <p className="section-kicker">Not found</p>
      <h1 id="not-found-title">That route is not here</h1>
      <p>The event shell can recover from this route without leaving the app.</p>
      <p className="muted-link">
        <Link to="/">Return home</Link>
      </p>
    </section>
  );
}
