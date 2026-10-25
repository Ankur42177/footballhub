"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import type { FormEvent } from "react";

import { Field, FormError } from "@/components/forms/Field";
import { useSession } from "@/components/session/SessionProvider";
import { ApiError, api, errorMessage } from "@/lib/api";
import type { Me } from "@/types/api";

export function ProfileForm({ me }: { me: Me }) {
  const router = useRouter();
  const { setMe } = useSession();
  const [displayName, setDisplayName] = useState(me.displayName);
  const [bio, setBio] = useState(me.bio ?? "");
  const [pending, setPending] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function submit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setSaved(false);
    setError(null);
    setFieldErrors({});
    try {
      const updated = await api<Me>("/users/me", {
        method: "PATCH",
        body: { displayName: displayName.trim(), bio: bio.trim() || null },
      });
      setMe(updated);
      setSaved(true);
      router.refresh();
    } catch (caught) {
      if (caught instanceof ApiError) setFieldErrors(caught.fieldErrors);
      setError(errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-5">
      <FormError message={error} />
      <Field
        id="displayName"
        label="Display name"
        required
        maxLength={80}
        value={displayName}
        onChange={(event) => setDisplayName(event.target.value)}
        error={fieldErrors.displayName}
      />
      <div>
        <label htmlFor="bio" className="label">
          Bio <span className="font-normal text-chalk-dim">(optional)</span>
        </label>
        <textarea
          id="bio"
          rows={4}
          maxLength={500}
          value={bio}
          onChange={(event) => setBio(event.target.value)}
          aria-invalid={fieldErrors.bio ? true : undefined}
          className="input"
        />
        <p className={fieldErrors.bio ? "field-error" : "hint mt-1"}>
          {fieldErrors.bio ?? `Shown on your public profile. ${bio.length}/500`}
        </p>
      </div>
      <div className="flex items-center gap-4">
        <button type="submit" disabled={pending} className="btn btn-primary">
          {pending ? "Saving…" : "Save profile"}
        </button>
        {saved && (
          <span role="status" className="text-sm text-success">
            Saved.
          </span>
        )}
      </div>
    </form>
  );
}

export function PasswordForm() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (newPassword.length < 8) {
      setFieldErrors({ newPassword: "Use at least 8 characters." });
      return;
    }
    setPending(true);
    setDone(false);
    setError(null);
    setFieldErrors({});
    try {
      await api("/users/me/password", { method: "POST", body: { currentPassword, newPassword } });
      setDone(true);
      setCurrentPassword("");
      setNewPassword("");
    } catch (caught) {
      if (caught instanceof ApiError) setFieldErrors(caught.fieldErrors);
      setError(errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-5">
      <FormError message={error} />
      <Field
        id="currentPassword"
        label="Current password"
        type="password"
        autoComplete="current-password"
        required
        value={currentPassword}
        onChange={(event) => setCurrentPassword(event.target.value)}
        error={fieldErrors.currentPassword}
      />
      <Field
        id="newPassword"
        label="New password"
        type="password"
        autoComplete="new-password"
        required
        minLength={8}
        maxLength={128}
        value={newPassword}
        onChange={(event) => setNewPassword(event.target.value)}
        error={fieldErrors.newPassword}
        hint="At least 8 characters."
      />
      <div className="flex items-center gap-4">
        <button type="submit" disabled={pending} className="btn btn-secondary">
          {pending ? "Changing…" : "Change password"}
        </button>
        {done && (
          <span role="status" className="text-sm text-success">
            Password changed.
          </span>
        )}
      </div>
    </form>
  );
}

export function ResendVerification() {
  const [state, setState] = useState<"idle" | "pending" | "sent">("idle");
  const [error, setError] = useState<string | null>(null);

  async function resend() {
    setState("pending");
    setError(null);
    try {
      await api("/users/me/resend-verification", { method: "POST" });
      setState("sent");
    } catch (caught) {
      setError(errorMessage(caught));
      setState("idle");
    }
  }

  return (
    <div className="notice flex flex-col gap-2">
      <p>Your email address isn&apos;t confirmed yet. Check your inbox for the link we sent when you signed up.</p>
      {state === "sent" ? (
        <p role="status" className="text-success">
          A new link is on its way.
        </p>
      ) : (
        <button type="button" onClick={resend} disabled={state === "pending"} className="btn btn-secondary btn-small self-start">
          {state === "pending" ? "Sending…" : "Send a new link"}
        </button>
      )}
      {error && <p className="field-error">{error}</p>}
    </div>
  );
}
