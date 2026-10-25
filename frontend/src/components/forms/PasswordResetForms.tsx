"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";

import { Field, FormError } from "@/components/forms/Field";
import { useSession } from "@/components/session/SessionProvider";
import { ApiError, api, errorMessage } from "@/lib/api";

export function ForgotPasswordForm() {
  const [email, setEmail] = useState("");
  const [pending, setPending] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      await api("/auth/forgot-password", { method: "POST", body: { email: email.trim() } });
      setSent(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? (caught.fieldErrors.email ?? caught.message) : errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  if (sent) {
    return (
      <p className="notice notice-success" role="status">
        If an account uses {email.trim()}, we&apos;ve sent it a reset link. It works for one hour.
      </p>
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-5">
      <FormError message={error} />
      <Field
        id="email"
        label="Email"
        type="email"
        autoComplete="email"
        required
        value={email}
        onChange={(event) => setEmail(event.target.value)}
      />
      <button type="submit" disabled={pending} className="btn btn-primary self-start">
        {pending ? "Sending…" : "Send reset link"}
      </button>
    </form>
  );
}

export function ResetPasswordForm({ token }: { token: string | undefined }) {
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!token) {
    return (
      <p className="notice notice-error">
        This reset link is incomplete. <Link href="/forgot-password">Ask for a new one</Link>.
      </p>
    );
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (password.length < 8) {
      setError("Use at least 8 characters.");
      return;
    }
    setPending(true);
    setError(null);
    try {
      await api("/auth/reset-password", { method: "POST", body: { token, newPassword: password } });
      setDone(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? (caught.fieldErrors.newPassword ?? caught.message) : errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  if (done) {
    return (
      <p className="notice notice-success" role="status">
        Your password has been changed. <Link href="/login">Log in with it now</Link>.
      </p>
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-5">
      <FormError message={error} />
      <Field
        id="password"
        label="New password"
        type="password"
        autoComplete="new-password"
        required
        minLength={8}
        maxLength={128}
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        hint="At least 8 characters."
      />
      <button type="submit" disabled={pending} className="btn btn-primary self-start">
        {pending ? "Saving…" : "Set new password"}
      </button>
    </form>
  );
}

/** Confirms the token from the email link as soon as the page opens. */
export function VerifyEmail({ token }: { token: string | undefined }) {
  const { refresh } = useSession();
  const [state, setState] = useState<"working" | "done" | "failed">(token ? "working" : "failed");
  const [message, setMessage] = useState<string | null>(token ? null : "This verification link is incomplete.");
  const started = useRef(false);

  useEffect(() => {
    if (!token || started.current) return;
    started.current = true;
    api("/auth/verify-email", { method: "POST", body: { token } })
      .then(async () => {
        setState("done");
        await refresh();
      })
      .catch((caught) => {
        setState("failed");
        setMessage(errorMessage(caught));
      });
  }, [token, refresh]);

  if (state === "working") return <p role="status">Checking your link…</p>;
  if (state === "done") {
    return (
      <p className="notice notice-success" role="status">
        Thanks, your email address is confirmed. <Link href="/write">Write your first article</Link>.
      </p>
    );
  }
  return (
    <p className="notice notice-error" role="alert">
      {message} Links expire after 48 hours; you can send a new one from <Link href="/settings">your settings</Link>.
    </p>
  );
}
