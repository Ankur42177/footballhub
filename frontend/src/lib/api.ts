import type { ApiErrorBody } from "@/types/api";

/**
 * Browser-side API client. Requests go to /api/v1 on this origin and are proxied to
 * the Spring Boot API, so the session cookie is sent automatically. Mutations carry
 * the CSRF token from the XSRF-TOKEN cookie in the X-XSRF-TOKEN header.
 */

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, body: Partial<ApiErrorBody> | null) {
    super(body?.message ?? defaultMessage(status));
    this.status = status;
    this.code = body?.code ?? "ERROR";
    this.fieldErrors = Object.fromEntries((body?.fieldErrors ?? []).map((e) => [e.field, e.message]));
  }
}

function defaultMessage(status: number): string {
  if (status === 401) return "Please log in to continue.";
  if (status === 403) return "The server refused this request. Refresh the page and try again.";
  if (status === 404) return "That page or item doesn't exist.";
  if (status === 429) return "Too many requests. Wait a moment and try again.";
  if (status >= 500) return "The server had a problem. Try again in a minute.";
  return "Something went wrong. Try again.";
}

type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

interface RequestOptions {
  method?: Method;
  body?: unknown;
  signal?: AbortSignal;
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.split("; ").find((part) => part.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null;
}

async function refreshCsrfToken(): Promise<void> {
  await fetch("/api/v1/auth/csrf", { credentials: "include", cache: "no-store" });
}

async function send<T>(path: string, options: RequestOptions, allowRetry: boolean): Promise<T> {
  const method = options.method ?? "GET";
  const mutating = method !== "GET";
  if (mutating && !readCookie("XSRF-TOKEN")) {
    await refreshCsrfToken();
  }

  const headers: Record<string, string> = { Accept: "application/json" };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  const token = mutating ? readCookie("XSRF-TOKEN") : null;
  if (token) headers["X-XSRF-TOKEN"] = token;

  let response: Response;
  try {
    response = await fetch(`/api/v1${path}`, {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      credentials: "include",
      cache: "no-store",
      signal: options.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new ApiError(0, {
      code: "NETWORK",
      message: "Couldn't reach the server. Check your connection; if the site was idle it may need a minute to wake up.",
    });
  }

  if (response.status === 204) return undefined as T;
  const body = await response.json().catch(() => null);

  if (!response.ok) {
    const csrfProblem = response.status === 403 && (!body || body.code === "CSRF_INVALID");
    if (mutating && csrfProblem && allowRetry) {
      await refreshCsrfToken();
      return send<T>(path, options, false);
    }
    throw new ApiError(response.status, body);
  }
  return body as T;
}

export function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return send<T>(path, options, true);
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return "Something went wrong. Try again.";
}
