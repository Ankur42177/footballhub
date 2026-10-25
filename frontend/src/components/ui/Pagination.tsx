import Link from "next/link";

/**
 * Previous / next links with the current position. `page` is zero-based (as the API
 * returns it); `hrefFor` receives the zero-based page and builds the link.
 */
export function Pagination({
  page,
  totalPages,
  hrefFor,
  label = "Pages",
}: {
  page: number;
  totalPages: number;
  hrefFor: (page: number) => string;
  label?: string;
}) {
  if (totalPages <= 1) return null;
  const hasPrevious = page > 0;
  const hasNext = page < totalPages - 1;

  return (
    <nav aria-label={label} className="mt-8 flex items-center justify-between gap-4 border-t border-line pt-5">
      {hasPrevious ? (
        <Link href={hrefFor(page - 1)} className="btn btn-secondary btn-small" rel="prev">
          Previous
        </Link>
      ) : (
        <span />
      )}
      <p className="text-sm text-chalk-dim">
        Page {page + 1} of {totalPages}
      </p>
      {hasNext ? (
        <Link href={hrefFor(page + 1)} className="btn btn-secondary btn-small" rel="next">
          Next
        </Link>
      ) : (
        <span />
      )}
    </nav>
  );
}

/** Builds `/path?a=1&page=3` from a base path and params, dropping empty values. */
export function withParams(path: string, params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `${path}?${text}` : path;
}

/** Page links use 1-based numbers in the URL; page 1 is left out. */
export function pageQuery(page: number): number | undefined {
  return page > 0 ? page + 1 : undefined;
}
