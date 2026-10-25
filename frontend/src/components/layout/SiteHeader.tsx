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

        {/* UserMenu reads the pathname, which is request data on dynamic routes. */}
        <Suspense fallback={<div className="ml-auto h-9 w-28 sm:ml-0" aria-hidden="true" />}>
          <UserMenu />
        </Suspense>
      </div>
    </header>
  );
}
