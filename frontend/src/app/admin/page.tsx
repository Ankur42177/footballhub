import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { AdminModerationTable } from "@/components/admin/AdminModerationTable";
import { AdminReviewQueue } from "@/components/admin/AdminReviewQueue";
import { AdminUsersTable, AuditLogTable } from "@/components/admin/AdminUsersTable";
import { PageSkeleton } from "@/components/ui/Skeletons";
import { isStaffRole, requireMe } from "@/lib/auth";
import { firstParam } from "@/lib/navigation";

export const metadata: Metadata = {
  title: "Moderation",
  robots: { index: false },
};

export default function AdminPage(props: PageProps<"/admin">) {
  return (
    <Suspense fallback={<PageSkeleton withFeed={false} />}>
      <Admin searchParams={props.searchParams} />
    </Suspense>
  );
}

async function Admin({ searchParams }: { searchParams: PageProps<"/admin">["searchParams"] }) {
  const requested = firstParam((await searchParams).tab);
  const me = await requireMe("/admin");

  // UI gate only; every /api/v1/admin call is checked again by the server.
  if (!isStaffRole(me)) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <h1 className="display text-5xl">Staff only</h1>
        <p className="mt-4 text-chalk-dim">
          This area is for moderators and administrators. <Link href="/">Back to the homepage</Link>.
        </p>
      </div>
    );
  }

  const isAdmin = me.role === "ADMIN";
  const tabs = [
    { value: "reports", label: "Reports" },
    ...(isAdmin
      ? [
          { value: "reviews", label: "Awaiting review" },
          { value: "users", label: "Users" },
          { value: "audit", label: "Audit log" },
        ]
      : []),
  ];
  const tab = tabs.some((item) => item.value === requested) ? requested : "reports";

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <h1 className="display text-5xl sm:text-6xl">Moderation</h1>
      <p className="mt-2 text-chalk-dim">
        Signed in as {isAdmin ? "an administrator" : "a moderator"}. Every action here is recorded in the audit log.
      </p>
      <nav aria-label="Moderation sections" className="mt-8 flex gap-1 border-b border-line">
        {tabs.map((item) => (
          <Link
            key={item.value}
            href={item.value === "reports" ? "/admin" : `/admin?tab=${item.value}`}
            aria-current={item.value === tab ? "page" : undefined}
            className={`-mb-px border-b-2 px-4 py-2 font-display text-xl font-bold no-underline ${
              item.value === tab ? "border-floodlight text-chalk" : "border-transparent text-chalk-dim hover:text-chalk"
            }`}
          >
            {item.label}
          </Link>
        ))}
      </nav>
      <div className="mt-6">
        {tab === "reports" && <AdminModerationTable />}
        {tab === "reviews" && <AdminReviewQueue />}
        {tab === "users" && <AdminUsersTable currentUserId={me.id} />}
        {tab === "audit" && <AuditLogTable />}
      </div>
    </div>
  );
}
