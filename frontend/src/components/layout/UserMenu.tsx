"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { UserAvatar } from "@/components/ui/UserAvatar";
import { isStaff, useSession } from "@/components/session/SessionProvider";
import { api } from "@/lib/api";
import { loginHref } from "@/lib/navigation";

export function UserMenu() {
  const { me, setMe } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const container = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!me) return;
    let cancelled = false;
    api<{ count: number }>("/notifications/unread-count")
      .then((result) => {
        if (!cancelled) setUnread(result.count);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [me, pathname]);

  useEffect(() => {
    if (!open) return;
    function onPointer(event: PointerEvent) {
      if (!container.current?.contains(event.target as Node)) setOpen(false);
    }
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("pointerdown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  async function logOut() {
    setOpen(false);
    try {
      await api("/auth/logout", { method: "POST" });
    } finally {
      setMe(null);
      router.push("/");
      router.refresh();
    }
  }

  if (me === undefined) {
    return <div className="ml-auto h-9 w-28 sm:ml-0" aria-hidden="true" />;
  }

  if (me === null) {
    return (
      <div className="ml-auto flex items-center gap-3 sm:ml-0">
        <Link href={loginHref(pathname)} className="text-[0.95rem] text-chalk-dim no-underline hover:text-chalk">
          Log in
        </Link>
        <Link
          href="/register"
          className="rounded-chip bg-floodlight px-4 py-1.5 font-display text-[1.05rem] font-bold text-pitch no-underline hover:bg-floodlight-dim"
        >
          Join
        </Link>
      </div>
    );
  }

  return (
    <div className="ml-auto flex items-center gap-3 sm:ml-0" ref={container}>
      <Link
        href="/write"
        className="hidden rounded-chip bg-floodlight px-4 py-1.5 font-display text-[1.05rem] font-bold text-pitch no-underline hover:bg-floodlight-dim sm:inline-block"
      >
        Write
      </Link>
      <div className="relative">
        <button
          type="button"
          onClick={() => setOpen((value) => !value)}
          aria-expanded={open}
          aria-haspopup="true"
          aria-controls="user-menu"
          className="relative flex items-center rounded-full"
        >
          <span className="sr-only">Open account menu{unread > 0 ? `, ${unread} unread notifications` : ""}</span>
          <UserAvatar name={me.displayName} size={36} />
          {unread > 0 && (
            <span className="absolute -right-0.5 -top-0.5 h-3 w-3 rounded-full border-2 border-pitch-deep bg-floodlight" />
          )}
        </button>
        {open && (
          <div
            id="user-menu"
            className="absolute right-0 top-11 z-30 w-60 rounded-panel border border-line bg-pitch-raised py-2 shadow-[0_12px_32px_rgba(0,0,0,0.45)]"
          >
            <p className="border-b border-line px-4 pb-2 text-sm text-chalk-dim">
              Signed in as <span className="text-chalk">@{me.username}</span>
            </p>
            <MenuLink href="/write" onSelect={() => setOpen(false)}>
              Write an article
            </MenuLink>
            <MenuLink href="/dashboard" onSelect={() => setOpen(false)}>
              Your posts{unread > 0 ? ` and ${unread} new notification${unread === 1 ? "" : "s"}` : ""}
            </MenuLink>
            <MenuLink href="/bookmarks" onSelect={() => setOpen(false)}>
              Bookmarks
            </MenuLink>
            <MenuLink href={`/profile/${me.username}`} onSelect={() => setOpen(false)}>
              Public profile
            </MenuLink>
            <MenuLink href="/settings" onSelect={() => setOpen(false)}>
              Settings
            </MenuLink>
            {isStaff(me) && (
              <MenuLink href="/admin" onSelect={() => setOpen(false)}>
                Moderation
              </MenuLink>
            )}
            <button
              type="button"
              onClick={logOut}
              className="mt-1 block w-full border-t border-line px-4 pt-2 text-left text-[0.95rem] text-chalk-dim hover:text-chalk"
            >
              Log out
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function MenuLink({ href, children, onSelect }: { href: string; children: React.ReactNode; onSelect: () => void }) {
  return (
    <Link
      href={href}
      onClick={onSelect}
      className="block px-4 py-1.5 text-[0.95rem] text-chalk no-underline hover:bg-pitch hover:text-floodlight"
    >
      {children}
    </Link>
  );
}
