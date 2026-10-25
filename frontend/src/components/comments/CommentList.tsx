"use client";

import Link from "next/link";
import { useState } from "react";

import { CommentForm } from "@/components/comments/CommentForm";
import { ReportDialog } from "@/components/moderation/ReportDialog";
import { useSession } from "@/components/session/SessionProvider";
import { UserAvatar } from "@/components/ui/UserAvatar";
import { api, errorMessage } from "@/lib/api";
import { formatDateTime, plural } from "@/lib/format";
import { loginHref } from "@/lib/navigation";
import type { Comment, Page } from "@/types/api";

/**
 * The discussion under an article: one level of replies (replies to replies attach to
 * the top comment, as the API stores them). Starts from the server-rendered first page.
 */
export function CommentList({
  blogId,
  slug,
  initial,
  commentCount,
}: {
  blogId: number;
  slug: string;
  initial: Page<Comment>;
  commentCount: number;
}) {
  const { me } = useSession();
  const returnTo = `/blogs/${slug}#comments`;
  const [comments, setComments] = useState(initial.content);
  const [page, setPage] = useState(initial.page);
  const [hasNext, setHasNext] = useState(initial.hasNext);
  const [total, setTotal] = useState(commentCount);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  async function addComment(content: string, parentId: number | null) {
    const created = await api<Comment>(`/blogs/${blogId}/comments`, {
      method: "POST",
      body: { content, parentId },
    });
    setTotal((value) => value + 1);
    if (created.parentId === null) {
      setComments((list) => [{ ...created, replies: [] }, ...list]);
    } else {
      setComments((list) =>
        list.map((root) => (root.id === created.parentId ? { ...root, replies: [...root.replies, created] } : root)),
      );
    }
  }

  function replace(updated: Comment) {
    setComments((list) =>
      list.map((root) =>
        root.id === updated.id
          ? { ...updated, replies: root.replies }
          : { ...root, replies: root.replies.map((reply) => (reply.id === updated.id ? updated : reply)) },
      ),
    );
  }

  function markRemoved(id: number) {
    setTotal((value) => Math.max(0, value - 1));
    setComments((list) =>
      list
        .map((root) => ({ ...root, replies: root.replies.filter((reply) => reply.id !== id) }))
        .map((root) =>
          root.id === id ? { ...root, status: "REMOVED" as const, content: null, author: null, canEdit: false, canDelete: false } : root,
        )
        .filter((root) => root.status !== "REMOVED" || root.replies.length > 0),
    );
  }

  async function loadMore() {
    setLoadingMore(true);
    setLoadError(null);
    try {
      const next = await api<Page<Comment>>(`/blogs/${blogId}/comments?page=${page + 1}&size=20`);
      setComments((list) => {
        const seen = new Set(list.map((comment) => comment.id));
        return [...list, ...next.content.filter((comment) => !seen.has(comment.id))];
      });
      setPage(next.page);
      setHasNext(next.hasNext);
    } catch (caught) {
      setLoadError(errorMessage(caught));
    } finally {
      setLoadingMore(false);
    }
  }

  return (
    <section id="comments" aria-labelledby="comments-title" className="scroll-mt-6">
      <h2 id="comments-title" className="display-medium text-4xl">
        Discussion <span className="score text-chalk-dim">{total}</span>
      </h2>

      <div className="mt-5">
        {me ? (
          <CommentForm label="Add a comment" submitLabel="Post comment" onSubmit={(content) => addComment(content, null)} />
        ) : me === null ? (
          <p className="notice">
            <Link href={loginHref(returnTo)}>Log in</Link> or <Link href="/register">create an account</Link> to join
            the discussion.
          </p>
        ) : null}
      </div>

      {comments.length === 0 ? (
        <p className="mt-8 text-chalk-dim">No comments yet. Start the conversation.</p>
      ) : (
        <ol className="mt-8 flex flex-col gap-7">
          {comments.map((comment) => (
            <li key={comment.id}>
              <CommentItem
                comment={comment}
                returnTo={returnTo}
                signedIn={Boolean(me)}
                onReply={(content) => addComment(content, comment.id)}
                onUpdated={replace}
                onRemoved={markRemoved}
              />
              {comment.replies.length > 0 && (
                <ol className="mt-5 ml-5 flex flex-col gap-5 border-l border-line pl-5 sm:ml-12" aria-label={`Replies, ${plural(comment.replies.length, "reply", "replies")}`}>
                  {comment.replies.map((reply) => (
                    <li key={reply.id}>
                      <CommentItem
                        comment={reply}
                        returnTo={returnTo}
                        signedIn={Boolean(me)}
                        onReply={(content) => addComment(content, comment.id)}
                        onUpdated={replace}
                        onRemoved={markRemoved}
                      />
                    </li>
                  ))}
                </ol>
              )}
            </li>
          ))}
        </ol>
      )}

      {hasNext && (
        <div className="mt-8">
          <button type="button" onClick={loadMore} disabled={loadingMore} className="btn btn-secondary">
            {loadingMore ? "Loading…" : "Show older comments"}
          </button>
          {loadError && (
            <p role="alert" className="field-error">
              {loadError}
            </p>
          )}
        </div>
      )}
    </section>
  );
}

