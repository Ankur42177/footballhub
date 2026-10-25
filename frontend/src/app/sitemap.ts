import type { MetadataRoute } from "next";

import { CLUBS } from "@/lib/clubs";
import { siteUrl } from "@/lib/format";

/**
 * Static on purpose: fetching articles from the API at build time would fail the build
 * whenever the API is asleep. Club pages link to every article, so crawlers still find them.
 */
export default function sitemap(): MetadataRoute.Sitemap {
  const base = siteUrl();
  return [
    { url: `${base}/`, changeFrequency: "hourly", priority: 1 },
    { url: `${base}/clubs`, changeFrequency: "weekly", priority: 0.8 },
    ...CLUBS.map((club) => ({
      url: `${base}/clubs/${club.slug}`,
      changeFrequency: "daily" as const,
      priority: 0.7,
    })),
    { url: `${base}/search`, changeFrequency: "weekly", priority: 0.3 },
  ];
}
