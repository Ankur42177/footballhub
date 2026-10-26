import "server-only";

import { cacheLife } from "next/cache";
import { cookies } from "next/headers";

import type { BlogDetail, BlogSummary, Club, Comment, Me, Notification, Page, PublicProfile } from "@/types/api";

/**
 * Server-side reads for Server Components. They call the API directly and forward only the
 * visitor's session cookie, so signed-in visitors get personalised responses (their drafts,
 * "voted by me", and so on). Every caller must sit inside a <Suspense> boundary because
 * reading cookies is request-time data.
 *
 * Visitors who aren't signed in get the same data as everyone else, so those reads go
 * through a shared remote cache ('use cache: remote'): pages load from the cache instantly
 * and the cache refreshes in the background, even while the free-plan API is asleep.
 */

const BACKEND_URL = (process.env.BACKEND_URL ?? "http://localhost:8080").replace(/\/$/, "");

const TIMEOUT_MS = 12_000;

export class BackendUnavailableError extends Error {}

async function backendGet<T>(path: string, { personalised = true } = {}): Promise<T | null> {
  const session = personalised ? (await cookies()).get("SESSION") : undefined;
  if (!session) return publicGet<T>(path);
  return fetchFromApi<T>(path, `SESSION=${session.value}`);
}

/** Anonymous reads, shared by every visitor: serve cached data, refresh in the background. */
async function publicGet<T>(path: string): Promise<T | null> {
  "use cache: remote";
  // Fresh for a minute, then served stale while refreshing; kept for a week, so a sleeping
  // API never blanks a page that has been viewed before.
  cacheLife({ stale: 60, revalidate: 60, expire: 60 * 60 * 24 * 7 });
  return fetchFromApi<T>(path);
}

async function fetchFromApi<T>(path: string, cookie?: string): Promise<T | null> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (cookie) headers.Cookie = cookie;

  let response: Response;
  try {
    // Fail fast instead of hanging while the API is asleep or starting; error.tsx then
    // shows a "starting up" screen that retries on its own.
    response = await fetch(`${BACKEND_URL}/api/v1${path}`, {
      headers,
      cache: "no-store",
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
  } catch {
    throw new BackendUnavailableError("The FootballHub API could not be reached.");
  }

  if (response.status === 404 || response.status === 401 || response.status === 403) return null;
  if (!response.ok) {
    throw new BackendUnavailableError(`The FootballHub API answered ${response.status}.`);
  }
  return (await response.json()) as T;
}

function query(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}

export function getMe(): Promise<Me | null> {
  return backendGet<Me>("/users/me");
}

export interface FeedParams {
  q?: string;
  club?: string;
  author?: string;
  sort?: "featured" | "latest" | "top" | "trending" | "oldest";
  page?: number;
  size?: number;
}

export async function getFeed(params: FeedParams): Promise<Page<BlogSummary>> {
  const page = await backendGet<Page<BlogSummary>>(`/blogs${query({ ...params })}`);
  return page ?? emptyPage();
}

export function getBlog(slug: string): Promise<BlogDetail | null> {
  return backendGet<BlogDetail>(`/blogs/${encodeURIComponent(slug)}`);
}

/** Anonymous read used for metadata, so private drafts never leak into <head>. */
export function getPublicBlog(slug: string): Promise<BlogDetail | null> {
  return backendGet<BlogDetail>(`/blogs/${encodeURIComponent(slug)}`, { personalised: false });
}

export async function getComments(blogId: number): Promise<Page<Comment>> {
  const page = await backendGet<Page<Comment>>(`/blogs/${blogId}/comments?size=20`);
  return page ?? emptyPage();
}

export async function getClubs(): Promise<Club[]> {
  return (await backendGet<Club[]>("/clubs")) ?? [];
}

export function getClub(slug: string): Promise<Club | null> {
  return backendGet<Club>(`/clubs/${encodeURIComponent(slug)}`);
}

export function getProfile(username: string): Promise<PublicProfile | null> {
  return backendGet<PublicProfile>(`/profiles/${encodeURIComponent(username)}`);
}

export async function getMyBlogs(status: string | undefined, page: number): Promise<Page<BlogSummary>> {
  return (await backendGet<Page<BlogSummary>>(`/users/me/blogs${query({ status, page, size: 20 })}`)) ?? emptyPage();
}

export async function getMyBookmarks(page: number): Promise<Page<BlogSummary>> {
  return (await backendGet<Page<BlogSummary>>(`/users/me/bookmarks${query({ page, size: 20 })}`)) ?? emptyPage();
}

export async function getMyClubs(): Promise<Club[]> {
  return (await backendGet<Club[]>("/users/me/clubs")) ?? [];
}

export async function getNotifications(page = 0): Promise<Page<Notification>> {
  return (await backendGet<Page<Notification>>(`/notifications${query({ page, size: 20 })}`)) ?? emptyPage();
}

function emptyPage<T>(): Page<T> {
  return { content: [], page: 0, size: 0, totalElements: 0, totalPages: 0, hasNext: false };
}
