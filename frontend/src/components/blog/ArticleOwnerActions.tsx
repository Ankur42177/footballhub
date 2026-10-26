"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";

import { useSession } from "@/components/session/SessionProvider";
import { useAuthAction } from "@/components/session/useAuthAction";
import { api } from "@/lib/api";
import type { BlogDetail, BlogStatus } from "@/types/api";

/**
 * Controls for the author and for admins. Only admins publish directly; other authors
 * submit for review (the API enforces this, the labels just explain it).
 */
export function ArticleOwnerActions({
  blogId,
  slug,
  status,
  authorUsername,
  reviewNote,
}: {
  blogId: number;
  slug: string;
  status: BlogStatus;
  authorUsername: string;
  reviewNote: string | null;
}) {
  const router = useRouter();
  const { me } = useSession();
  const isAuthor = !me || me.username === authorUsername;
  const isAdmin = me?.role === "ADMIN";
  const { run, pending, error } = useAuthAction(`/blogs/${slug}`);

  async function changeStatus(action: "publish" | "archive") {
    const result = await run(() => api<BlogDetail>(`/blogs/${blogId}/${action}`, { method: "POST" }));
    if (result) router.refresh();
  }

  async function remove() {
    const whose = isAuthor ? "this article" : `@${authorUsername}'s article`;
    if (!window.confirm(`Delete ${whose} for good? Its votes, comments and bookmarks go with it.`)) return;
    const removed = await run(async () => {
      await api(`/blogs/${blogId}`, { method: "DELETE" });
      return true;
    });
    if (removed) {
      router.push(isAdmin && !isAuthor ? "/admin" : "/dashboard");
      router.refresh();
    }
  }

  const message: Record<BlogStatus, string> = {
    DRAFT: isAuthor ? "This is a draft. Only you can see it." : "Draft by another author (admin view).",
    PENDING_REVIEW: isAdmin
      ? "This story is waiting for review. Approve it here or from the admin page."
      : "Sent for review. It will appear on the site once an editor approves it.",
    PUBLISHED: isAuthor ? "You wrote this article." : "Admin controls for this article.",
    REJECTED: "This story wasn't approved." + (reviewNote ? ` Editor's note: ${reviewNote}` : "") + " Edit it and submit again.",
    ARCHIVED: isAuthor ? "This article is archived. It's hidden from feeds." : "Archived by its author (admin view).",
  };
  const canEdit = isAdmin || status !== "PENDING_REVIEW";
  const publishLabel = isAdmin ? (status === "PENDING_REVIEW" ? "Approve and publish" : "Publish") : "Submit for review";

  return (
    <div className={`notice flex flex-col gap-3 ${status === "REJECTED" ? "notice-error" : ""}`}>
      <p className="text-chalk-dim">{message[status]}</p>
      <div className="flex flex-wrap gap-2">
        {canEdit && (
          <Link href={`/write?edit=${encodeURIComponent(slug)}`} className="btn btn-secondary btn-small">
            Edit
          </Link>
        )}
        {status !== "PUBLISHED" && (status !== "PENDING_REVIEW" || isAdmin) && (
          <button type="button" onClick={() => changeStatus("publish")} disabled={pending} className="btn btn-primary btn-small">
            {publishLabel}
          </button>
        )}
        {status === "PUBLISHED" && (
          <button type="button" onClick={() => changeStatus("archive")} disabled={pending} className="btn btn-secondary btn-small">
            Archive
          </button>
        )}
        <button type="button" onClick={remove} disabled={pending} className="btn btn-danger btn-small">
          Delete
        </button>
      </div>
      {error && (
        <p role="alert" className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}
