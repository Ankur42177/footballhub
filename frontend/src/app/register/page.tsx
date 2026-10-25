import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { Suspense } from "react";

import { AuthShell } from "@/components/forms/Field";
import { RegisterForm } from "@/components/forms/RegisterForm";
import { safeNextPath } from "@/lib/navigation";
import { getMe } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Create an account",
  description: "Join FootballHub to write Premier League analysis, vote and comment.",
};

export default function RegisterPage(props: PageProps<"/register">) {
  return (
    <AuthShell title="Join FootballHub" intro="Free. Write analysis, upvote the best takes and argue in the comments.">
      <Suspense fallback={<div className="skeleton h-96" aria-hidden="true" />}>
        <RegisterContent searchParams={props.searchParams} />
      </Suspense>
    </AuthShell>
  );
}

async function RegisterContent({ searchParams }: { searchParams: PageProps<"/register">["searchParams"] }) {
  const next = safeNextPath((await searchParams).next);
  if (await getMe().catch(() => null)) redirect(next);
  return <RegisterForm next={next} />;
}
