"use client";

import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";

import { useSession } from "@/components/session/SessionProvider";
import { ApiError, errorMessage } from "@/lib/api";
import { loginHref } from "@/lib/navigation";

/**
 * Runs a signed-in action. Guests (or expired sessions answering 401) are sent to the
 * login page and brought back to `returnTo` afterwards. Tracks pending and error state.
 */
export function useAuthAction(returnTo: string) {
  const { me } = useSession();
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(
    async <T>(action: () => Promise<T>): Promise<T | undefined> => {
      if (me === null) {
        router.push(loginHref(returnTo));
        return undefined;
      }
      setPending(true);
      setError(null);
      try {
        return await action();
      } catch (caught) {
        if (caught instanceof ApiError && caught.status === 401) {
          router.push(loginHref(returnTo));
        } else {
          setError(errorMessage(caught));
        }
        return undefined;
      } finally {
        setPending(false);
      }
    },
    [me, returnTo, router],
  );

  return { run, pending, error, setError };
}
