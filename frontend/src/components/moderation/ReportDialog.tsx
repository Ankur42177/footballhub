"use client";

import { useId, useRef, useState } from "react";
import type { FormEvent } from "react";

import { useAuthAction } from "@/components/session/useAuthAction";
import { api } from "@/lib/api";

/** "Report" link that opens a modal form. Works for an article or a single comment. */
export function ReportDialog({
  target,
  returnTo,
}: {
  target: { blogId: number } | { commentId: number };
  returnTo: string;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const reasonId = useId();
  const [reason, setReason] = useState("");
  const [sent, setSent] = useState(false);
  const { run, pending, error, setError } = useAuthAction(returnTo);
  const what = "blogId" in target ? "article" : "comment";

  function open() {
    run(async () => {
      setError(null);
      dialog.current?.showModal();
    });
  }

  function close() {
    dialog.current?.close();
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    // run() resolves undefined when the request failed (the error is shown in the form).
    const result = await run(() =>
      api<{ id: number }>("/reports", { method: "POST", body: { ...target, reason: reason.trim() } }),
    );
    if (result) {
      setSent(true);
      setReason("");
    }
  }

  return (
    <>
      <button type="button" onClick={open} className="link-action">
        Report{sent ? "ed" : ""}
      </button>
      <dialog
        ref={dialog}
        aria-labelledby={`${reasonId}-title`}
        className="m-auto w-[min(32rem,calc(100vw-2rem))] rounded-panel border border-line bg-pitch-raised p-0 text-chalk"
      >
        <form onSubmit={submit} className="p-6">
          <h2 id={`${reasonId}-title`} className="display-medium text-3xl">
            Report this {what}
          </h2>
          {sent ? (
            <>
              <p className="mt-4">Thanks. A moderator will look at it and take action if it breaks the rules.</p>
              <div className="mt-6 flex justify-end">
                <button type="button" onClick={close} className="btn btn-primary">
                  Close
                </button>
              </div>
            </>
          ) : (
            <>
              <p className="mt-2 text-[0.95rem] text-chalk-dim">
                Tell the moderators what&apos;s wrong, for example abuse, spam or copied work.
              </p>
              <label htmlFor={reasonId} className="label mt-5">
                Reason
              </label>
              <textarea
                id={reasonId}
                required
                maxLength={500}
                rows={4}
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                className="input"
              />
              <p className="hint mt-1 text-right">{reason.length}/500</p>
              {error && (
                <p role="alert" className="field-error">
                  {error}
                </p>
              )}
              <div className="mt-5 flex justify-end gap-3">
                <button type="button" onClick={close} className="btn btn-secondary">
                  Cancel
                </button>
                <button type="submit" disabled={pending || reason.trim().length === 0} className="btn btn-primary">
                  {pending ? "Sending…" : "Send report"}
                </button>
              </div>
            </>
          )}
        </form>
      </dialog>
    </>
  );
}
