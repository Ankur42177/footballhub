import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { BlogCard } from "@/components/blog/BlogCard";
import { BlogFeedSkeleton } from "@/components/blog/BlogFeed";
import { ClubBadge } from "@/components/ui/ClubBadge";
import { getChampionsLeagueFixtures, getFeed } from "@/lib/server-api";
import type { CupMatch, CupTeam } from "@/types/api";

export const metadata: Metadata = {
  title: "Champions League",
  description:
    "Premier League clubs in the Champions League: upcoming fixtures, recent results, England's winners and our Champions League stories.",
  alternates: { canonical: "/champions-league" },
};

const WINNERS: { club: string; slug: string | null; titles: number; years: string }[] = [
  { club: "Liverpool", slug: "liverpool", titles: 6, years: "1977, 1978, 1981, 1984, 2005, 2019" },
  { club: "Manchester United", slug: "manchester-united", titles: 3, years: "1968, 1999, 2008" },
  { club: "Chelsea", slug: "chelsea", titles: 2, years: "2012, 2021" },
  { club: "Nottingham Forest", slug: "nottingham-forest", titles: 2, years: "1979, 1980" },
  { club: "Aston Villa", slug: "aston-villa", titles: 1, years: "1982" },
  { club: "Manchester City", slug: "manchester-city", titles: 1, years: "2023" },
];

const kickOffFormat = new Intl.DateTimeFormat("en-GB", {
  weekday: "short",
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Europe/London",
});

