import type { Metadata } from "next";

import { ReviewFromLink } from "@/components/review/ReviewFromLink";

export const metadata: Metadata = {
  title: "Review a story",
  robots: { index: false, follow: false },
  referrer: "no-referrer",
};

export default function ReviewPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <h1 className="display text-5xl sm:text-6xl">Review a story</h1>
      <p className="mt-3 text-chalk-dim">Read the submission, then approve it to publish or reject it with a note.</p>
      <div className="mt-8">
        <ReviewFromLink />
      </div>
    </div>
  );
}
