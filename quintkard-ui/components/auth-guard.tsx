"use client";

import { usePathname, useRouter } from "next/navigation";
import { PropsWithChildren, useEffect, useState } from "react";
import { getStoredAuth } from "../lib/auth";

export function AuthGuard({ children }: PropsWithChildren) {
  const router = useRouter();
  const pathname = usePathname();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const auth = getStoredAuth();
    if (!auth && pathname !== "/login") {
      router.replace("/login");
      return;
    }

    if (auth && pathname === "/login") {
      router.replace("/");
      return;
    }

    setReady(true);
  }, [pathname, router]);

  if (!ready) {
    return <main className="shell shell-center"><div className="panel">Loading...</div></main>;
  }

  return <>{children}</>;
}
