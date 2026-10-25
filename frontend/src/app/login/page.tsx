import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { Suspense } from "react";

import { AuthShell } from "@/components/forms/Field";
import { LoginForm } from "@/components/forms/LoginForm";
import { safeNextPath } from "@/lib/navigation";
import { getMe } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Log in",
  robots: { index: false },
};

export default function LoginPage(props: PageProps<"/login">) {
  return (
    <AuthShell title="Log in" intro="Welcome back. Vote, comment and pick up your drafts where you left them.">
      <Suspense fallback={<div className="skeleton h-64" aria-hidden="true" />}>
        <LoginContent searchParams={props.searchParams} />
      </Suspense>
    </AuthShell>
  );
}

async function LoginContent({ searchParams }: { searchParams: PageProps<"/login">["searchParams"] }) {
  const next = safeNextPath((await searchParams).next);
  if (await getMe().catch(() => null)) redirect(next);
  return <LoginForm next={next} />;
}
