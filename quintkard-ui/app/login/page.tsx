"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { setStoredAuth, toBasicAuth } from "../../lib/auth";
import { apiFetch } from "../../lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");

    const authHeader = toBasicAuth(username, password);

    try {
      const response = await apiFetch({
        path: "/api/user",
        headers: {
          Authorization: authHeader
        }
      });

      if (!response.ok) {
        throw new Error("Invalid username or password.");
      }

      setStoredAuth(authHeader);
      router.replace("/");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to sign in.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="shell shell-center">
      <section className="login-card">
        <div className="eyebrow">Quintkard</div>
        <div className="hero-copy">
          <h1>Sign in</h1>
          <p>Use your persisted app user credentials to enter the workspace.</p>
        </div>

        <form className="stack" onSubmit={handleSubmit}>
          <label className="field">
            <span>Username</span>
            <input value={username} onChange={(event) => setUsername(event.target.value)} />
          </label>

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>

          <button className="button button-primary button-wide" disabled={loading} type="submit">
            {loading ? "Signing In..." : "Sign In"}
          </button>
        </form>

        {error ? <div className="message message-error">{error}</div> : null}
      </section>
    </main>
  );
}
