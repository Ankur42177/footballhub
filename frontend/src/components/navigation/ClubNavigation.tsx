import Link from "next/link";

import { ClubBadge } from "@/components/ui/ClubBadge";
import { CLUBS } from "@/lib/clubs";

/**
 * Visible club navigation: every Premier League club, one tap away. Wide screens show all
 * 20 clubs on two centred rows; phones scroll sideways, with a fade at the edge to show
 * there's more.
 */
export function ClubNavigation() {
  return (
    <nav aria-label="Clubs" className="relative border-b border-line bg-pitch">
      <ul className="no-scrollbar relative mx-auto flex max-w-6xl gap-1 overflow-x-auto px-3 py-2 sm:px-5 lg:flex-wrap lg:justify-center lg:overflow-visible">
        {CLUBS.map((club) => (
          <li key={club.slug} className="shrink-0">
            <Link
              href={`/clubs/${club.slug}`}
              title={club.name}
              className="flex items-center gap-1.5 rounded-chip px-2.5 py-1 font-display text-[1.05rem] font-bold tracking-wide text-chalk-dim no-underline hover:bg-pitch-raised hover:text-chalk"
            >
              <ClubBadge slug={club.slug} size={18} />
              <span aria-hidden="true">{club.shortName}</span>
              <span className="sr-only">{club.name}</span>
            </Link>
          </li>
        ))}
      </ul>
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-y-0 right-0 w-10 bg-gradient-to-l from-pitch to-transparent lg:hidden"
      />
    </nav>
  );
}
