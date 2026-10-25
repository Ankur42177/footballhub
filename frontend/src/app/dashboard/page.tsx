import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { NotificationList } from "@/components/account/NotificationList";
import { ResendVerification } from "@/components/account/SettingsForms";
import { BlogFeed } from "@/components/blog/BlogFeed";
import { pageQuery, withParams } from "@/components/ui/Pagination";
import { PageSkeleton } from "@/components/ui/Skeletons";
import { requireMe } from "@/lib/auth";
import { firstParam, pageParam } from "@/lib/navigation";
import { getMyBlogs, getNotifications } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Your posts",
  robots: { index: false },
};

const TABS = [
  { value: undefined, label: "All" },
  { value: "draft", label: "Drafts" },
  { value: "published", label: "Published" },
  { value: "archived", label: "Archived" },
] as const;

type Status = (typeof TABS)[number]["value"];

export default function DashboardPage(props: PageProps<"/dashboard">) {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <Dashboard searchParams={props.searchParams} />
    </Suspense>
  );
}

async function Dashboard({ searchParams }: { searchParams: PageProps<"/dashboard">["searchParams"] }) {
  const params = await searchParams;
  const requested = firstParam(params.status);
  const status: Status = TABS.find((tab) => tab.value === requested)?.value;
  const page = pageParam(params.page);
  const me = await requireMe(withParams("/dashboard", { status }));
  const [posts, notifications] = await Promise.all([getMyBlogs(status, page), getNotifications()]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="display text-5xl sm:text-6xl">Your posts</h1>
          <p className="mt-2 text-chalk-dim">
            Signed in as <Link href={`/profile/${me.username}`}>@{me.username}</Link>
          </p>
        </div>
        <Link href="/write" className="btn btn-primary">
          Write an article
        </Link>
      </div>

      {!me.emailVerified && (
        <div className="mt-6">
          <ResendVerification />
        </div>
      )}

      <div className="mt-10 grid gap-12 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <section aria-labelledby="posts-title">
          <h2 id="posts-title" className="sr-only">
            Articles
          </h2>
          <nav aria-label="Filter by status" className="flex flex-wrap gap-1">
            {TABS.map((tab) => (
              <Link
                key={tab.label}
                href={withParams("/dashboard", { status: tab.value })}
                aria-current={tab.value === status ? "page" : undefined}
                className={`rounded-chip px-3 py-1 text-[0.95rem] no-underline ${
                  tab.value === status ? "bg-chalk text-pitch" : "text-chalk-dim hover:text-chalk"
                }`}
              >
                {tab.label}
              </Link>
            ))}
          </nav>
          <div className="mt-5">
            <BlogFeed
              page={posts}
              showStatus
              label="Your articles"
              hrefFor={(target) => withParams("/dashboard", { status, page: pageQuery(target) })}
              empty={
                status ? (
                  <p>Nothing here.</p>
                ) : (
                  <p>
                    You haven&apos;t written anything yet. <Link href="/write">Start your first article</Link>.
                  </p>
                )
              }
            />
          </div>
        </section>

        <section aria-labelledby="notifications-title">
          <h2 id="notifications-title" className="display-medium text-3xl">
            Notifications
          </h2>
          <div className="mt-4">
            <NotificationList initial={notifications.content} />
          </div>
        </section>
      </div>
    </div>
  );
}
