"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";

import { Markdown } from "@/components/blog/Markdown";
import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { ReviewDecision, ReviewPreview } from "@/types/api";

/**
 * Opened from the review email. The single-use token is in the URL fragment (#token=...),
 * which browsers never send to servers, so it doesn't appear in any request log. It is sent
 * to the API only in a POST body, and removed from the address bar straight away.
 */
export function ReviewFromLink() {
  const token = useRef<string | null>(null);
  const [story, setStory] = useState<ReviewPreview | null>(null);
  const [state, setState] = useState<"loading" | "ready" | "invalid" | "done">("loading");
  const [message, setMessage] = useState<string | null>(null);
  const [note, setNote] = useState("");
  const [pending, setPending] = useState(false);
  const [result, setResult] = useState<ReviewDecision | null>(null);

  useEffect(() => {
    const value = new URLSearchParams(window.location.hash.slice(1)).get("token");
    window.history.replaceState(null, "", window.location.pathname);
    token.current = value;
    const preview = value
      ? api<ReviewPreview>("/reviews/preview", { method: "POST", body: { token: value } })
      : Promise.reject(new Error("missing token"));
    preview
      .then((loaded) => {
        setStory(loaded);
        setState("ready");
      })
      .catch((caught) => {
        setState("invalid");
        setMessage(value ? errorMessage(caught) : "This page needs the link from the review email.");
      });
  }, []);

  async function decide(approve: boolean) {
    if (!token.current) return;
    if (!approve && !window.confirm("Reject this story? The author will see your note.")) return;
    setPending(true);
    setMessage(null);
    try {
      const decision = await api<ReviewDecision>(`/reviews/${approve ? "approve" : "reject"}`, {
        method: "POST",
        body: { token: token.current, note: note.trim() || null },
      });
      setResult(decision);
      setState("done");
    } catch (caught) {
      setMessage(errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  if (state === "loading") return <p role="status">Checking the review link…</p>;
  if (state === "invalid") {
    return (
      <p className="notice notice-error" role="alert">
        {message} Stories can also be reviewed on the <Link href="/admin?tab=reviews">admin page</Link>.
      </p>
    );
  }
  if (state === "done" && result) {
    return result.status === "PUBLISHED" ? (
      <p className="notice notice-success" role="status">
        Approved. The story is live: <Link href={`/blogs/${result.slug}`}>open it</Link>.
      </p>
    ) : (
      <p className="notice" role="status">
        Rejected. The author has been notified{note.trim() ? " with your note" : ""}.
      </p>
    );
  }
  if (!story) return null;

  return (
    <div>
      <div className="notice">
        <p>
          Submitted by <strong>{story.author.displayName}</strong> (@{story.author.username})
          {story.club ? ` about ${story.club.name}` : ""} on {formatDateTime(story.submittedAt)}. This link works once
          and expires on {formatDateTime(story.linkExpiresAt)}.
        </p>
      </div>
      <article className="mt-8 border-y border-line py-8">
        <h2 className="display text-4xl sm:text-5xl">{story.title}</h2>
        {story.excerpt && <p className="mt-3 text-lg text-chalk/85">{story.excerpt}</p>}
        <div className="mt-6">
          <Markdown source={story.content} />
        </div>
      </article>
      <div className="mt-6 flex flex-col gap-3">
        <label htmlFor="review-note" className="label">
          Note for the author <span className="font-normal text-chalk-dim">(optional, shown if you reject)</span>
        </label>
        <textarea
          id="review-note"
          className="input"
          rows={3}
          maxLength={500}
          value={note}
          onChange={(event) => setNote(event.target.value)}
        />
        <div className="flex flex-wrap gap-3">
          <button type="button" disabled={pending} onClick={() => decide(true)} className="btn btn-primary">
            Approve and publish
          </button>
          <button type="button" disabled={pending} onClick={() => decide(false)} className="btn btn-danger">
            Reject
          </button>
        </div>
        {message && (
          <p role="alert" className="field-error">
            {message}
          </p>
        )}
      </div>
    </div>
  );
}
