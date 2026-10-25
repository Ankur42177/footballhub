import type { InputHTMLAttributes, ReactNode } from "react";

/** Labelled input with its hint or error directly underneath. */
export function Field({
  id,
  label,
  error,
  hint,
  ...input
}: {
  id: string;
  label: ReactNode;
  error?: string;
  hint?: ReactNode;
} & InputHTMLAttributes<HTMLInputElement>) {
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;
  return (
    <div>
      <label htmlFor={id} className="label">
        {label}
      </label>
      <input
        id={id}
        name={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className="input"
        {...input}
      />
      {error ? (
        <p id={`${id}-error`} className="field-error">
          {error}
        </p>
      ) : hint ? (
        <p id={`${id}-hint`} className="hint mt-1">
          {hint}
        </p>
      ) : null}
    </div>
  );
}

export function FormError({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p role="alert" className="notice notice-error">
      {message}
    </p>
  );
}

/** Narrow centred column used by the account pages. */
export function AuthShell({ title, intro, children }: { title: string; intro?: ReactNode; children: ReactNode }) {
  return (
    <div className="mx-auto w-full max-w-md px-4 py-12 sm:py-16">
      <h1 className="display text-5xl sm:text-6xl">{title}</h1>
      {intro && <div className="mt-3 text-chalk-dim">{intro}</div>}
      <div className="mt-8">{children}</div>
    </div>
  );
}