function CommentItem({
  comment,
  returnTo,
  signedIn,
  onReply,
  onUpdated,
  onRemoved,
}: {
  comment: Comment;
  returnTo: string;
  signedIn: boolean;
  onReply: (content: string) => Promise<void>;
  onUpdated: (comment: Comment) => void;
  onRemoved: (id: number) => void;
}) {
  const [mode, setMode] = useState<"view" | "reply" | "edit">("view");
  const [error, setError] = useState<string | null>(null);

  if (comment.status !== "VISIBLE" || !comment.author || comment.content === null) {
    return <p className="italic text-chalk-dim">This comment was removed.</p>;
  }

  async function remove() {
    if (!window.confirm("Delete your comment?")) return;
    setError(null);
    try {
      await api(`/comments/${comment.id}`, { method: "DELETE" });
      onRemoved(comment.id);
    } catch (caught) {
      setError(errorMessage(caught));
    }
  }

  return (
    <article className="flex gap-3">
      <UserAvatar name={comment.author.displayName} size={36} />
      <div className="min-w-0 flex-1">
        <p className="flex flex-wrap items-baseline gap-x-3 text-sm">
          <Link href={`/profile/${comment.author.username}`} className="font-semibold text-chalk no-underline hover:underline">
            {comment.author.displayName}
          </Link>
          <time dateTime={comment.createdAt} className="text-chalk-dim">
            {formatDateTime(comment.createdAt)}
          </time>
          {comment.edited && <span className="text-chalk-dim">edited</span>}
        </p>

        {mode === "edit" ? (
          <div className="mt-2">
            <CommentForm
              label="Edit your comment"
              submitLabel="Save"
              initialValue={comment.content}
              autoFocus
              onCancel={() => setMode("view")}
              onSubmit={async (content) => {
                const updated = await api<Comment>(`/comments/${comment.id}`, { method: "PUT", body: { content } });
                onUpdated({ ...updated, replies: comment.replies });
                setMode("view");
              }}
            />
          </div>
        ) : (
          <p className="mt-1 whitespace-pre-line break-words text-[1rem] leading-relaxed">{comment.content}</p>
        )}

        {mode !== "edit" && (
          <div className="mt-2 flex flex-wrap gap-4">
            {signedIn && (
              <button type="button" className="link-action" onClick={() => setMode(mode === "reply" ? "view" : "reply")}>
                Reply
              </button>
            )}
            {comment.canEdit && (
              <button type="button" className="link-action" onClick={() => setMode("edit")}>
                Edit
              </button>
            )}
            {comment.canDelete && (
              <button type="button" className="link-action" onClick={remove}>
                Delete
              </button>
            )}
            {signedIn && !comment.canEdit && <ReportDialog target={{ commentId: comment.id }} returnTo={returnTo} />}
          </div>
        )}
        {error && (
          <p role="alert" className="field-error">
            {error}
          </p>
        )}

        {mode === "reply" && (
          <div className="mt-3">
            <CommentForm
              label={`Reply to ${comment.author.displayName}`}
              submitLabel="Post reply"
              autoFocus
              onCancel={() => setMode("view")}
              onSubmit={async (content) => {
                await onReply(content);
                setMode("view");
              }}
            />
          </div>
        )}
      </div>
    </article>
  );
}
