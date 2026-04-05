"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "../../components/app-shell";
import { AuthGuard } from "../../components/auth-guard";
import { getStoredAuth } from "../../lib/auth";
import { apiFetch } from "../../lib/api";
import type { UserResponse } from "../../lib/types";

export default function AccountPage() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [redactionEnabled, setRedactionEnabled] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadUser() {
    const authHeader = getStoredAuth();
    if (!authHeader) {
      return;
    }

    setLoading(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: "/api/user",
        headers: {
          Authorization: authHeader
        }
      });

      if (!response.ok) {
        throw new Error(response.status === 401 ? "Authentication failed." : "Unable to load profile.");
      }

      const data: UserResponse = await response.json();
      setUser(data);
      setDisplayName(data.displayName);
      setEmail(data.email);
      setRedactionEnabled(data.redactionEnabled);
      setNotice("Profile loaded.");
    } catch (requestError) {
      setUser(null);
      setError(requestError instanceof Error ? requestError.message : "Unable to load profile.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadUser();
  }, []);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const authHeader = getStoredAuth();

    if (!authHeader) {
      setError("Sign in first.");
      return;
    }

    setSaving(true);
    setError("");
    setNotice("");

    try {
      const response = await apiFetch({
        path: "/api/user",
        method: "PUT",
        headers: {
          Authorization: authHeader,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          displayName,
          email,
          redactionEnabled
        })
      });

      if (!response.ok) {
        throw new Error(response.status === 401 ? "Authentication failed." : "Unable to save profile.");
      }

      const data: UserResponse = await response.json();
      setUser(data);
      setDisplayName(data.displayName);
      setEmail(data.email);
      setRedactionEnabled(data.redactionEnabled);
      setNotice("Profile updated.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to save profile.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AuthGuard>
      <AppShell
        title="Account"
        subtitle="Manage your profile and workspace-level user preferences."
      >
        <section className="grid">
          <article className="panel">
            <div className="panel-header">
              <span className="panel-kicker">Current User</span>
              <span className="panel-badge">{loading ? "Loading" : "Active"}</span>
            </div>

            <div className="stack">
              <div className="list-card">
                <div className="list-card-top">
                  <span>User ID</span>
                  <strong>{user?.userId ?? "Not loaded"}</strong>
                </div>
                <p>Authentication is resolved directly from the Spring Security context.</p>
              </div>
              <div className="list-card">
                <div className="list-card-top">
                  <span>Email</span>
                  <strong>{user?.email ?? "Not loaded"}</strong>
                </div>
                <p>Profile data is read from the persisted user record in PostgreSQL.</p>
              </div>
              <div className="list-card">
                <div className="list-card-top">
                  <span>Workspace</span>
                  <strong>Ready</strong>
                </div>
                <p>Cards are now the default landing zone for daily work.</p>
              </div>
            </div>
          </article>

          <article className="panel panel-feature">
            <div className="panel-header">
              <span className="panel-kicker">Profile</span>
              <span className="panel-badge">/api/user</span>
            </div>

            <form className="profile-form" onSubmit={handleSave}>
              <div className="form-grid">
                <label className="field">
                  <span>Display name</span>
                  <input
                    value={displayName}
                    onChange={(event) => setDisplayName(event.target.value)}
                  />
                </label>

                <label className="field">
                  <span>Email</span>
                  <input
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                  />
                </label>
              </div>

              <label className="toggle">
                <input
                  checked={redactionEnabled}
                  onChange={(event) => setRedactionEnabled(event.target.checked)}
                  type="checkbox"
                />
                <span>Enable redaction for this user</span>
              </label>

              <div className="hero-actions">
                <button className="button button-primary" disabled={!user || saving} type="submit">
                  {saving ? "Saving..." : "Save Changes"}
                </button>
                <a className="button" href="/">
                  Open Cards
                </a>
              </div>
            </form>

            {(error || notice) && (
              <div className={`message ${error ? "message-error" : "message-success"}`}>
                {error || notice}
              </div>
            )}
          </article>
        </section>
      </AppShell>
    </AuthGuard>
  );
}
