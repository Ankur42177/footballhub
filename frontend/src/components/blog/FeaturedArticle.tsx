import Link from "next/link";
import type { CSSProperties } from "react";

import { clubColor } from "@/lib/clubs";
import { formatDate, plural } from "@/lib/format";
import type { BlogSummary } from "@/types/api";

/** Chalk markings of a full pitch (105 x 68 m, 10 units per metre). */
function PitchLines() {
  return (
    <svg
      viewBox="0 0 1050 680"
      preserveAspectRatio="xMidYMid slice"
      className="pitch-lines absolute inset-0 h-full w-full"
      aria-hidden="true"
      focusable="false"
    >
      <g fill="none" stroke="currentColor" strokeWidth="3">
        <rect x="10" y="10" width="1030" height="660" />
        <line x1="525" y1="10" x2="525" y2="670" />
        <circle cx="525" cy="340" r="91.5" />
        {/* Left end */}
        <rect x="10" y="138.5" width="165" height="403" />
        <rect x="10" y="248.5" width="55" height="183" />
        <path d="M 175 266.9 A 91.5 91.5 0 0 1 175 413.1" />
        {/* Right end */}
        <rect x="875" y="138.5" width="165" height="403" />
        <rect x="985" y="248.5" width="55" height="183" />
        <path d="M 875 266.9 A 91.5 91.5 0 0 0 875 413.1" />
      </g>
      <g fill="currentColor">
        <circle cx="525" cy="340" r="5" />
        <circle cx="120" cy="340" r="5" />
        <circle cx="930" cy="340" r="5" />
      </g>
    </svg>
  );
}

/** The lead story: the one loud element on the homepage. */
export function FeaturedArticle({ blog }: { blog: BlogSummary }) {
  const kit = { "--kit": clubColor(blog.club?.slug, blog.club?.primaryColor) } as CSSProperties;

  return (
    <article className="relative overflow-hidden border-b border-line bg-pitch-deep" style={kit}>
      <PitchLines />
      <div className="relative mx-auto max-w-6xl px-4 py-14 sm:px-6 sm:py-20">
        <div className="kit-stripe max-w-4xl pl-5 sm:pl-7">
          <p className="text-[0.95rem] text-chalk-dim">
            Lead story
            {blog.club && (
              <>
                {" from "}
                <Link href={`/clubs/${blog.club.slug}`} className="text-chalk">
                  {blog.club.name}
                </Link>
              </>
            )}
          </p>
          <h2 className="display lead-enter mt-3 text-[2.9rem] sm:text-[4.4rem] lg:text-[5.4rem]">
            <Link href={`/blogs/${blog.slug}`} className="no-underline hover:text-floodlight">
              {blog.title}
            </Link>
          </h2>
          {blog.excerpt && (
            <p className="mt-5 max-w-2xl text-lg leading-relaxed text-chalk/85 sm:text-xl">{blog.excerpt}</p>
          )}
          <div className="mt-7 flex flex-wrap items-center gap-x-6 gap-y-3">
            <Link href={`/blogs/${blog.slug}`} className="btn btn-primary">
              Read the article
            </Link>
            <p className="text-[0.95rem] text-chalk-dim">
              By{" "}
              <Link href={`/profile/${blog.author.username}`} className="text-chalk">
                {blog.author.displayName}
              </Link>
              {blog.publishedAt && (
                <>
                  {", "}
                  <time dateTime={blog.publishedAt}>{formatDate(blog.publishedAt)}</time>
                </>
              )}
            </p>
            <p className="flex items-baseline gap-2 text-chalk-dim">
              <span className="score text-3xl text-floodlight">{blog.voteCount}</span>
              <span className="text-sm">
                {blog.voteCount === 1 ? "upvote" : "upvotes"}, {plural(blog.commentCount, "comment")}
              </span>
            </p>
          </div>
        </div>
      </div>
    </article>
  );
}
