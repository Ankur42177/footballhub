"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { useSession } from "@/components/session/SessionProvider";
import { api, errorMessage } from "@/lib/api";
import type { Me } from "@/types/api";

interface GoogleIdApi {
  initialize(options: {
    client_id: string;
    nonce: string;
    callback: (response: { credential?: string }) => void;
    ux_mode?: "popup" | "redirect";
    auto_select?: boolean;
    itp_support?: boolean;
  }): void;
  renderButton(parent: HTMLElement, options: Record<string, string | number>): void;
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleIdApi } };
  }
}

const SCRIPT_SRC = "https://accounts.google.com/gsi/client";

function loadGoogleScript(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${SCRIPT_SRC}"]`);
    const script = existing ?? Object.assign(document.createElement("script"), { src: SCRIPT_SRC, async: true });
    script.addEventListener("load", () => resolve(), { once: true });
    script.addEventListener("error", () => reject(new Error("Google sign-in couldn't load")), { once: true });
    if (!existing) document.head.appendChild(script);
  });
}

/**
 * Google's official sign-in button. The API hands out a one-time nonce for this browser
 * session; Google signs it into the ID token, and the API verifies the token's signature,
 * issuer, audience, expiry, nonce and verified email before starting a session. Renders
 * nothing when Google sign-in isn't configured.
 */
export function GoogleSignIn({ next, label = "continue_with" }: { next: string; label?: "continue_with" | "signup_with" }) {
  const router = useRouter();
  const { setMe } = useSession();
  const container = useRef<HTMLDivElement>(null);
  const [available, setAvailable] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api<{ clientId: string; nonce: string }>("/auth/google/start")
      .then(async ({ clientId, nonce }) => {
        await loadGoogleScript();
        if (cancelled || !container.current || !window.google) return;
        window.google.accounts.id.initialize({
          client_id: clientId,
          nonce,
          ux_mode: "popup",
          auto_select: false,
          itp_support: true,
          callback: async ({ credential }) => {
            if (!credential) return;
            setError(null);
            try {
              const me = await api<Me>("/auth/google", { method: "POST", body: { credential } });
              setMe(me);
              router.replace(next);
              router.refresh();
            } catch (caught) {
              setError(errorMessage(caught));
            }
          },
        });
        window.google.accounts.id.renderButton(container.current, {
          type: "standard",
          theme: "filled_black",
          size: "large",
          shape: "pill",
          text: label,
          width: Math.min(container.current.offsetWidth || 320, 400),
        });
        setAvailable(true);
      })
      .catch(() => {
        // Not configured (404) or Google's script blocked: the email form still works.
      });
    return () => {
      cancelled = true;
    };
  }, [label, next, router, setMe]);

  return (
    <div className={available ? "flex flex-col gap-3" : "hidden"}>
      <div ref={container} className="min-h-[44px] w-full" />
      {error && (
        <p role="alert" className="field-error">
          {error}
        </p>
      )}
      <p className="flex items-center gap-3 text-sm text-chalk-dim" aria-hidden="true">
        <span className="h-px flex-1 bg-line" />
        or use your email
        <span className="h-px flex-1 bg-line" />
      </p>
    </div>
  );
}
