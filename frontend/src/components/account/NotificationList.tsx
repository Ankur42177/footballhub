"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { safeNextPath } from "@/lib/navigation";
import type { Notification } from "@/types/api";

export function NotificationList({ initial }: { initial: Notification[] }) {
  const router = useRouter();
  const [items, setItems] = useState(initial);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const unread = items.filter((item) => !item.read).length;

  async function markAllRead() {
    setPending(true);
    setError(null);
    try {
      await api("/notifications/read-all", { method: "POST" });
      setItems((list) => list.map((item) => ({ ...item, read: true })));
      router.refresh();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  if (items.length === 0) {
    return <p className="text-chalk-dim">Nothing yet. Replies to you and moderation updates show up here.</p>;
  }

  return (
    <div>
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-chalk-dim">{unread === 0 ? "All caught up." : `${unread} unread`}</p>
        {unread > 0 && (
          <button type="button" onClick={markAllRead} disabled={pending} className="link-action">
            Mark all as read
          </button>
        )}
      </div>
      {error && <p className="field-error">{error}</p>}
      <ul className="mt-3 divide-y divide-line border-y border-line">
        {items.map((item) => {
          // Links come from the API; only follow same-site paths.
          const href = item.link ? safeNextPath(item.link, "") : "";
          return (
            <li key={item.id} className="flex gap-3 py-3">
              <span
                aria-hidden="true"
                className={`mt-2 h-2 w-2 shrink-0 rounded-full ${item.read ? "bg-transparent" : "bg-floodlight"}`}
              />
              <div className="min-w-0">
                <p className={item.read ? "text-chalk-dim" : "text-chalk"}>
                  {href ? (
                    <Link href={href} className="no-underline hover:underline">
                      {item.message}
                    </Link>
                  ) : (
                    item.message
                  )}
                  {!item.read && <span className="sr-only"> (unread)</span>}
                </p>
                <time dateTime={item.createdAt} className="text-xs text-chalk-dim">
                  {formatDateTime(item.createdAt)}
                </time>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
