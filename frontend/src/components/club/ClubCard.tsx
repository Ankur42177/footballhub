import Link from "next/link";
import type { CSSProperties } from "react";

import { FollowClubButton } from "@/components/club/FollowClubButton";
import { ClubBadge } from "@/components/ui/ClubBadge";
import { clubColor } from "@/lib/clubs";
import { plural } from "@/lib/format";
import type { Club } from "@/types/api";

export function ClubCard({ club }: { club: Club }) {
  const kit = { "--kit": clubColor(club.slug, club.primaryColor) } as CSSProperties;
  return (
    <div className="kit-stripe flex items-center justify-between gap-4 py-4 pl-5" style={kit}>
      <div className="flex min-w-0 items-center gap-3">
        <ClubBadge slug={club.slug} size={34} />
        <div className="min-w-0">
        <h2 className="display-medium text-2xl">
          <Link href={`/clubs/${club.slug}`} className="no-underline hover:text-floodlight">
            {club.name}
          </Link>
        </h2>
        <p className="text-sm text-chalk-dim">{plural(club.postCount, "article")}</p>
        </div>
      </div>
      <FollowClubButton
        clubId={club.id}
        clubName={club.name}
        initialFollowing={club.followedByMe}
        initialFollowers={club.followerCount}
        returnTo="/clubs"
        showCount={false}
      />
    </div>
  );
}
