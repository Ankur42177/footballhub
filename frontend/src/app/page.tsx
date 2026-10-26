import Link from "next/link";
import { Suspense } from "react";

import { BlogFeed, BlogFeedSkeleton } from "@/components/blog/BlogFeed";
import { FeaturedArticle } from "@/components/blog/FeaturedArticle";
import { pageQuery, withParams } from "@/components/ui/Pagination";
import { formatDate } from "@/lib/format";
import { firstParam, pageParam } from "@/lib/navigation";
import { getFeed } from "@/lib/server-api";
import type { FeedParams } from "@/lib/server-api";

type Sort = NonNullable<FeedParams["sort"]>;

const SORTS: { value: Sort; label: string }[] = [
  { value: "latest", label: "Latest" },
  { value: "trending", label: "Trending" },
  { value: "top", label: "Most upvoted" },
];

function parseSort(value: string | undefined): Sort {
  return SORTS.some((sort) => sort.value === value) ? (value as Sort) : "latest";
}

export default function HomePage(props: PageProps<"/">) {
  return (
    <Suspense fallback={<HomeSkeleton />}>
      <HomeContent searchParams={props.searchParams} />
    </Suspense>
  );
}

async function HomeContent({ searchParams }: { searchParams: PageProps<"/">["searchParams"] }) {
  const params = await searchParams;
  const sort = parseSort(firstParam(params.sort));
  const page = pageParam(params.page);
  const showLead = sort === "latest" && page === 0;

  const [feed, trending] = await Promise.all([
    getFeed({ sort, page, size: showLead ? 11 : 10 }),
    getFeed({ sort: "trending", size: 5 }),
  ]);

  const lead = showLead ? feed.content[0] : undefined;
  const rest = lead ? { ...feed, content: feed.content.slice(1) } : feed;
  const hrefFor = (target: number) => withParams("/", { sort: sort === "latest" ? undefined : sort, page: pageQuery(target) });

  return (
    <>
      {lead && <h1 className="sr-only">FootballHub: Premier League analysis by fans</h1>}
      {lead ? (
        <FeaturedArticle blog={lead} />
      ) : (
        <div className="border-b border-line bg-pitch-deep">
          <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
            <h1 className="display text-5xl sm:text-6xl">
              {sort === "top" ? "Most upvoted" : sort === "trending" ? "Trending now" : "Latest articles"}
            </h1>
          </div>
        </div>
      )}

      <section aria-labelledby="learn-title" className="border-b border-line bg-pitch-raised">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-6 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <div>
            <h2 id="learn-title" className="display-medium text-3xl">
              New to football? <span className="text-floodlight">Start here.</span>
            </h2>
            <p className="mt-1 text-[0.95rem] text-chalk-dim">
              Positions, the offside rule, fouls and cards, and how a season works, explained for beginners.
            </p>
          </div>
          <Link href="/learn" className="btn btn-primary shrink-0 self-start sm:self-auto">
            Learn football
          </Link>
        </div>
      </section>

      <div className="mx-auto grid max-w-6xl gap-12 px-4 py-10 sm:px-6 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <section aria-labelledby="feed-title">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <h2 id="feed-title" className="display-medium text-3xl">
              {lead ? "More from the community" : "Articles"}
            </h2>
            <nav aria-label="Sort articles" className="flex gap-1">
              {SORTS.map((option) => (
                <Link
                  key={option.value}
                  href={option.value === "latest" ? "/" : `/?sort=${option.value}`}
                  aria-current={option.value === sort ? "page" : undefined}
                  className={`rounded-chip px-3 py-1 text-[0.95rem] no-underline ${
                    option.value === sort ? "bg-chalk text-pitch" : "text-chalk-dim hover:text-chalk"
                  }`}
                >
                  {option.label}
                </Link>
              ))}
            </nav>
          </div>
          <div className="mt-5">
            <BlogFeed
              page={rest}
              hrefFor={hrefFor}
              empty={
                lead ? (
                  <p>That&apos;s everything so far.</p>
                ) : (
                  <p>
                    No articles yet. <Link href="/write">Be the first to write one</Link>.
                  </p>
                )
              }
            />
          </div>
        </section>

        <aside className="flex flex-col gap-10">
          {trending.content.length > 0 && (
            <section aria-labelledby="trending-title">
              <h2 id="trending-title" className="display-medium text-2xl">
                Trending
              </h2>
              <ol className="mt-3 flex flex-col gap-4">
                {trending.content.map((blog, index) => (
                  <li key={blog.id} className="grid grid-cols-[2rem_1fr] gap-2">
                    <span className="score text-3xl text-floodlight" aria-hidden="true">
                      {index + 1}
                    </span>
                    <div>
                      <Link href={`/blogs/${blog.slug}`} className="font-semibold leading-snug no-underline hover:underline">
                        {blog.title}
                      </Link>
                      <p className="mt-0.5 text-sm text-chalk-dim">
                        {blog.club?.name ?? "General"}, {formatDate(blog.publishedAt)}
                      </p>
                    </div>
                  </li>
                ))}
              </ol>
            </section>
          )}
          <section className="border-t border-line pt-6">
            <h2 className="display-medium text-2xl">Got a take?</h2>
            <p className="mt-2 text-[0.95rem] text-chalk-dim">
              Tactics, transfers, the one substitution that changed everything. Write it up and let other fans argue
              with you.
            </p>
            <Link href="/write" className="btn btn-primary mt-4">
              Write an article
            </Link>
          </section>
        </aside>
      </div>
    </>
  );
}

function HomeSkeleton() {
  return (
    <>
      <div className="border-b border-line bg-pitch-deep" aria-hidden="true">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-24">
          <div className="skeleton h-4 w-40" />
          <div className="skeleton mt-5 h-16 w-full max-w-3xl" />
          <div className="skeleton mt-3 h-16 w-2/3 max-w-2xl" />
        </div>
      </div>
      <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
        <span className="sr-only">Loading articles…</span>
        <BlogFeedSkeleton />
      </div>
    </>
  );
}
