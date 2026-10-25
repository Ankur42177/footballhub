import type { Metadata } from "next";
import Link from "next/link";

import { AuthShell } from "@/components/forms/Field";
import { ForgotPasswordForm } from "@/components/forms/PasswordResetForms";

export const metadata: Metadata = {
  title: "Reset your password",
  robots: { index: false },
};

export default function ForgotPasswordPage() {
  return (
    <AuthShell
      title="Forgot your password?"
      intro="Enter the email you signed up with and we'll send you a link to choose a new one."
    >
      <ForgotPasswordForm />
      <p className="mt-6 text-[0.95rem] text-chalk-dim">
        Remembered it? <Link href="/login">Log in</Link>
      </p>
    </AuthShell>
  );
}
