import type { ReactNode } from "react";

import { BlogCard } from "@/components/blog/BlogCard";
import { Pagination } from "@/components/ui/Pagination";
import type { BlogSummary, Page } from "@/types/api";

export function BlogFeed({
  page,
  hrefFor,
  empty,
  showStatus = false,
  label = "Articles",
}: {
  page: Page<BlogSummary>;
  hrefFor: (page: number) => string;
  empty: ReactNode;
  showStatus?: boolean;
  label?: string;
}) {
  if (page.content.length === 0) {
    return <div className="border-t border-line py-10 text-chalk-dim">{empty}</div>;
  }

  return (
    <div>
      <ol className="divide-y divide-line border-y border-line" aria-label={label}>
        {page.content.map((blog) => (
          <li key={blog.id}>
            <BlogCard blog={blog} showStatus={showStatus} />
          </li>
        ))}
      </ol>
      <Pagination page={page.page} totalPages={page.totalPages} hrefFor={hrefFor} label={`${label} pages`} />
    </div>
  );
}

/** Placeholder rows while a feed streams in. */
export function BlogFeedSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <div className="divide-y divide-line border-y border-line" aria-hidden="true">
      {Array.from({ length: rows }, (_, index) => (
        <div key={index} className="py-6 pl-5">
          <div className="skeleton h-3 w-24" />
          <div className="skeleton mt-3 h-7 w-3/4" />
          <div className="skeleton mt-3 h-4 w-1/2" />
        </div>
      ))}
    </div>
  );
}
