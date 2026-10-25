import "server-only";

import { cookies } from "next/headers";

import type { BlogDetail, BlogSummary, Club, Comment, Me, Notification, Page, PublicProfile } from "@/types/api";

/**
 * Server-side reads for Server Components. They call the API directly and forward
 * only the visitor's session cookie, so responses are personalised (drafts for their
 * author, "voted by me", and so on). Every caller must sit inside a <Suspense>
 * boundary because reading cookies is request-time data.
 */

const BACKEND_URL = (process.env.BACKEND_URL ?? "http://localhost:8080").replace(/\/$/, "");

export class BackendUnavailableError extends Error {}

async function backendGet<T>(path: string, { personalised = true } = {}): Promise<T | null> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (personalised) {
    const session = (await cookies()).get("SESSION");
    if (session) headers.Cookie = `SESSION=${session.value}`;
  }

  let response: Response;
  try {
    response = await fetch(`${BACKEND_URL}/api/v1${path}`, { headers, cache: "no-store" });
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
  sort?: "latest" | "top" | "trending" | "oldest";
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
