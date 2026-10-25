import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { BlogEditor } from "@/components/blog/BlogEditor";
import { PageSkeleton } from "@/components/ui/Skeletons";
import { requireMe } from "@/lib/auth";
import { firstParam } from "@/lib/navigation";
import { getBlog, getClubs } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Write",
  robots: { index: false },
};

export default function WritePage(props: PageProps<"/write">) {
  return (
    <Suspense fallback={<PageSkeleton withFeed={false} />}>
      <WriteContent searchParams={props.searchParams} />
    </Suspense>
  );
}

async function WriteContent({ searchParams }: { searchParams: PageProps<"/write">["searchParams"] }) {
  const editSlug = firstParam((await searchParams).edit);
  const me = await requireMe(editSlug ? `/write?edit=${encodeURIComponent(editSlug)}` : "/write");
  const [clubs, existing] = await Promise.all([getClubs(), editSlug ? getBlog(editSlug) : Promise.resolve(null)]);
  if (editSlug && (!existing || !existing.canEdit)) notFound();

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <h1 className="display text-5xl sm:text-6xl">{existing ? "Edit article" : "Write an article"}</h1>
      <p className="mt-3 text-chalk-dim">
        {existing
          ? existing.status === "PUBLISHED"
            ? "Changes go live as soon as you save. The address of the article stays the same."
            : "Still a draft. Publish when it's ready."
          : `Writing as ${me.displayName}. Save a draft at any time; nobody else sees it until you publish.`}
      </p>
      {!me.emailVerified && (
        <p className="notice mt-6">
          Your email address isn&apos;t confirmed yet. If publishing is blocked, use the link we emailed you or send a new
          one from your settings.
        </p>
      )}
      <div className="mt-8">
        <BlogEditor clubs={clubs.map(({ id, name }) => ({ id, name }))} existing={existing ?? undefined} />
      </div>
    </div>
  );
}
