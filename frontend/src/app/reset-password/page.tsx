import type { Metadata } from "next";
import { Suspense } from "react";

import { AuthShell } from "@/components/forms/Field";
import { ResetPasswordForm } from "@/components/forms/PasswordResetForms";
import { firstParam } from "@/lib/navigation";

export const metadata: Metadata = {
  title: "Choose a new password",
  robots: { index: false },
  referrer: "no-referrer",
};

export default function ResetPasswordPage(props: PageProps<"/reset-password">) {
  return (
    <AuthShell title="Choose a new password">
      <Suspense fallback={<div className="skeleton h-40" aria-hidden="true" />}>
        <ResetContent searchParams={props.searchParams} />
      </Suspense>
    </AuthShell>
  );
}

async function ResetContent({ searchParams }: { searchParams: PageProps<"/reset-password">["searchParams"] }) {
  const token = firstParam((await searchParams).token);
  return <ResetPasswordForm token={token} />;
}
