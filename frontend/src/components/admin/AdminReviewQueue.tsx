"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { Page, PendingReview, ReviewDecision } from "@/types/api";

/** Admin only: stories waiting for approval, oldest first. The API enforces the role. */
export function AdminReviewQueue() {
  const [data, setData] = useState<Page<PendingReview> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    api<Page<PendingReview>>("/admin/reviews?size=50", { signal: controller.signal })
      .then((result) => {
        setData(result);
        setError(null);
      })
      .catch((caught) => {
        if (!controller.signal.aborted) setError(errorMessage(caught));
      });
    return () => controller.abort();
  }, [reloadKey]);

  const reload = useCallback(() => setReloadKey((value) => value + 1), []);

  if (error) return <p className="notice notice-error">{error}</p>;
  if (!data) return <p className="text-chalk-dim">Loading stories waiting for review…</p>;
  if (data.content.length === 0) {
    return <p className="text-chalk-dim">Nothing waiting for review. New submissions also arrive by email.</p>;
  }

  return (
    <ul className="divide-y divide-line border-y border-line">
      {data.content.map((story) => (
        <li key={story.id}>
          <ReviewRow story={story} onDecided={reload} />
        </li>
      ))}
    </ul>
  );
}

function ReviewRow({ story, onDecided }: { story: PendingReview; onDecided: () => void }) {
  const [note, setNote] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function decide(approve: boolean) {
    if (!approve && !window.confirm("Reject this story? The author will see your note.")) return;
    setPending(true);
    setError(null);
    try {
      await api<ReviewDecision>(`/admin/reviews/${story.id}/${approve ? "approve" : "reject"}`, {
        method: "POST",
        body: { note: note.trim() || null },
      });
      onDecided();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <article className="grid gap-4 py-5 lg:grid-cols-[1fr_20rem]">
      <div className="min-w-0">
        <p className="text-sm text-chalk-dim">
          {story.club?.name ?? "General"}, submitted {formatDateTime(story.submittedAt)} by{" "}
          <span className="text-chalk">
            {story.author.displayName} (@{story.author.username})
          </span>
        </p>
        <h3 className="mt-1 display-medium text-2xl">
          <Link href={`/blogs/${story.slug}`}>{story.title}</Link>
        </h3>
        {story.excerpt && <p className="mt-1 text-[0.95rem] text-chalk-dim">{story.excerpt}</p>}
        <p className="mt-2 text-sm text-chalk-dim">Open the story to read it in full before deciding.</p>
      </div>
      <div className="flex flex-col gap-3">
        <label htmlFor={`review-note-${story.id}`} className="sr-only">
          Note for the author
        </label>
        <input
          id={`review-note-${story.id}`}
          className="input"
          placeholder="Note for the author (optional)"
          maxLength={500}
          value={note}
          onChange={(event) => setNote(event.target.value)}
        />
        <div className="flex flex-wrap gap-2">
          <button type="button" disabled={pending} onClick={() => decide(true)} className="btn btn-primary btn-small">
            Approve and publish
          </button>
          <button type="button" disabled={pending} onClick={() => decide(false)} className="btn btn-danger btn-small">
            Reject
          </button>
        </div>
        {error && <p className="field-error">{error}</p>}
      </div>
    </article>
  );
}
