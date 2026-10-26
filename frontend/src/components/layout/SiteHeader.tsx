import Link from "next/link";
import { Suspense } from "react";

import { UserMenu } from "@/components/layout/UserMenu";

export function SiteHeader() {
  return (
    <header className="border-b border-line bg-pitch-deep">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4 sm:px-6">
        <Link href="/" className="display shrink-0 text-[1.9rem] text-chalk no-underline" aria-label="FootballHub home">
          Football<span className="text-floodlight">Hub</span>
        </Link>

        <nav aria-label="Main" className="hidden items-center gap-5 text-[0.95rem] text-chalk-dim md:flex">
          <Link href="/learn" className="font-semibold text-floodlight no-underline hover:text-chalk">
            Learn football
          </Link>
          <Link href="/champions-league" className="no-underline hover:text-chalk">
            Champions League
          </Link>
          <Link href="/clubs" className="no-underline hover:text-chalk">
            Clubs
          </Link>
          <Link href="/?sort=top" className="no-underline hover:text-chalk">
            Most upvoted
          </Link>
        </nav>

        <form action="/search" role="search" className="ml-auto hidden w-56 sm:block lg:w-72">
          <label htmlFor="site-search" className="sr-only">
            Search articles
          </label>
          <input
            id="site-search"
            name="q"
            type="search"
            placeholder="Search articles"
            className="h-9 w-full rounded-chip border border-line bg-pitch px-4 text-[0.95rem] text-chalk placeholder:text-chalk-dim/70 focus:border-floodlight focus:outline-none"
          />
        </form>

        <Link
          href="/search"
          aria-label="Search articles"
          className="ml-auto flex h-9 w-9 items-center justify-center rounded-full text-chalk-dim hover:bg-pitch-raised hover:text-chalk sm:hidden"
        >
          <svg viewBox="0 0 20 20" className="h-5 w-5" aria-hidden="true" focusable="false">
            <circle cx="8.5" cy="8.5" r="5.5" fill="none" stroke="currentColor" strokeWidth="2" />
            <line x1="12.6" y1="12.6" x2="17" y2="17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </Link>

        {/* UserMenu reads the pathname, which is request data on dynamic routes. */}
        <Suspense fallback={<div className="h-9 w-28" aria-hidden="true" />}>
          <UserMenu />
        </Suspense>
      </div>
    </header>
  );
}
