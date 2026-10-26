"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { FormEvent } from "react";

import { Field, FormError } from "@/components/forms/Field";
import { GoogleSignIn } from "@/components/forms/GoogleSignIn";
import { useSession } from "@/components/session/SessionProvider";
import { ApiError, api, errorMessage } from "@/lib/api";
import type { Me } from "@/types/api";

export function LoginForm({ next }: { next: string }) {
  const router = useRouter();
  const { setMe } = useSession();
  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function submit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    setFieldErrors({});
    try {
      const me = await api<Me>("/auth/login", { method: "POST", body: { login: login.trim(), password } });
      setMe(me);
      router.replace(next);
      router.refresh();
    } catch (caught) {
      if (caught instanceof ApiError) {
        setFieldErrors(caught.fieldErrors);
        setError(caught.status === 401 ? "That username or password doesn't match an account." : caught.message);
      } else {
        setError(errorMessage(caught));
      }
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-5">
      <GoogleSignIn next={next} />
      <FormError message={error} />
      <Field
        id="login"
        label="Email or username"
        autoComplete="username"
        required
        value={login}
        onChange={(event) => setLogin(event.target.value)}
        error={fieldErrors.login}
      />
      <Field
        id="password"
        label="Password"
        type="password"
        autoComplete="current-password"
        required
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        error={fieldErrors.password}
      />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button type="submit" disabled={pending} className="btn btn-primary">
          {pending ? "Logging in…" : "Log in"}
        </button>
        <Link href="/forgot-password" className="text-sm text-chalk-dim">
          Forgot your password?
        </Link>
      </div>
      <p className="border-t border-line pt-5 text-[0.95rem] text-chalk-dim">
        New here?{" "}
        <Link href={next === "/" ? "/register" : `/register?next=${encodeURIComponent(next)}`}>Create an account</Link>
      </p>
    </form>
  );
}
