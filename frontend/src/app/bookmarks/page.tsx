import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { BlogFeed } from "@/components/blog/BlogFeed";
import { pageQuery, withParams } from "@/components/ui/Pagination";
import { PageSkeleton } from "@/components/ui/Skeletons";
import { requireMe } from "@/lib/auth";
import { pageParam } from "@/lib/navigation";
import { getMyBookmarks } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Bookmarks",
  robots: { index: false },
};

export default function BookmarksPage(props: PageProps<"/bookmarks">) {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <Bookmarks searchParams={props.searchParams} />
    </Suspense>
  );
}

async function Bookmarks({ searchParams }: { searchParams: PageProps<"/bookmarks">["searchParams"] }) {
  const page = pageParam((await searchParams).page);
  await requireMe("/bookmarks");
  const bookmarks = await getMyBookmarks(page);

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <h1 className="display text-5xl sm:text-6xl">Bookmarks</h1>
      <p className="mt-3 text-chalk-dim">Articles you saved to read later, newest first.</p>
      <div className="mt-8">
        <BlogFeed
          page={bookmarks}
          label="Bookmarked articles"
          hrefFor={(target) => withParams("/bookmarks", { page: pageQuery(target) })}
          empty={
            <p>
              Nothing saved yet. Use <strong>Save for later</strong> on any article, or{" "}
              <Link href="/">browse the latest</Link>.
            </p>
          }
        />
      </div>
    </div>
  );
}
