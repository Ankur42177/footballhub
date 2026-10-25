import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import type { CSSProperties } from "react";

import { BlogFeed } from "@/components/blog/BlogFeed";
import { FollowClubButton } from "@/components/club/FollowClubButton";
import { ClubBadge } from "@/components/ui/ClubBadge";
import { pageQuery, withParams } from "@/components/ui/Pagination";
import { PageSkeleton } from "@/components/ui/Skeletons";
import { CLUBS, clubColor, clubTint } from "@/lib/clubs";
import { plural } from "@/lib/format";
import { firstParam, pageParam } from "@/lib/navigation";
import { getClub, getFeed } from "@/lib/server-api";

export async function generateMetadata({ params }: PageProps<"/clubs/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  // Static club list: no API call needed for the page title.
  const club = CLUBS.find((item) => item.slug === slug);
  if (!club) return { title: "Club" };
  return {
    title: `${club.name} articles`,
    description: `Fan analysis and opinion about ${club.name} on FootballHub.`,
    alternates: { canonical: `/clubs/${club.slug}` },
  };
}

export default function ClubPage(props: PageProps<"/clubs/[slug]">) {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <ClubContent params={props.params} searchParams={props.searchParams} />
    </Suspense>
  );
}

async function ClubContent({
  params,
  searchParams,
}: {
  params: PageProps<"/clubs/[slug]">["params"];
  searchParams: PageProps<"/clubs/[slug]">["searchParams"];
}) {
  const [{ slug }, query] = await Promise.all([params, searchParams]);
  const sort = firstParam(query.sort) === "top" ? "top" : "latest";
  const page = pageParam(query.page);

  const [club, feed] = await Promise.all([getClub(slug), getFeed({ club: slug, sort, page, size: 15 })]);
  if (!club) notFound();

  const kit = { "--kit": clubColor(club.slug, club.primaryColor) } as CSSProperties;
  const path = `/clubs/${club.slug}`;

  return (
    <>
      <div
        className="border-b border-line"
        style={{
          ...kit,
          background: `linear-gradient(135deg, color-mix(in srgb, ${clubTint(club.slug)} 38%, var(--color-pitch-deep)) 0%, var(--color-pitch-deep) 70%)`,
        }}
      >
        <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
          <div className="kit-stripe pl-5 sm:pl-7">
            <div className="flex items-center gap-4 sm:gap-6">
              <ClubBadge slug={club.slug} size={64} className="sm:h-[95px] sm:w-[80px]" />
              <div>
                <p className="text-[0.95rem] text-chalk-dim">{club.league}</p>
                <h1 className="display mt-1 text-5xl sm:text-7xl">{club.name}</h1>
              </div>
            </div>
            {club.description && <p className="mt-4 max-w-2xl text-lg text-chalk/85">{club.description}</p>}
            <div className="mt-6 flex flex-wrap items-center gap-x-8 gap-y-3">
              <FollowClubButton
                clubId={club.id}
                clubName={club.name}
                initialFollowing={club.followedByMe}
                initialFollowers={club.followerCount}
                returnTo={path}
              />
              <p className="text-sm text-chalk-dim">{plural(club.postCount, "article")}</p>
            </div>
          </div>
        </div>
      </div>

      <section className="mx-auto max-w-6xl px-4 py-10 sm:px-6" aria-labelledby="club-feed-title">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <h2 id="club-feed-title" className="display-medium text-3xl">
            Articles
          </h2>
          <nav aria-label="Sort articles" className="flex gap-1">
            {(["latest", "top"] as const).map((value) => (
              <Link
                key={value}
                href={value === "latest" ? path : `${path}?sort=top`}
                aria-current={value === sort ? "page" : undefined}
                className={`rounded-chip px-3 py-1 text-[0.95rem] no-underline ${
                  value === sort ? "bg-chalk text-pitch" : "text-chalk-dim hover:text-chalk"
                }`}
              >
                {value === "latest" ? "Latest" : "Most upvoted"}
              </Link>
            ))}
          </nav>
        </div>
        <div className="mt-5">
          <BlogFeed
            page={feed}
            label={`${club.name} articles`}
            hrefFor={(target) => withParams(path, { sort: sort === "top" ? "top" : undefined, page: pageQuery(target) })}
            empty={
              <p>
                Nobody has written about {club.name} yet. <Link href="/write">Write the first article</Link>.
              </p>
            }
          />
        </div>
      </section>
    </>
  );
}
