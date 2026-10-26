import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import type { CSSProperties } from "react";

import { ArticleOwnerActions } from "@/components/blog/ArticleOwnerActions";
import { BookmarkButton } from "@/components/blog/BookmarkButton";
import { Markdown } from "@/components/blog/Markdown";
import { VoteButton } from "@/components/blog/VoteButton";
import { CommentList } from "@/components/comments/CommentList";
import { ReportDialog } from "@/components/moderation/ReportDialog";
import { ArticleSkeleton } from "@/components/ui/Skeletons";
import { ClubBadge } from "@/components/ui/ClubBadge";
import { UserAvatar } from "@/components/ui/UserAvatar";
import { clubColor, clubTint } from "@/lib/clubs";
import { formatDate, siteUrl } from "@/lib/format";
import { getBlog, getComments, getPublicBlog } from "@/lib/server-api";

type Params = PageProps<"/blogs/[slug]">["params"];

export async function generateMetadata({ params }: PageProps<"/blogs/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  // Anonymous read, so a draft's title never ends up in <head> for anyone.
  const blog = await getPublicBlog(slug).catch(() => null);
  if (!blog) return { title: "Article", robots: { index: false } };
  const description = blog.excerpt ?? `${blog.title}, by ${blog.author.displayName} on FootballHub.`;
  return {
    title: blog.title,
    description,
    alternates: { canonical: `/blogs/${blog.slug}` },
    authors: [{ name: blog.author.displayName, url: `/profile/${blog.author.username}` }],
    openGraph: {
      type: "article",
      title: blog.title,
      description,
      url: `/blogs/${blog.slug}`,
      publishedTime: blog.publishedAt ?? undefined,
      modifiedTime: blog.updatedAt,
      authors: [blog.author.displayName],
    },
  };
}

export default function BlogPage(props: PageProps<"/blogs/[slug]">) {
  return (
    <Suspense fallback={<ArticleSkeleton />}>
      <Article params={props.params} />
    </Suspense>
  );
}

async function Article({ params }: { params: Params }) {
  const { slug } = await params;
  const blog = await getBlog(slug);
  if (!blog) notFound();

  const live = blog.status === "PUBLISHED" && !blog.hidden;
  const comments = live ? await getComments(blog.id) : null;
  const returnTo = `/blogs/${blog.slug}`;
  const kit = { "--kit": clubColor(blog.club?.slug, blog.club?.primaryColor) } as CSSProperties;

  const jsonLd = live
    ? {
        "@context": "https://schema.org",
        "@type": "Article",
        headline: blog.title,
        description: blog.excerpt ?? undefined,
        datePublished: blog.publishedAt ?? undefined,
        dateModified: blog.updatedAt,
        author: { "@type": "Person", name: blog.author.displayName, url: `${siteUrl()}/profile/${blog.author.username}` },
        mainEntityOfPage: `${siteUrl()}/blogs/${blog.slug}`,
      }
    : null;

  return (
    <article className="mx-auto max-w-3xl px-4 py-10 sm:px-6 sm:py-14">
      {jsonLd && (
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd).replace(/</g, "\\u003c") }}
        />
      )}

      {blog.canEdit && (
        <div className="mb-8">
          <ArticleOwnerActions blogId={blog.id} slug={blog.slug} status={blog.status} authorUsername={blog.author.username} />
        </div>
      )}
      {blog.hidden && (
        <p className="notice notice-error mb-8">Moderators have hidden this article. Only its author and staff can see it.</p>
      )}

      <header
        className="kit-stripe -mx-4 rounded-none py-6 pl-9 pr-4 sm:mx-0 sm:rounded-panel sm:py-8 sm:pl-10 sm:pr-8"
        style={{
          ...kit,
          background: `linear-gradient(135deg, color-mix(in srgb, ${clubTint(blog.club?.slug)} 34%, var(--color-pitch)) 0%, var(--color-pitch-raised) 75%)`,
        }}
      >
        <p className="text-[0.95rem] text-chalk-dim">
          {blog.club ? (
            <Link href={`/clubs/${blog.club.slug}`} className="inline-flex items-center gap-2 text-chalk">
              <ClubBadge slug={blog.club.slug} size={20} />
              {blog.club.name}
            </Link>
          ) : (
            "General"
          )}
        </p>
        <h1 className="display mt-2 text-[2.6rem] sm:text-[3.6rem]">{blog.title}</h1>
        {blog.excerpt && <p className="mt-4 text-xl leading-relaxed text-chalk/85">{blog.excerpt}</p>}
      </header>

      <div className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-4 border-y border-line py-4">
        <div className="flex items-center gap-3">
          <UserAvatar name={blog.author.displayName} size={44} />
          <div className="text-[0.95rem] leading-tight">
            <Link href={`/profile/${blog.author.username}`} className="font-semibold text-chalk no-underline hover:underline">
              {blog.author.displayName}
            </Link>
            <p className="mt-1 flex gap-3 text-sm text-chalk-dim">
              {blog.publishedAt ? (
                <time dateTime={blog.publishedAt}>{formatDate(blog.publishedAt)}</time>
              ) : (
                <span>Last saved {formatDate(blog.updatedAt)}</span>
              )}
              <span>{blog.readingMinutes} min read</span>
            </p>
          </div>
        </div>
        {live && (
          <div className="flex flex-wrap items-center gap-4 sm:ml-auto">
            <BookmarkButton blogId={blog.id} initialBookmarked={blog.bookmarkedByMe} returnTo={returnTo} />
            {!blog.canEdit && <ReportDialog target={{ blogId: blog.id }} returnTo={returnTo} />}
          </div>
        )}
      </div>

      <div className="mt-10">
        <Markdown source={blog.content} />
      </div>

      {live && (
        <div className="mt-12 flex flex-wrap items-center gap-4 border-t border-line pt-6">
          <VoteButton blogId={blog.id} initialCount={blog.voteCount} initialVoted={blog.votedByMe} />
          <p className="text-[0.95rem] text-chalk-dim">Good read? Upvote it, no account needed. It helps other fans find it.</p>
        </div>
      )}

      {live && comments && (
        <div className="mt-14">
          <CommentList blogId={blog.id} slug={blog.slug} initial={comments} commentCount={blog.commentCount} />
        </div>
      )}
    </article>
  );
}
