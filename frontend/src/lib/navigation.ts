/** Only allow same-site relative paths as post-login destinations (no open redirects). */
export function safeNextPath(value: string | string[] | undefined | null, fallback = "/"): string {
  const path = Array.isArray(value) ? value[0] : value;
  if (!path || !path.startsWith("/") || path.startsWith("//") || path.startsWith("/\\")) return fallback;
  return path;
}

export function loginHref(next: string): string {
  return `/login?next=${encodeURIComponent(next)}`;
}

export function firstParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export function pageParam(value: string | string[] | undefined): number {
  const parsed = Number.parseInt(firstParam(value) ?? "", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed - 1 : 0;
}
