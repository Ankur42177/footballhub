"use client";

import { useId, useState } from "react";
import type { FormEvent } from "react";

import { ApiError, errorMessage } from "@/lib/api";

const MAX = 3000;

export function CommentForm({
  label,
  submitLabel,
  initialValue = "",
  autoFocus = false,
  onSubmit,
  onCancel,
}: {
  label: string;
  submitLabel: string;
  initialValue?: string;
  autoFocus?: boolean;
  onSubmit: (content: string) => Promise<void>;
  onCancel?: () => void;
}) {
  const id = useId();
  const [value, setValue] = useState(initialValue);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const content = value.trim();
    if (!content) {
      setError("Write something first.");
      return;
    }
    setPending(true);
    setError(null);
    try {
      await onSubmit(content);
      setValue("");
    } catch (caught) {
      setError(caught instanceof ApiError ? (caught.fieldErrors.content ?? caught.message) : errorMessage(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-2">
      <label htmlFor={id} className="label">
        {label}
      </label>
      <textarea
        id={id}
        value={value}
        onChange={(event) => setValue(event.target.value)}
        maxLength={MAX}
        rows={3}
        autoFocus={autoFocus}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className="input"
      />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <span className="hint">
          {value.length > MAX - 300 ? `${MAX - value.length} characters left` : "Plain text. Be decent."}
        </span>
        <div className="flex gap-2">
          {onCancel && (
            <button type="button" onClick={onCancel} className="btn btn-secondary btn-small">
              Cancel
            </button>
          )}
          <button type="submit" disabled={pending} className="btn btn-primary btn-small">
            {pending ? "Posting…" : submitLabel}
          </button>
        </div>
      </div>
      {error && (
        <p id={`${id}-error`} role="alert" className="field-error">
          {error}
        </p>
      )}
    </form>
  );
}
