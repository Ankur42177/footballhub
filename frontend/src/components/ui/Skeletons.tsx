import { BlogFeedSkeleton } from "@/components/blog/BlogFeed";

/** Generic fallback for a page section that is still streaming in. */
export function PageSkeleton({ withFeed = true }: { withFeed?: boolean }) {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6" aria-busy="true">
      <span className="sr-only">Loading…</span>
      <div className="skeleton h-12 w-2/3 max-w-xl" />
      <div className="skeleton mt-4 h-5 w-1/2 max-w-md" />
      {withFeed && (
        <div className="mt-10">
          <BlogFeedSkeleton />
        </div>
      )}
    </div>
  );
}

export function ArticleSkeleton() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6" aria-busy="true">
      <span className="sr-only">Loading the article…</span>
      <div className="skeleton h-4 w-32" />
      <div className="skeleton mt-5 h-14 w-full" />
      <div className="skeleton mt-3 h-14 w-2/3" />
      <div className="skeleton mt-8 h-5 w-1/2" />
      <div className="mt-10 flex flex-col gap-3">
        {Array.from({ length: 8 }, (_, index) => (
          <div key={index} className="skeleton h-4 w-full" />
        ))}
      </div>
    </div>
  );
}
