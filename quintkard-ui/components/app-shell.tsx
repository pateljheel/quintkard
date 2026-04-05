"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { PropsWithChildren } from "react";
import { clearStoredAuth } from "../lib/auth";

type AppShellProps = PropsWithChildren<{
  title: string;
  subtitle: string;
}>;

export function AppShell({ title, subtitle, children }: AppShellProps) {
  const pathname = usePathname();
  const router = useRouter();

  function handleSignOut() {
    clearStoredAuth();
    router.replace("/login");
  }

  return (
    <main className="shell shell-app">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">Q</div>
          <div>
            <strong>Quintkard</strong>
            <p>Operations Console</p>
          </div>
        </div>

        <nav className="nav">
          <Link className={pathname === "/" ? "nav-link nav-link-active" : "nav-link"} href="/">
            Cards
          </Link>
          <Link
            className={pathname === "/account" ? "nav-link nav-link-active" : "nav-link"}
            href="/account"
          >
            Account
          </Link>
          <Link
            className={pathname === "/agents" ? "nav-link nav-link-active" : "nav-link"}
            href="/agents"
          >
            Agents
          </Link>
          <Link
            className={pathname === "/orchestrator" ? "nav-link nav-link-active" : "nav-link"}
            href="/orchestrator"
          >
            Orchestrator
          </Link>
          <Link
            className={pathname === "/messages" ? "nav-link nav-link-active" : "nav-link"}
            href="/messages"
          >
            Messages
          </Link>
        </nav>

        <button className="button sidebar-button" onClick={handleSignOut} type="button">
          Sign Out
        </button>
      </aside>

      <section className="content">
        <header className="page-header">
          <div>
            <div className="eyebrow">Workspace</div>
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>
        </header>
        {children}
      </section>
    </main>
  );
}
