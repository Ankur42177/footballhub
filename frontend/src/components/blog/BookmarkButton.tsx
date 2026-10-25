"use client";

import { useState } from "react";

import { useAuthAction } from "@/components/session/useAuthAction";
import { api } from "@/lib/api";
import type { BookmarkResult } from "@/types/api";

export function BookmarkButton({
  blogId,
  initialBookmarked,
  returnTo,
}: {
  blogId: number;
  initialBookmarked: boolean;
  returnTo: string;
}) {
  const [bookmarked, setBookmarked] = useState(initialBookmarked);
  const { run, pending, error } = useAuthAction(returnTo);

  async function toggle() {
    const result = await run(() =>
      api<BookmarkResult>(`/blogs/${blogId}/bookmark`, { method: bookmarked ? "DELETE" : "POST" }),
    );
    if (result) setBookmarked(result.bookmarked);
  }

  return (
    <div className="flex flex-col items-start gap-1">
      <button
        type="button"
        onClick={toggle}
        disabled={pending}
        aria-pressed={bookmarked}
        className={`btn btn-small ${bookmarked ? "btn-primary" : "btn-secondary"}`}
      >
        <svg viewBox="0 0 16 20" className="h-4 w-4" aria-hidden="true" focusable="false">
          <path
            d="M2 1.5 H14 V18.5 L8 14 L2 18.5 Z"
            fill={bookmarked ? "currentColor" : "none"}
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
          />
        </svg>
        {bookmarked ? "Saved" : "Save for later"}
      </button>
      {error && (
        <p role="alert" className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}