export default function ChampionsLeaguePage() {
  return (
    <div>
      <div className="border-b border-line bg-pitch-deep">
        <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 sm:py-16">
          <p className="text-[0.95rem] text-chalk-dim">Europe</p>
          <h1 className="display mt-2 text-5xl sm:text-7xl">Champions League</h1>
          <p className="mt-4 max-w-2xl text-lg text-chalk/85">
            Premier League clubs against Europe&apos;s best: what&apos;s coming up, how they&apos;ve done, the nights
            English clubs lifted the trophy, and our analysis of all of it.
          </p>
          <nav aria-label="On this page" className="mt-8 flex flex-wrap gap-2">
            {[
              ["fixtures", "Fixtures"],
              ["results", "Results"],
              ["winners", "English winners"],
              ["stories", "Stories"],
            ].map(([id, label]) => (
              <a
                key={id}
                href={`#${id}`}
                className="rounded-chip border border-line-strong px-3 py-1 text-sm text-chalk no-underline hover:border-floodlight hover:text-floodlight"
              >
                {label}
              </a>
            ))}
          </nav>
        </div>
      </div>

      <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
        <Suspense fallback={<FixturesSkeleton />}>
          <Fixtures />
        </Suspense>

        <section id="winners" aria-labelledby="winners-title" className="scroll-mt-6 border-t border-line py-10">
          <h2 id="winners-title" className="display-medium text-4xl">
            English winners
          </h2>
          <p className="mt-2 text-chalk-dim">
            English clubs have won Europe&apos;s top prize 15 times, seven of them since the Premier League began in 1992.
          </p>
          <div className="table-scroll mt-5">
            <table className="w-full min-w-[32rem] text-left text-[0.98rem]">
              <thead className="border-b border-line text-sm text-chalk-dim">
                <tr>
                  <th scope="col" className="py-2 pr-4 font-semibold">Club</th>
                  <th scope="col" className="py-2 pr-4 font-semibold">Titles</th>
                  <th scope="col" className="py-2 font-semibold">Won in</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {WINNERS.map((row) => (
                  <tr key={row.club}>
                    <td className="py-3 pr-4">
                      <span className="flex items-center gap-2">
                        {row.slug && <ClubBadge slug={row.slug} size={18} />}
                        {row.slug ? <Link href={`/clubs/${row.slug}`}>{row.club}</Link> : row.club}
                      </span>
                    </td>
                    <td className="score py-3 pr-4 text-2xl text-floodlight">{row.titles}</td>
                    <td className="py-3 text-chalk-dim">{row.years}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section id="stories" aria-labelledby="stories-title" className="scroll-mt-6 border-t border-line py-10">
          <h2 id="stories-title" className="display-medium text-4xl">
            Champions League stories
          </h2>
          <p className="mt-2 text-chalk-dim">
            How Premier League clubs won Europe&apos;s biggest prize: the finals, the players who made the difference,
            the tactics behind each win, and why English clubs find the competition so hard.
          </p>
          <div className="mt-6">
            <Suspense fallback={<BlogFeedSkeleton rows={3} />}>
              <Stories />
            </Suspense>
          </div>
        </section>
      </div>
    </div>
  );
}

async function Fixtures() {
  const data = await getChampionsLeagueFixtures().catch(() => null);
  const configured = data?.configured ?? false;
  return (
    <>
      <section id="fixtures" aria-labelledby="fixtures-title" className="scroll-mt-6 pb-10">
        <h2 id="fixtures-title" className="display-medium text-4xl">
          Upcoming matches
        </h2>
        <p className="mt-2 text-chalk-dim">Premier League clubs&apos; next Champions League fixtures. Kick-off times are UK time.</p>
        <div className="mt-5">
          {!configured ? (
            <p className="notice">Live fixtures are being connected and will appear here soon.</p>
          ) : data && data.upcoming.length > 0 ? (
            <MatchList matches={data.upcoming} />
          ) : (
            <p className="text-chalk-dim">No Champions League fixtures for Premier League clubs in the next few weeks.</p>
          )}
        </div>
      </section>
      <section id="results" aria-labelledby="results-title" className="scroll-mt-6 border-t border-line py-10">
        <h2 id="results-title" className="display-medium text-4xl">
          Recent results
        </h2>
        <div className="mt-5">
          {configured && data && data.recent.length > 0 ? (
            <MatchList matches={data.recent} />
          ) : (
            <p className="text-chalk-dim">{configured ? "No recent results." : "Results will appear here with the fixtures."}</p>
          )}
        </div>
        {data?.updatedAt && (
          <p className="mt-3 text-xs text-chalk-dim">Data from football-data.org.</p>
        )}
      </section>
    </>
  );
}

function MatchList({ matches }: { matches: CupMatch[] }) {
  return (
    <ul className="divide-y divide-line border-y border-line">
      {matches.map((match) => {
        const finished = match.status === "FINISHED";
        const live = match.status === "IN_PLAY" || match.status === "PAUSED";
        return (
          <li key={match.id} className="grid gap-2 py-4 sm:grid-cols-[11rem_1fr_auto] sm:items-center">
            <div className="text-sm text-chalk-dim">
              <time dateTime={match.kickOff}>{kickOffFormat.format(new Date(match.kickOff))}</time>
              <span className="block">
                {match.stage}
                {match.matchday ? `, matchday ${match.matchday}` : ""}
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[1.05rem]">
              <TeamName team={match.home} />
              <span className="score text-2xl text-chalk">
                {finished || live ? `${match.homeGoals ?? 0} - ${match.awayGoals ?? 0}` : "v"}
              </span>
              <TeamName team={match.away} />
            </div>
            <div>{live ? <span className="tag tag-danger">Live</span> : finished ? <span className="tag">Full time</span> : null}</div>
          </li>
        );
      })}
    </ul>
  );
}

function TeamName({ team }: { team: CupTeam }) {
  if (!team.clubSlug) return <span className="text-chalk-dim">{team.name}</span>;
  return (
    <Link href={`/clubs/${team.clubSlug}`} className="flex items-center gap-1.5 font-semibold text-chalk no-underline hover:underline">
      <ClubBadge slug={team.clubSlug} size={18} />
      {team.name.replace(/\s+(A?FC)$/, "")}
    </Link>
  );
}

/**
 * Only stories about Premier League clubs in the Champions League: the winning campaigns,
 * the players and tactics behind them, and how English clubs fare in Europe. A story
 * qualifies when its title names the competition, so passing mentions don't count.
 */
async function Stories() {
  const feed = await getFeed({ q: "Champions League", sort: "featured", size: 50 });
  const stories = feed.content.filter((blog) => /champions league/i.test(blog.title));
  if (stories.length === 0) return <p className="text-chalk-dim">No Champions League stories yet.</p>;
  return (
    <ol className="divide-y divide-line border-y border-line" aria-label="Champions League stories">
      {stories.map((blog) => (
        <li key={blog.id}>
          <BlogCard blog={blog} />
        </li>
      ))}
    </ol>
  );
}

function FixturesSkeleton() {
  return (
    <div className="pb-10" aria-busy="true">
      <span className="sr-only">Loading fixtures…</span>
      <div className="skeleton h-9 w-72" />
      <div className="mt-6 flex flex-col gap-3">
        {Array.from({ length: 4 }, (_, index) => (
          <div key={index} className="skeleton h-12 w-full" />
        ))}
      </div>
    </div>
  );
}
