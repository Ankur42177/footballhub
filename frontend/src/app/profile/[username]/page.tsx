import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { BlogFeed } from "@/components/blog/BlogFeed";
import { pageQuery, withParams } from "@/components/ui/Pagination";
import { PageSkeleton } from "@/components/ui/Skeletons";
import { UserAvatar } from "@/components/ui/UserAvatar";
import { formatDate } from "@/lib/format";
import { pageParam } from "@/lib/navigation";
import { getFeed, getProfile } from "@/lib/server-api";

export async function generateMetadata({ params }: PageProps<"/profile/[username]">): Promise<Metadata> {
  const { username } = await params;
  const profile = await getProfile(username).catch(() => null);
  if (!profile) return { title: "Profile", robots: { index: false } };
  return {
    title: `${profile.displayName} (@${profile.username})`,
    description: profile.bio ?? `Articles by ${profile.displayName} on FootballHub.`,
    alternates: { canonical: `/profile/${profile.username}` },
  };
}

export default function ProfilePage(props: PageProps<"/profile/[username]">) {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <Profile params={props.params} searchParams={props.searchParams} />
    </Suspense>
  );
}

const ROLE_LABEL = { USER: null, MODERATOR: "Moderator", ADMIN: "Administrator" } as const;

async function Profile({
  params,
  searchParams,
}: {
  params: PageProps<"/profile/[username]">["params"];
  searchParams: PageProps<"/profile/[username]">["searchParams"];
}) {
  const [{ username }, query] = await Promise.all([params, searchParams]);
  const page = pageParam(query.page);
  const [profile, posts] = await Promise.all([getProfile(username), getFeed({ author: username, page, size: 15 })]);
  if (!profile) notFound();
  const role = ROLE_LABEL[profile.role];

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <header className="flex flex-col gap-5 sm:flex-row sm:items-center">
        <UserAvatar name={profile.displayName} size={88} />
        <div>
          <h1 className="display text-5xl sm:text-6xl">{profile.displayName}</h1>
          <p className="mt-1 flex flex-wrap gap-x-3 text-chalk-dim">
            <span>@{profile.username}</span>
            {role && <span className="tag tag-warn">{role}</span>}
            <span>Joined {formatDate(profile.joinedAt)}</span>
          </p>
        </div>
      </header>
      {profile.bio && <p className="mt-6 max-w-2xl whitespace-pre-line text-lg">{profile.bio}</p>}

      <dl className="mt-8 flex gap-10 border-y border-line py-4">
        <div>
          <dt className="text-sm text-chalk-dim">Articles</dt>
          <dd className="score text-4xl">{profile.publishedPosts}</dd>
        </div>
        <div>
          <dt className="text-sm text-chalk-dim">Upvotes received</dt>
          <dd className="score text-4xl text-floodlight">{profile.totalVotes}</dd>
        </div>
      </dl>

      <section aria-labelledby="profile-posts" className="mt-10">
        <h2 id="profile-posts" className="display-medium text-3xl">
          Articles by {profile.displayName}
        </h2>
        <div className="mt-5">
          <BlogFeed
            page={posts}
            label={`Articles by ${profile.displayName}`}
            hrefFor={(target) => withParams(`/profile/${profile.username}`, { page: pageQuery(target) })}
            empty={<p>{profile.displayName} hasn&apos;t published anything yet.</p>}
          />
        </div>
      </section>
    </div>
  );
}
