"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { Page, Report, ReportStatus } from "@/types/api";

const FILTERS: { status: ReportStatus; label: string }[] = [
  { status: "OPEN", label: "Open" },
  { status: "REVIEWING", label: "Reviewing" },
  { status: "RESOLVED", label: "Resolved" },
  { status: "DISMISSED", label: "Dismissed" },
];

/** The reports queue. Moderators and admins only; the API enforces it. */
export function AdminModerationTable() {
  const [status, setStatus] = useState<ReportStatus>("OPEN");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Report> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    api<Page<Report>>(`/admin/reports?status=${status}&page=${page}&size=20`, { signal: controller.signal })
      .then((result) => {
        setData(result);
        setError(null);
      })
      .catch((caught) => {
        if (!controller.signal.aborted) setError(errorMessage(caught));
      });
    return () => controller.abort();
  }, [status, page, reloadKey]);

  const reload = useCallback(() => setReloadKey((value) => value + 1), []);

  return (
    <div>
      <div role="group" aria-label="Filter reports" className="flex flex-wrap gap-2">
        {FILTERS.map((filter) => (
          <button
            key={filter.status}
            type="button"
            aria-pressed={status === filter.status}
            onClick={() => {
              setStatus(filter.status);
              setPage(0);
              setData(null);
            }}
            className={`btn btn-small ${status === filter.status ? "btn-primary" : "btn-secondary"}`}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {error && <p className="notice notice-error mt-5">{error}</p>}
      {!data && !error && <p className="mt-5 text-chalk-dim">Loading reports…</p>}
      {data && data.content.length === 0 && (
        <p className="mt-6 text-chalk-dim">No {status.toLowerCase()} reports. The pitch is clear.</p>
      )}

      {data && data.content.length > 0 && (
        <ul className="mt-5 divide-y divide-line border-y border-line">
          {data.content.map((report) => (
            <li key={report.id}>
              <ReportRow report={report} onChanged={reload} />
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-5 flex items-center justify-between">
          <button type="button" className="btn btn-secondary btn-small" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </button>
          <span className="text-sm text-chalk-dim">
            Page {page + 1} of {data.totalPages}
          </span>
          <button type="button" className="btn btn-secondary btn-small" disabled={!data.hasNext} onClick={() => setPage(page + 1)}>
            Next
          </button>
        </div>
      )}
    </div>
  );
}

function ReportRow({ report, onChanged }: { report: Report; onChanged: () => void }) {
  const [note, setNote] = useState("");
  const [hideContent, setHideContent] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { target } = report;
  const isComment = target.type === "COMMENT";
  const contentHidden = isComment ? target.commentStatus === "HIDDEN" : target.blogHidden;
  const closed = report.status === "RESOLVED" || report.status === "DISMISSED";

  async function act(request: () => Promise<unknown>) {
    setPending(true);
    setError(null);
    try {
      await request();
      onChanged();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  function resolve(status: ReportStatus) {
    return act(() =>
      api(`/admin/reports/${report.id}/resolve`, {
        method: "POST",
        body: { status, note: note.trim() || null, hideContent: status === "RESOLVED" && hideContent },
      }),
    );
  }

  function toggleHidden() {
    const path = isComment ? `/admin/comments/${target.commentId}` : `/admin/blogs/${target.blogId}`;
    return act(() => api(`${path}/${contentHidden ? "unhide" : "hide"}`, { method: "POST" }));
  }

  return (
    <article className="grid gap-4 py-5 lg:grid-cols-[1fr_20rem]">
      <div className="min-w-0">
        <p className="flex flex-wrap items-center gap-2 text-sm text-chalk-dim">
          <span className="tag">{isComment ? "Comment" : "Article"}</span>
          {contentHidden && <span className="tag tag-danger">Hidden</span>}
          {isComment && target.commentStatus === "REMOVED" && <span className="tag">Removed by author</span>}
          <span>
            Reported by @{report.reporter} on {formatDateTime(report.createdAt)}
          </span>
        </p>
        <h3 className="mt-2 font-semibold">
          <Link href={`/blogs/${target.blogSlug}${isComment ? "#comments" : ""}`}>{target.blogTitle}</Link>
          <span className="font-normal text-chalk-dim"> by @{target.contentAuthor}</span>
        </h3>
        {isComment && target.commentExcerpt && (
          <blockquote className="mt-2 border-l-2 border-line-strong pl-3 text-[0.95rem] text-chalk-dim">
            {target.commentExcerpt}
          </blockquote>
        )}
        <p className="mt-3">
          <span className="text-chalk-dim">Reason: </span>
          {report.reason}
        </p>
        {closed && (
          <p className="mt-2 text-sm text-chalk-dim">
            {report.status === "RESOLVED" ? "Resolved" : "Dismissed"} by @{report.resolvedBy}{" "}
            {formatDateTime(report.resolvedAt)}
            {report.resolutionNote ? `: ${report.resolutionNote}` : ""}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-3">
        {!closed && (
          <>
            <label className="sr-only" htmlFor={`note-${report.id}`}>
              Note for the audit log
            </label>
            <input
              id={`note-${report.id}`}
              className="input"
              placeholder="Note for the audit log (optional)"
              maxLength={500}
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={hideContent} onChange={(event) => setHideContent(event.target.checked)} />
              Hide the {isComment ? "comment" : "article"} when resolving
            </label>
            <div className="flex flex-wrap gap-2">
              <button type="button" disabled={pending} onClick={() => resolve("RESOLVED")} className="btn btn-primary btn-small">
                Resolve
              </button>
              <button type="button" disabled={pending} onClick={() => resolve("DISMISSED")} className="btn btn-secondary btn-small">
                Dismiss
              </button>
              {report.status === "OPEN" && (
                <button type="button" disabled={pending} onClick={() => resolve("REVIEWING")} className="btn btn-secondary btn-small">
                  Mark reviewing
                </button>
              )}
            </div>
          </>
        )}
        {!(isComment && target.commentStatus === "REMOVED") && (
          <button type="button" disabled={pending} onClick={toggleHidden} className="link-action self-start">
            {contentHidden ? "Make visible again" : `Hide ${isComment ? "comment" : "article"} now`}
          </button>
        )}
        {error && <p className="field-error">{error}</p>}
      </div>
    </article>
  );
}
