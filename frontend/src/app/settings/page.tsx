import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { PasswordForm, ProfileForm, ResendVerification } from "@/components/account/SettingsForms";
import { ClubCard } from "@/components/club/ClubCard";
import { PageSkeleton } from "@/components/ui/Skeletons";
import { requireMe } from "@/lib/auth";
import { formatDate } from "@/lib/format";
import { getMyClubs } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Settings",
  robots: { index: false },
};

export default function SettingsPage() {
  return (
    <Suspense fallback={<PageSkeleton withFeed={false} />}>
      <Settings />
    </Suspense>
  );
}

async function Settings() {
  const me = await requireMe("/settings");
  const clubs = await getMyClubs();

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <h1 className="display text-5xl sm:text-6xl">Settings</h1>
      <p className="mt-3 text-chalk-dim">
        @{me.username}, {me.email}. Member since {formatDate(me.createdAt)}.
      </p>

      {!me.emailVerified && (
        <div className="mt-6">
          <ResendVerification />
        </div>
      )}

      <section aria-labelledby="profile-title" className="mt-10 border-t border-line pt-8">
        <h2 id="profile-title" className="display-medium text-3xl">
          Profile
        </h2>
        <p className="mt-1 text-[0.95rem] text-chalk-dim">
          What readers see on <Link href={`/profile/${me.username}`}>your public profile</Link>. Your username can&apos;t
          be changed.
        </p>
        <div className="mt-6">
          <ProfileForm me={me} />
        </div>
      </section>

      <section aria-labelledby="clubs-title" className="mt-12 border-t border-line pt-8">
        <h2 id="clubs-title" className="display-medium text-3xl">
          Clubs you follow
        </h2>
        {clubs.length === 0 ? (
          <p className="mt-3 text-chalk-dim">
            None yet. <Link href="/clubs">Pick some clubs to follow</Link>.
          </p>
        ) : (
          <ul className="mt-4 border-t border-line">
            {clubs.map((club) => (
              <li key={club.id} className="border-b border-line">
                <ClubCard club={club} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="password-title" className="mt-12 border-t border-line pt-8">
        <h2 id="password-title" className="display-medium text-3xl">
          Password
        </h2>
        <div className="mt-6 max-w-md">
          <PasswordForm />
        </div>
      </section>
    </div>
  );
}
