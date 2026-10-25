"use client";

import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import { api, errorMessage } from "@/lib/api";
import { formatDate, formatDateTime } from "@/lib/format";
import type { AdminUser, AuditLogEntry, Page, Role } from "@/types/api";

const ROLES: Role[] = ["USER", "MODERATOR", "ADMIN"];

/** Admin only: find users, change roles, suspend accounts. */
export function AdminUsersTable({ currentUserId }: { currentUserId: number }) {
  const [query, setQuery] = useState("");
  const [submitted, setSubmitted] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AdminUser> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    const q = submitted ? `&q=${encodeURIComponent(submitted)}` : "";
    api<Page<AdminUser>>(`/admin/users?page=${page}&size=20${q}`, { signal: controller.signal })
      .then((result) => {
        setData(result);
        setError(null);
      })
      .catch((caught) => {
        if (!controller.signal.aborted) setError(errorMessage(caught));
      });
    return () => controller.abort();
  }, [submitted, page]);

  function search(event: FormEvent) {
    event.preventDefault();
    setPage(0);
    setSubmitted(query.trim());
  }

  function replace(updated: AdminUser) {
    setData((current) =>
      current ? { ...current, content: current.content.map((user) => (user.id === updated.id ? updated : user)) } : current,
    );
  }

  return (
    <div>
      <form onSubmit={search} role="search" className="flex max-w-md gap-2">
        <label htmlFor="user-search" className="sr-only">
          Search users
        </label>
        <input
          id="user-search"
          type="search"
          className="input"
          placeholder="Username, name or email"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <button type="submit" className="btn btn-secondary">
          Search
        </button>
      </form>

      {error && <p className="notice notice-error mt-5">{error}</p>}
      {!data && !error && <p className="mt-5 text-chalk-dim">Loading users…</p>}
      {data && data.content.length === 0 && <p className="mt-5 text-chalk-dim">No users match.</p>}

      {data && data.content.length > 0 && (
        <div className="mt-5 overflow-x-auto">
          <table className="w-full min-w-[40rem] text-left text-[0.95rem]">
            <thead className="border-b border-line text-sm text-chalk-dim">
              <tr>
                <th scope="col" className="py-2 pr-4 font-semibold">User</th>
                <th scope="col" className="py-2 pr-4 font-semibold">Joined</th>
                <th scope="col" className="py-2 pr-4 font-semibold">Role</th>
                <th scope="col" className="py-2 font-semibold">Account</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {data.content.map((user) => (
                <UserRow key={user.id} user={user} isSelf={user.id === currentUserId} onChanged={replace} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-5 flex items-center justify-between">
          <button type="button" className="btn btn-secondary btn-small" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </button>
          <span className="text-sm text-chalk-dim">
            Page {page + 1} of {data.totalPages}
          </span>
          <button type="button" className="btn btn-secondary btn-small" disabled={!data.hasNext} onClick={() => setPage(page + 1)}>
            Next
          </button>
        </div>
      )}
    </div>
  );
}

function UserRow({ user, isSelf, onChanged }: { user: AdminUser; isSelf: boolean; onChanged: (user: AdminUser) => void }) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function act(request: () => Promise<AdminUser>) {
    setPending(true);
    setError(null);
    try {
      onChanged(await request());
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <tr className="align-top">
      <td className="py-3 pr-4">
        <p className="font-semibold">{user.displayName}</p>
        <p className="text-sm text-chalk-dim">
          @{user.username}, {user.email}
          {!user.emailVerified && " (unverified)"}
        </p>
        {error && <p className="field-error">{error}</p>}
      </td>
      <td className="py-3 pr-4 text-sm text-chalk-dim">{formatDate(user.createdAt)}</td>
      <td className="py-3 pr-4">
        <label htmlFor={`role-${user.id}`} className="sr-only">
          Role for {user.username}
        </label>
        <select
          id={`role-${user.id}`}
          className="input py-1"
          value={user.role}
          disabled={pending || isSelf}
          onChange={(event) =>
            act(() => api<AdminUser>(`/admin/users/${user.id}/role`, { method: "PATCH", body: { role: event.target.value } }))
          }
        >
          {ROLES.map((role) => (
            <option key={role} value={role}>
              {role === "USER" ? "Member" : role === "MODERATOR" ? "Moderator" : "Admin"}
            </option>
          ))}
        </select>
      </td>
      <td className="py-3">
        {isSelf ? (
          <span className="text-sm text-chalk-dim">That&apos;s you</span>
        ) : (
          <button
            type="button"
            disabled={pending}
            onClick={() => {
              if (user.enabled && !window.confirm(`Suspend @${user.username}? They won't be able to post or log in.`)) return;
              act(() =>
                api<AdminUser>(`/admin/users/${user.id}/status`, { method: "PATCH", body: { enabled: !user.enabled } }),
              );
            }}
            className={`btn btn-small ${user.enabled ? "btn-danger" : "btn-secondary"}`}
          >
            {user.enabled ? "Suspend" : "Reinstate"}
          </button>
        )}
      </td>
    </tr>
  );
}

/** Admin only: the record of moderation and administration actions. */
export function AuditLogTable() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AuditLogEntry> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    api<Page<AuditLogEntry>>(`/admin/audit-logs?page=${page}&size=30`, { signal: controller.signal })
      .then((result) => {
        setData(result);
        setError(null);
      })
      .catch((caught) => {
        if (!controller.signal.aborted) setError(errorMessage(caught));
      });
    return () => controller.abort();
  }, [page]);

  if (error) return <p className="notice notice-error">{error}</p>;
  if (!data) return <p className="text-chalk-dim">Loading the audit log…</p>;
  if (data.content.length === 0) return <p className="text-chalk-dim">No actions recorded yet.</p>;

  return (
    <div>
      <ul className="divide-y divide-line border-y border-line text-[0.95rem]">
        {data.content.map((entry) => (
          <li key={entry.id} className="grid gap-1 py-3 sm:grid-cols-[11rem_1fr]">
            <time dateTime={entry.createdAt} className="text-sm text-chalk-dim">
              {formatDateTime(entry.createdAt)}
            </time>
            <p>
              <span className="font-semibold">@{entry.actor}</span> {humanAction(entry.action)}{" "}
              <span className="text-chalk-dim">
                {entry.targetType.toLowerCase()}
                {entry.targetId !== null ? ` #${entry.targetId}` : ""}
              </span>
              {entry.details && <span className="block text-sm text-chalk-dim">{entry.details}</span>}
            </p>
          </li>
        ))}
      </ul>
      {data.totalPages > 1 && (
        <div className="mt-5 flex items-center justify-between">
          <button type="button" className="btn btn-secondary btn-small" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Newer
          </button>
          <span className="text-sm text-chalk-dim">
            Page {page + 1} of {data.totalPages}
          </span>
          <button type="button" className="btn btn-secondary btn-small" disabled={!data.hasNext} onClick={() => setPage(page + 1)}>
            Older
          </button>
        </div>
      )}
    </div>
  );
}

function humanAction(action: string): string {
  const text = action.toLowerCase().replaceAll("_", " ");
  return text.charAt(0) + text.slice(1);
}
