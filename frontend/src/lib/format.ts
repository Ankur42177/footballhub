const dateFormat = new Intl.DateTimeFormat("en-GB", {
  day: "numeric",
  month: "short",
  year: "numeric",
  timeZone: "UTC",
});

const dateTimeFormat = new Intl.DateTimeFormat("en-GB", {
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "UTC",
});

/** "12 Oct 2026". Dates come from the API as ISO strings. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  return dateFormat.format(new Date(iso));
}

/** "12 Oct, 18:40 UTC" for moderation and notification lists. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "";
  return `${dateTimeFormat.format(new Date(iso))} UTC`;
}

export function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
  return `${count.toLocaleString("en-GB")} ${count === 1 ? singular : pluralForm}`;
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  const letters = parts.length === 1 ? parts[0].slice(0, 2) : parts[0][0] + parts[parts.length - 1][0];
  return letters.toUpperCase();
}

export function siteUrl(): string {
  return (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, "");
}
