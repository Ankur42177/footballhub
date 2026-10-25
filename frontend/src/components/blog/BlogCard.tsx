import Link from "next/link";
import type { CSSProperties } from "react";

import { clubColor } from "@/lib/clubs";
import { formatDate, plural } from "@/lib/format";
import type { BlogSummary } from "@/types/api";

/**
 * One row in a feed: club stripe on the left, headline and byline in the middle,
 * the upvote total on the right like a scoreboard figure.
 */
export function BlogCard({ blog, showStatus = false }: { blog: BlogSummary; showStatus?: boolean }) {
  const kit = { "--kit": clubColor(blog.club?.slug, blog.club?.primaryColor) } as CSSProperties;
  const date = blog.publishedAt ?? blog.updatedAt;

  return (
    <article className="kit-stripe grid grid-cols-[1fr_auto] gap-x-5 gap-y-2 py-5 pl-5" style={kit}>
      <div className="min-w-0">
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-chalk-dim">
          {blog.club ? (
            <Link href={`/clubs/${blog.club.slug}`} className="font-semibold text-chalk no-underline hover:underline">
              {blog.club.name}
            </Link>
          ) : (
            <span>General</span>
          )}
          {showStatus && blog.status !== "PUBLISHED" && (
            <span className={blog.status === "DRAFT" ? "tag tag-warn" : "tag"}>
              {blog.status === "DRAFT" ? "Draft" : "Archived"}
            </span>
          )}
          {blog.hidden && <span className="tag tag-danger">Hidden by moderators</span>}
        </p>

        <h3 className="display-medium mt-1.5 text-[1.65rem] sm:text-[1.9rem]">
          <Link href={`/blogs/${blog.slug}`} className="no-underline hover:text-floodlight">
            {blog.title}
          </Link>
        </h3>

        {blog.excerpt && <p className="mt-2 line-clamp-3 max-w-2xl text-[1rem] text-chalk-dim">{blog.excerpt}</p>}

        <p className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-chalk-dim">
          <Link href={`/profile/${blog.author.username}`} className="text-chalk no-underline hover:underline">
            {blog.author.displayName}
          </Link>
          {date && <time dateTime={date}>{formatDate(date)}</time>}
          <span>{blog.readingMinutes} min read</span>
          <span>{plural(blog.commentCount, "comment")}</span>
        </p>
      </div>

      <div className="flex flex-col items-end pt-6 text-right" aria-label={plural(blog.voteCount, "upvote")}>
        <span className={`score text-4xl ${blog.votedByMe ? "text-floodlight" : "text-chalk"}`}>{blog.voteCount}</span>
        <span className="text-xs text-chalk-dim" aria-hidden="true">
          {blog.voteCount === 1 ? "upvote" : "upvotes"}
        </span>
      </div>
    </article>
  );
}
