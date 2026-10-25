import type { Metadata } from "next";
import { Suspense } from "react";

import { BlogFeed, BlogFeedSkeleton } from "@/components/blog/BlogFeed";
import { pageQuery, withParams } from "@/components/ui/Pagination";
import { CLUBS } from "@/lib/clubs";
import { plural } from "@/lib/format";
import { firstParam, pageParam } from "@/lib/navigation";
import { getFeed } from "@/lib/server-api";
import type { FeedParams } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Search",
  description: "Search FootballHub articles by keyword, club and popularity.",
  alternates: { canonical: "/search" },
};

const SORTS = [
  { value: "latest", label: "Newest first" },
  { value: "oldest", label: "Oldest first" },
  { value: "top", label: "Most upvoted" },
] as const;

type Sort = (typeof SORTS)[number]["value"];

export default function SearchPage(props: PageProps<"/search">) {
  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <h1 className="display text-5xl sm:text-6xl">Search</h1>
      <Suspense fallback={<SearchSkeleton />}>
        <SearchContent searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}

async function SearchContent({ searchParams }: { searchParams: PageProps<"/search">["searchParams"] }) {
  const params = await searchParams;
  const q = (firstParam(params.q) ?? "").trim().slice(0, 100);
  const club = CLUBS.some((item) => item.slug === firstParam(params.club)) ? firstParam(params.club) : undefined;
  const requestedSort = firstParam(params.sort);
  const sort: Sort = SORTS.find((item) => item.value === requestedSort)?.value ?? "latest";
  const page = pageParam(params.page);

  const feedParams: FeedParams = { q: q || undefined, club, sort, page, size: 15 };
  const results = await getFeed(feedParams);
  const filtered = Boolean(q || club);

  return (
    <>
      <form action="/search" role="search" className="mt-6 grid gap-4 sm:grid-cols-[1fr_12rem_11rem_auto] sm:items-end">
        <div>
          <label htmlFor="q" className="label">
            Keywords
          </label>
          <input id="q" name="q" type="search" defaultValue={q} maxLength={100} className="input" placeholder="Pressing, Haaland, derby…" />
        </div>
        <div>
          <label htmlFor="club" className="label">
            Club
          </label>
          <select id="club" name="club" defaultValue={club ?? ""} className="input">
            <option value="">Any club</option>
            {CLUBS.map((item) => (
              <option key={item.slug} value={item.slug}>
                {item.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="sort" className="label">
            Order
          </label>
          <select id="sort" name="sort" defaultValue={sort} className="input">
            {SORTS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </div>
        <button type="submit" className="btn btn-primary">
          Search
        </button>
      </form>

      <p className="mt-8 text-chalk-dim" role="status">
        {filtered
          ? `${plural(results.totalElements, "article")} found${q ? ` for “${q}”` : ""}${
              club ? ` about ${CLUBS.find((item) => item.slug === club)?.name}` : ""
            }.`
          : "Showing every article. Add keywords or pick a club to narrow it down."}
      </p>
      <div className="mt-4">
        <BlogFeed
          page={results}
          label="Search results"
          hrefFor={(target) =>
            withParams("/search", { q: q || undefined, club, sort: sort === "latest" ? undefined : sort, page: pageQuery(target) })
          }
          empty={<p>No articles match. Try fewer or different words.</p>}
        />
      </div>
    </>
  );
}

function SearchSkeleton() {
  return (
    <div className="mt-6" aria-busy="true">
      <span className="sr-only">Searching…</span>
      <div className="skeleton h-11 w-full" />
      <div className="mt-10">
        <BlogFeedSkeleton />
      </div>
    </div>
  );
}
