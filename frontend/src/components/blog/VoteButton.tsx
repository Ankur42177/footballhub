"use client";

import { useEffect, useState } from "react";

import { useSession } from "@/components/session/SessionProvider";
import { api, errorMessage } from "@/lib/api";
import type { VoteResult } from "@/types/api";

/**
 * Upvote toggle, open to everyone. Signed-in users vote as themselves; other visitors are
 * remembered by a visitor cookie the API sets, so it's still one vote per person. The count
 * only changes once the server confirms.
 */
export function VoteButton({
  blogId,
  initialCount,
  initialVoted,
}: {
  blogId: number;
  initialCount: number;
  initialVoted: boolean;
}) {
  const { me } = useSession();
  const [count, setCount] = useState(initialCount);
  const [voted, setVoted] = useState(initialVoted);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Guests see a cached page, so ask the API for the live count and whether this visitor
  // already upvoted. Signed-in pages are rendered fresh and already know.
  useEffect(() => {
    if (me !== null) return;
    const controller = new AbortController();
    api<VoteResult>(`/blogs/${blogId}/vote`, { signal: controller.signal })
      .then((result) => {
        setCount(result.voteCount);
        setVoted(result.voted);
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, [blogId, me]);

  async function toggle() {
    setPending(true);
    setError(null);
    try {
      const result = await api<VoteResult>(`/blogs/${blogId}/vote`, { method: voted ? "DELETE" : "POST" });
      setCount(result.voteCount);
      setVoted(result.voted);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex flex-col items-start gap-1">
      <button
        type="button"
        onClick={toggle}
        disabled={pending}
        aria-pressed={voted}
        className={`group flex items-center gap-3 rounded-panel border px-4 py-2 transition-colors ${
          voted
            ? "border-floodlight bg-floodlight/10 text-floodlight"
            : "border-line-strong text-chalk hover:border-chalk-dim"
        } disabled:opacity-60`}
      >
        <svg viewBox="0 0 20 20" className="h-5 w-5" aria-hidden="true" focusable="false">
          <path d="M10 3 L18 13 H2 Z" fill={voted ? "currentColor" : "none"} stroke="currentColor" strokeWidth="1.8" />
        </svg>
        <span className="score text-3xl">{count}</span>
        <span className="text-sm">{voted ? "Upvoted" : "Upvote"}</span>
      </button>
      {error && (
        <p role="alert" className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}
