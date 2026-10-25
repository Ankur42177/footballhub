"use client";

import { useState } from "react";

import { useAuthAction } from "@/components/session/useAuthAction";
import { api } from "@/lib/api";
import { plural } from "@/lib/format";
import type { FollowResult } from "@/types/api";

export function FollowClubButton({
  clubId,
  clubName,
  initialFollowing,
  initialFollowers,
  returnTo,
  showCount = true,
}: {
  clubId: number;
  clubName: string;
  initialFollowing: boolean;
  initialFollowers: number;
  returnTo: string;
  showCount?: boolean;
}) {
  const [following, setFollowing] = useState(initialFollowing);
  const [followers, setFollowers] = useState(initialFollowers);
  const { run, pending, error } = useAuthAction(returnTo);

  async function toggle() {
    const result = await run(() =>
      api<FollowResult>(`/clubs/${clubId}/follow`, { method: following ? "DELETE" : "POST" }),
    );
    if (result) {
      setFollowing(result.following);
      setFollowers(result.followerCount);
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
      <button
        type="button"
        onClick={toggle}
        disabled={pending}
        aria-pressed={following}
        className={`btn btn-small ${following ? "btn-secondary" : "btn-primary"}`}
      >
        {following ? "Following" : "Follow"}
        <span className="sr-only"> {clubName}</span>
      </button>
      {showCount && <span className="text-sm text-chalk-dim">{plural(followers, "follower")}</span>}
      {error && (
        <p role="alert" className="field-error basis-full">
          {error}
        </p>
      )}
    </div>
  );
}
