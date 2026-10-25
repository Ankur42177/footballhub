"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import { Markdown } from "@/components/blog/Markdown";
import { ApiError, api, errorMessage } from "@/lib/api";
import type { BlogDetail, BlogInput, Club } from "@/types/api";

const TITLE_MAX = 180;
const EXCERPT_MAX = 400;

type Intent = "draft" | "publish";

/**
 * Markdown editor for new articles (`existing` undefined) and edits. "Save" keeps the
 * current status; "Publish" saves first, then publishes.
 */
export function BlogEditor({ clubs, existing }: { clubs: Pick<Club, "id" | "name">[]; existing?: BlogDetail }) {
  const router = useRouter();
  const [values, setValues] = useState<BlogInput>({
    title: existing?.title ?? "",
    excerpt: existing?.excerpt ?? "",
    content: existing?.content ?? "",
    clubId: existing?.club?.id ?? null,
  });
  const [tab, setTab] = useState<"write" | "preview">("write");
  const [pending, setPending] = useState<Intent | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [dirty, setDirty] = useState(false);
  const published = existing?.status === "PUBLISHED";

  useEffect(() => {
    if (!dirty) return;
    function warn(event: BeforeUnloadEvent) {
      event.preventDefault();
    }
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  function update<K extends keyof BlogInput>(key: K, value: BlogInput[K]) {
    setValues((current) => ({ ...current, [key]: value }));
    setDirty(true);
    if (fieldErrors[key]) {
      setFieldErrors((current) => {
        const next = { ...current };
        delete next[key];
        return next;
      });
    }
  }

  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};
    if (!values.title.trim()) errors.title = "Give your article a headline.";
    if (!values.content.trim()) errors.content = "The article needs some words.";
    return errors;
  }

  async function save(intent: Intent) {
    const localErrors = validate();
    setFieldErrors(localErrors);
    if (Object.keys(localErrors).length > 0) {
      setError("Fix the highlighted fields.");
      return;
    }
    setPending(intent);
    setError(null);
    const body = {
      title: values.title.trim(),
      excerpt: values.excerpt.trim() || null,
      content: values.content,
      clubId: values.clubId,
    };
    try {
      let saved: BlogDetail;
      if (existing) {
        saved = await api<BlogDetail>(`/blogs/${existing.id}`, { method: "PUT", body });
        if (intent === "publish" && saved.status !== "PUBLISHED") {
          saved = await api<BlogDetail>(`/blogs/${existing.id}/publish`, { method: "POST" });
        }
      } else {
        saved = await api<BlogDetail>("/blogs", { method: "POST", body: { ...body, publish: intent === "publish" } });
      }
      setDirty(false);
      router.push(`/blogs/${saved.slug}`);
      router.refresh();
    } catch (caught) {
      if (caught instanceof ApiError) {
        setFieldErrors(caught.fieldErrors);
        setError(
          caught.code === "EMAIL_NOT_VERIFIED"
            ? "Verify your email address before publishing. You can save this as a draft in the meantime."
            : caught.message,
        );
      } else {
        setError(errorMessage(caught));
      }
      setPending(null);
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    save(published ? "publish" : "draft");
  }

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-6">
      <div>
        <label htmlFor="title" className="label">
          Headline
        </label>
        <input
          id="title"
          value={values.title}
          onChange={(event) => update("title", event.target.value)}
          maxLength={TITLE_MAX}
          required
          aria-invalid={fieldErrors.title ? true : undefined}
          aria-describedby="title-help"
          className="input display-medium text-2xl sm:text-3xl"
        />
        <p id="title-help" className={fieldErrors.title ? "field-error" : "hint mt-1"}>
          {fieldErrors.title ?? `${values.title.length}/${TITLE_MAX}`}
        </p>
      </div>

      <div className="grid gap-6 sm:grid-cols-[minmax(0,16rem)_1fr]">
        <div>
          <label htmlFor="club" className="label">
            Club
          </label>
          <select
            id="club"
            value={values.clubId ?? ""}
            onChange={(event) => update("clubId", event.target.value ? Number(event.target.value) : null)}
            aria-invalid={fieldErrors.clubId ? true : undefined}
            className="input"
          >
            <option value="">No specific club</option>
            {clubs.map((club) => (
              <option key={club.id} value={club.id}>
                {club.name}
              </option>
            ))}
          </select>
          {fieldErrors.clubId && <p className="field-error">{fieldErrors.clubId}</p>}
        </div>
        <div>
          <label htmlFor="excerpt" className="label">
            Summary <span className="font-normal text-chalk-dim">(optional)</span>
          </label>
          <textarea
            id="excerpt"
            value={values.excerpt}
            onChange={(event) => update("excerpt", event.target.value)}
            maxLength={EXCERPT_MAX}
            rows={2}
            aria-invalid={fieldErrors.excerpt ? true : undefined}
            aria-describedby="excerpt-help"
            className="input"
          />
          <p id="excerpt-help" className={fieldErrors.excerpt ? "field-error" : "hint mt-1"}>
            {fieldErrors.excerpt ?? `Shown in feeds and search results. ${values.excerpt.length}/${EXCERPT_MAX}`}
          </p>
        </div>
      </div>

      <div>
        <div className="flex items-end justify-between gap-3">
          <label htmlFor="content" className="label mb-0">
            Article
          </label>
          <div role="tablist" aria-label="Editor view" className="flex gap-1">
            {(["write", "preview"] as const).map((name) => (
              <button
                key={name}
                type="button"
                role="tab"
                aria-selected={tab === name}
                aria-controls={`panel-${name}`}
                onClick={() => setTab(name)}
                className={`rounded-t-panel px-3 py-1 text-sm ${
                  tab === name ? "bg-pitch-raised text-chalk" : "text-chalk-dim hover:text-chalk"
                }`}
              >
                {name === "write" ? "Write" : "Preview"}
              </button>
            ))}
          </div>
        </div>
        <div id="panel-write" role="tabpanel" hidden={tab !== "write"} className="mt-1">
          <textarea
            id="content"
            value={values.content}
            onChange={(event) => update("content", event.target.value)}
            rows={22}
            required
            aria-invalid={fieldErrors.content ? true : undefined}
            aria-describedby="content-help"
            className="input font-mono text-[0.95rem] leading-relaxed"
            placeholder={"## The press that changed the game\n\nWrite in Markdown: **bold**, _italic_, lists, > quotes and tables."}
          />
        </div>
        <div
          id="panel-preview"
          role="tabpanel"
          hidden={tab !== "preview"}
          className="mt-1 min-h-[24rem] rounded-panel border border-line bg-pitch-deep p-5"
        >
          {values.content.trim() ? (
            <Markdown source={values.content} />
          ) : (
            <p className="text-chalk-dim">Nothing to preview yet.</p>
          )}
        </div>
        <p id="content-help" className={fieldErrors.content ? "field-error" : "hint mt-1"}>
          {fieldErrors.content ?? "Markdown is supported. Images only from Wikimedia Commons: ![alt](url \"Caption||Photo: Name, licence||Commons file page\"). No HTML."}
        </p>
      </div>

      {error && (
        <p role="alert" className="notice notice-error">
          {error}
        </p>
      )}

      <div className="flex flex-wrap items-center gap-3 border-t border-line pt-5">
        {published ? (
          <button type="submit" disabled={pending !== null} className="btn btn-primary">
            {pending ? "Saving…" : "Save changes"}
          </button>
        ) : (
          <>
            <button type="button" onClick={() => save("publish")} disabled={pending !== null} className="btn btn-primary">
              {pending === "publish" ? "Publishing…" : "Publish"}
            </button>
            <button type="submit" disabled={pending !== null} className="btn btn-secondary">
              {pending === "draft" ? "Saving…" : existing ? "Save draft" : "Save as draft"}
            </button>
          </>
        )}
        {existing && (
          <button
            type="button"
            onClick={() => router.push(`/blogs/${existing.slug}`)}
            className="link-action ml-auto"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
