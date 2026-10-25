import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6">
      <p className="score text-8xl text-floodlight">404</p>
      <h1 className="display mt-4 text-5xl">Off the pitch</h1>
      <p className="mt-4 text-lg text-chalk-dim">
        That page doesn&apos;t exist, or the article was moved, unpublished or removed.
      </p>
      <div className="mt-8 flex flex-wrap gap-3">
        <Link href="/" className="btn btn-primary">
          Back to the homepage
        </Link>
        <Link href="/search" className="btn btn-secondary">
          Search articles
        </Link>
      </div>
    </div>
  );
}
