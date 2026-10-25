"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";

import { useAuthAction } from "@/components/session/useAuthAction";
import { api } from "@/lib/api";
import type { BlogDetail, BlogStatus } from "@/types/api";

/** Edit, publish, archive and delete controls shown to the author (and admins). */
export function ArticleOwnerActions({ blogId, slug, status }: { blogId: number; slug: string; status: BlogStatus }) {
  const router = useRouter();
  const { run, pending, error } = useAuthAction(`/blogs/${slug}`);

  async function changeStatus(action: "publish" | "archive") {
    const result = await run(() => api<BlogDetail>(`/blogs/${blogId}/${action}`, { method: "POST" }));
    if (result) router.refresh();
  }

  async function remove() {
    if (!window.confirm("Delete this article for good? Its votes, comments and bookmarks go with it.")) return;
    const removed = await run(async () => {
      await api(`/blogs/${blogId}`, { method: "DELETE" });
      return true;
    });
    if (removed) {
      router.push("/dashboard");
      router.refresh();
    }
  }

  return (
    <div className="notice flex flex-col gap-3">
      <p className="text-chalk-dim">
        {status === "DRAFT" && "This is a draft. Only you and the moderators can see it."}
        {status === "ARCHIVED" && "This article is archived. It's hidden from feeds and only you can open it."}
        {status === "PUBLISHED" && "You wrote this article."}
      </p>
      <div className="flex flex-wrap gap-2">
        <Link href={`/write?edit=${encodeURIComponent(slug)}`} className="btn btn-secondary btn-small">
          Edit
        </Link>
        {status !== "PUBLISHED" && (
          <button type="button" onClick={() => changeStatus("publish")} disabled={pending} className="btn btn-primary btn-small">
            Publish
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
