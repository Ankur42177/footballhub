import type { Metadata } from "next";
import { Suspense } from "react";

import { AuthShell } from "@/components/forms/Field";
import { VerifyEmail } from "@/components/forms/PasswordResetForms";
import { firstParam } from "@/lib/navigation";

export const metadata: Metadata = {
  title: "Confirm your email",
  robots: { index: false },
  referrer: "no-referrer",
};

export default function VerifyEmailPage(props: PageProps<"/verify-email">) {
  return (
    <AuthShell title="Confirm your email">
      <Suspense fallback={<p>Checking your link…</p>}>
        <VerifyContent searchParams={props.searchParams} />
      </Suspense>
    </AuthShell>
  );
}

async function VerifyContent({ searchParams }: { searchParams: PageProps<"/verify-email">["searchParams"] }) {
  const token = firstParam((await searchParams).token);
  return <VerifyEmail token={token} />;
}
