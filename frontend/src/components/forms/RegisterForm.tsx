"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { FormEvent } from "react";

import { Field, FormError } from "@/components/forms/Field";
import { useSession } from "@/components/session/SessionProvider";
import { ApiError, api, errorMessage } from "@/lib/api";
import { loginHref } from "@/lib/navigation";
import type { Me } from "@/types/api";

interface Values {
  username: string;
  displayName: string;
  email: string;
  password: string;
}

function validate(values: Values): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!/^[A-Za-z0-9_]{3,30}$/.test(values.username)) {
    errors.username = "Use 3 to 30 letters, numbers or underscores.";
  }
  if (!values.displayName.trim()) errors.displayName = "Tell readers what to call you.";
  if (!/^\S+@\S+\.\S+$/.test(values.email)) errors.email = "Enter a valid email address.";
  if (values.password.length < 8) errors.password = "Use at least 8 characters.";
  return errors;
}

export function RegisterForm({ next }: { next: string }) {
  const router = useRouter();
  const { setMe } = useSession();
  const [values, setValues] = useState<Values>({ username: "", displayName: "", email: "", password: "" });
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function set(key: keyof Values, value: string) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const localErrors = validate(values);
    setFieldErrors(localErrors);
    if (Object.keys(localErrors).length > 0) {
      setError("Fix the highlighted fields.");
      return;
    }
    setPending(true);
    setError(null);
    try {
      const me = await api<Me>("/auth/register", {
        method: "POST",
        body: { ...values, username: values.username.trim(), email: values.email.trim(), displayName: values.displayName.trim() },
      });
      setMe(me);
      router.replace(next);
      router.refresh();
    } catch (caught) {
      if (caught instanceof ApiError) {
        setFieldErrors(caught.fieldErrors);
        setError(caught.message);
      } else {
        setError(errorMessage(caught));
      }
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-5">
      <FormError message={error} />
      <Field
        id="username"
        label="Username"
        autoComplete="username"
        required
        maxLength={30}
        value={values.username}
        onChange={(event) => set("username", event.target.value)}
        error={fieldErrors.username}
        hint="Letters, numbers and underscores. This is your profile address."
      />
      <Field
        id="displayName"
        label="Display name"
        autoComplete="name"
        required
        maxLength={80}
        value={values.displayName}
        onChange={(event) => set("displayName", event.target.value)}
        error={fieldErrors.displayName}
        hint="Shown on your articles and comments."
      />
      <Field
        id="email"
        label="Email"
        type="email"
        autoComplete="email"
        required
        value={values.email}
        onChange={(event) => set("email", event.target.value)}
        error={fieldErrors.email}
        hint="We'll send a link to confirm it. Never shown publicly."
      />
      <Field
        id="password"
        label="Password"
        type="password"
        autoComplete="new-password"
        required
        minLength={8}
        maxLength={128}
        value={values.password}
        onChange={(event) => set("password", event.target.value)}
        error={fieldErrors.password}
        hint="At least 8 characters."
      />
      <button type="submit" disabled={pending} className="btn btn-primary self-start">
        {pending ? "Creating your account…" : "Create account"}
      </button>
      <p className="border-t border-line pt-5 text-[0.95rem] text-chalk-dim">
        Already have an account? <Link href={loginHref(next)}>Log in</Link>
      </p>
    </form>
  );
}
