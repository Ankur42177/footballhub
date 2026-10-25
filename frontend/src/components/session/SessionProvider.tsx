"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";

import { api, ApiError } from "@/lib/api";
import type { Me } from "@/types/api";

/**
 * Who is signed in, for client-side UI decisions only (showing "Write", the admin link,
 * login prompts). Every permission is enforced again by the API.
 * `me` is undefined while loading, null when signed out.
 */
interface SessionValue {
  me: Me | null | undefined;
  setMe: (me: Me | null) => void;
  refresh: () => Promise<void>;
}

const SessionContext = createContext<SessionValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null | undefined>(undefined);

  const refresh = useCallback(async () => {
    try {
      setMe(await api<Me>("/users/me"));
    } catch (error) {
      if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
        setMe(null);
      } else {
        setMe(null);
      }
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    api<Me>("/users/me")
      .then((user) => {
        if (!cancelled) setMe(user);
      })
      .catch(() => {
        if (!cancelled) setMe(null);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo(() => ({ me, setMe, refresh }), [me, refresh]);
  return <SessionContext value={value}>{children}</SessionContext>;
}

export function useSession(): SessionValue {
  const value = useContext(SessionContext);
  if (!value) throw new Error("useSession must be used inside <SessionProvider>");
  return value;
}

export function isStaff(me: Me | null | undefined): boolean {
  return me?.role === "MODERATOR" || me?.role === "ADMIN";
}
