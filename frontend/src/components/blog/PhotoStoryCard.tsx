import Link from "next/link";

import { ClubBadge } from "@/components/ui/ClubBadge";
import { clubTint } from "@/lib/clubs";
import type { BlogSummary } from "@/types/api";

/**
 * A story as a photo card: the article's own (credited, Wikimedia Commons) cover photo,
 * the club, the headline and a short summary. The whole card is one link.
 */
export function PhotoStoryCard({ blog }: { blog: BlogSummary & { cover: NonNullable<BlogSummary["cover"]> } }) {
  return (
    <article
      className="group relative flex h-full flex-col overflow-hidden rounded-panel border border-line bg-pitch-raised transition-colors hover:border-line-strong"
      style={{ borderTop: `3px solid ${clubTint(blog.club?.slug)}` }}
    >
      <div className="aspect-[4/3] overflow-hidden bg-pitch-deep">
        {/* Remote, already-sized Commons thumbnails (the credit is on the article page). */}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={blog.cover.url}
          alt={blog.cover.alt}
          loading="lazy"
          decoding="async"
          referrerPolicy="no-referrer"
          className="h-full w-full object-cover object-[50%_20%] transition-transform duration-300 group-hover:scale-[1.03] motion-reduce:transition-none motion-reduce:group-hover:scale-100"
        />
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <p className="flex items-center gap-1.5 text-sm font-semibold text-chalk-dim">
          {blog.club ? (
            <>
              <ClubBadge slug={blog.club.slug} size={15} />
              {blog.club.name}
            </>
          ) : (
            "General"
          )}
        </p>
        <h3 className="display-medium text-[1.45rem] leading-tight">
          <Link
            href={`/blogs/${blog.slug}`}
            className="no-underline after:absolute after:inset-0 after:content-[''] group-hover:text-floodlight"
          >
            {blog.title}
          </Link>
        </h3>
        {blog.excerpt && <p className="line-clamp-3 text-[0.95rem] text-chalk-dim">{blog.excerpt}</p>}
        <p className="mt-auto pt-2 text-sm text-chalk-dim">
          {blog.readingMinutes} min read, {blog.voteCount} {blog.voteCount === 1 ? "upvote" : "upvotes"}
        </p>
      </div>
    </article>
  );
}

export function hasCover(blog: BlogSummary): blog is BlogSummary & { cover: NonNullable<BlogSummary["cover"]> } {
  return Boolean(blog.cover?.url);
}
