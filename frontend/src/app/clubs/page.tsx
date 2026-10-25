import type { Metadata } from "next";
import { Suspense } from "react";

import { ClubCard } from "@/components/club/ClubCard";
import { getClubs } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Clubs",
  description: "Every Premier League club on FootballHub. Follow the ones you care about.",
  alternates: { canonical: "/clubs" },
};

export default function ClubsPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="display text-5xl sm:text-6xl">Clubs</h1>
      <p className="mt-3 max-w-2xl text-chalk-dim">
        The 2026-27 Premier League. Follow a club to keep track of new writing about it.
      </p>
      <Suspense fallback={<ClubGridSkeleton />}>
        <ClubGrid />
      </Suspense>
    </div>
  );
}

async function ClubGrid() {
  const clubs = await getClubs();
  if (clubs.length === 0) {
    return <p className="mt-10 text-chalk-dim">Clubs couldn&apos;t be loaded. Try again in a minute.</p>;
  }
  return (
    <ul className="mt-10 grid gap-x-10 border-t border-line sm:grid-cols-2">
      {clubs.map((club) => (
        <li key={club.id} className="border-b border-line">
          <ClubCard club={club} />
        </li>
      ))}
    </ul>
  );
}

function ClubGridSkeleton() {
  return (
    <div className="mt-10 grid gap-x-10 gap-y-6 sm:grid-cols-2" aria-busy="true">
      <span className="sr-only">Loading clubs…</span>
      {Array.from({ length: 10 }, (_, index) => (
        <div key={index} className="skeleton h-12" />
      ))}
    </div>
  );
}
