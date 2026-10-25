"use client";

import { useState } from "react";

import { useAuthAction } from "@/components/session/useAuthAction";
import { api } from "@/lib/api";
import type { VoteResult } from "@/types/api";

/** Upvote toggle. The count only changes once the server confirms. */
export function VoteButton({
  blogId,
  initialCount,
  initialVoted,
  returnTo,
}: {
  blogId: number;
  initialCount: number;
  initialVoted: boolean;
  returnTo: string;
}) {
  const [count, setCount] = useState(initialCount);
  const [voted, setVoted] = useState(initialVoted);
  const { run, pending, error } = useAuthAction(returnTo);

  async function toggle() {
    const result = await run(() =>
      api<VoteResult>(`/blogs/${blogId}/vote`, { method: voted ? "DELETE" : "POST" }),
    );
    if (result) {
      setCount(result.voteCount);
      setVoted(result.voted);
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
