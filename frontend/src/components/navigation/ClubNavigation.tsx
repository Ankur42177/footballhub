import Link from "next/link";

import { CLUBS } from "@/lib/clubs";

/** Visible club navigation: every Premier League club, one tap away. */
export function ClubNavigation() {
  return (
    <nav aria-label="Clubs" className="border-b border-line bg-pitch">
      <ul className="no-scrollbar mx-auto flex max-w-6xl gap-1 overflow-x-auto px-3 py-2 sm:px-5">
        {CLUBS.map((club) => (
          <li key={club.slug} className="shrink-0">
            <Link
              href={`/clubs/${club.slug}`}
              title={club.name}
              className="flex items-center gap-1.5 rounded-chip px-2.5 py-1 font-display text-[1.05rem] font-bold tracking-wide text-chalk-dim no-underline hover:bg-pitch-raised hover:text-chalk"
            >
              <span aria-hidden="true" className="h-2.5 w-2.5 rounded-full" style={{ background: club.color }} />
              <span aria-hidden="true">{club.shortName}</span>
              <span className="sr-only">{club.name}</span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
