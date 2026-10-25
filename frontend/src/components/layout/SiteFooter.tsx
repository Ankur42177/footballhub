import Link from "next/link";

export function SiteFooter() {
  return (
    <footer className="mt-20 border-t border-line bg-pitch-deep">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-10 text-sm text-chalk-dim sm:flex-row sm:items-end sm:justify-between sm:px-6">
        <div>
          <p className="display text-2xl text-chalk">
            Football<span className="text-floodlight">Hub</span>
          </p>
          <p className="mt-2 max-w-md">
            Fan-written Premier League analysis. Not affiliated with the Premier League or any club; club names are
            used for identification only.
          </p>
        </div>
        <nav aria-label="Footer" className="flex flex-wrap gap-x-5 gap-y-2">
          <Link href="/clubs" className="no-underline hover:text-chalk">
            All clubs
          </Link>
          <Link href="/search" className="no-underline hover:text-chalk">
            Search
          </Link>
          <Link href="/write" className="no-underline hover:text-chalk">
            Write for FootballHub
          </Link>
          <a href="https://github.com/Ankur42177/footballhub" className="no-underline hover:text-chalk">
            Source on GitHub
          </a>
        </nav>
      </div>
      <div className="border-t border-line">
        <div className="mx-auto flex max-w-6xl flex-col gap-1 px-4 py-4 text-sm text-chalk-dim sm:flex-row sm:justify-between sm:px-6">
          <p>
            Designed by <span className="text-chalk">Ankur</span>
          </p>
          <p>
            Handled by <span className="text-chalk">ravenclaw_007 LLC</span>
          </p>
        </div>
      </div>
    </footer>
  );
}
