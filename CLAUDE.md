# FootballHub — notes for Claude Code

Premier League blogging and fan community. The full product spec is
`docs/implementation-plan.md`; follow it unless this file says otherwise.

- **Backend:** `backend/` — Java 21, Spring Boot 4.1.2, Spring Security 7, Spring Data JPA
  (Hibernate 7), Flyway 12, Spring Session JDBC, PostgreSQL 16.
- **Frontend:** `frontend/` — Next.js 16.4 (App Router, Cache Components on), React 19.3,
  TypeScript, Tailwind CSS 4.
- **Hosting plan:** frontend on Vercel, backend on Render (Docker), database on Neon.
  Vercel cannot run the Java API, so this split is deliberate.
- **Repo:** https://github.com/Ankur42177/footballhub (branch `main`). Commit each milestone
  separately with a descriptive message and push.

## Status

Done and committed:
- Database schema: Flyway V1 (the plan's schema verbatim), V2 (tokens, notifications,
  audit log, moderation fields, cascading deletes, DB-enforced same-blog replies),
  V3 (Spring Session tables), V4 (2026-27 Premier League clubs). All four were applied
  and constraint-tested on a real PostgreSQL 16.
- All backend modules from the plan's section 7 API: auth, users/profiles, clubs, blogs,
  votes, comments, bookmarks, notifications, reports/moderation/admin, demo seeder.
- Backend integration tests (Testcontainers) for the plan's section 12 test matrix.
- Frontend foundation: design tokens (`src/app/globals.css`), API types (`src/types/api.ts`),
  browser API client with CSRF (`src/lib/api.ts`), server reader (`src/lib/server-api.ts`),
  session context, header + account menu, club rail, footer, avatar.

**Not yet verified:** the backend has never been compiled. It was written in a sandbox
that could not reach Maven Central. All SQL was run against PostgreSQL, but expect a
few compile or test fixes.

## Next steps, in order

1. **Compile and test the backend.** Docker must be running (Testcontainers):
   `cd backend && mvn -B verify`. Fix whatever fails. There is no Maven wrapper yet;
   add one with `mvn -N wrapper:wrapper` and commit `mvnw` with the executable bit.
2. **Finish the frontend** (pages still to build; `src/app/layout.tsx` and
   `src/app/page.tsx` are still the create-next-app defaults):
   - `layout.tsx`: import `@fontsource-variable/big-shoulders-display` and
     `@fontsource-variable/literata`, wrap in `SessionProvider`, render `SiteHeader`,
     `ClubNavigation`, `<main id="main">`, `SiteFooter`, skip link, site metadata.
   - Routes from plan 2.2: `/`, `/login`, `/register`, `/blogs/[slug]`, `/clubs`,
     `/clubs/[slug]`, `/write` (new + `?edit=<slug>`), `/profile/[username]`,
     `/bookmarks`, `/dashboard` (own posts by status + notifications), `/settings`,
     `/admin` (reports queue; users and audit log for admins), `/search`,
     plus `/forgot-password`, `/reset-password`, `/verify-email`, `not-found.tsx`,
     `error.tsx`, `robots.ts`, `sitemap.ts`.
   - Components from plan 10.4: BlogCard, FeaturedArticle (lead story with the
     pitch-line SVG), BlogFeed, BlogEditor (Markdown with preview), VoteButton,
     BookmarkButton, FollowClubButton, CommentList/CommentForm (one-level threads),
     Pagination, LoginForm, RegisterForm, ReportDialog, AdminModerationTable,
     Markdown renderer (react-markdown + remark-gfm, `skipHtml`, no images).
   - Check: `npm run lint`, `npx tsc --noEmit`, `npm run build`.
3. **Deployment files:** `backend/Dockerfile` (multi-stage: Maven + Temurin 21 build,
   JRE 21 runtime, `-XX:MaxRAMPercentage=75`), `render.yaml` blueprint (Docker web
   service, rootDir `backend`, health check `/actuator/health`), and
   `.github/workflows/ci.yml` (backend `mvn -B verify`; frontend lint, typecheck, build;
   optional Playwright journey from plan section 12).
4. **README.md** with local setup and the hosting steps below.
5. **Deploy:** Neon database → Render backend → Vercel frontend (root directory
   `frontend`), then set `FRONTEND_URL` on Render to the Vercel URL.

## Environment variables

Render (backend):

| Name | Value |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://<neon-host>/<db>?sslmode=require` (JDBC form, not `postgresql://`) |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | from Neon |
| `FRONTEND_URL` | the Vercel URL (used in email links) |
| `COOKIE_SECURE` | `true` |
| `ADMIN_EMAILS` | owner's email; that account becomes ADMIN |
| `SEED_DEMO_DATA` | `true` to publish the sample articles once |
| `API_DOCS_ENABLED` | `false` in production |
| `CORS_ALLOWED_ORIGINS` | only if something calls the API directly |

Vercel (frontend): `BACKEND_URL` = Render URL (read at **build** time by the rewrite in
`next.config.ts`, so redeploy after changing it), `NEXT_PUBLIC_SITE_URL` = Vercel URL.

## How the pieces fit

- The browser only calls `/api/v1/*` on the frontend's own origin; `next.config.ts`
  rewrites that to `BACKEND_URL`. Session (`SESSION`) and CSRF (`XSRF-TOKEN`) cookies
  are therefore first-party on the Vercel domain.
- Mutations send the `XSRF-TOKEN` cookie value as the `X-XSRF-TOKEN` header
  (`lib/api.ts` handles this and retries once on `CSRF_INVALID`). Backend uses
  Spring Security 7's `csrf.spa()`.
- Server Components read data with `lib/server-api.ts`, forwarding only the `SESSION`
  cookie so responses are personalised (drafts for authors, `votedByMe`, ...).
- Errors from the API always look like
  `{status, code, message, timestamp, fieldErrors[]}`.
- Authorisation is enforced server-side: URL rules for `/api/v1/admin/**`, and
  `CurrentUser` re-loads the user from the DB for every mutation so suspensions and role
  changes apply immediately. Frontend role checks are UI hints only.

## Version-specific pitfalls (already checked against sources)

Spring Boot 4.1:
- Starters are modular: `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`
  (Flyway won't run with only `flyway-core`), `spring-boot-starter-session-jdbc`.
- `@AutoConfigureMockMvc` lives in `org.springframework.boot.webmvc.test.autoconfigure`
  (needs `spring-boot-starter-webmvc-test`).
- Testcontainers 2: artifacts `testcontainers-postgresql` / `testcontainers-junit-jupiter`,
  class `org.testcontainers.postgresql.PostgreSQLContainer` (not generic).
- Jackson 3 (`tools.jackson.*`); the code avoids using ObjectMapper directly.
- `DaoAuthenticationProvider` takes the `UserDetailsService` in its constructor.
- `Blog.voteCount` is `insertable = false, updatable = false`: only the vote module's
  atomic SQL changes it. Don't make it writable through JPA.
- `ddl-auto: validate`; Flyway owns every schema change. Add a new `V5__...` migration
  rather than editing existing ones once deployed.

Next.js 16.4 (read `frontend/node_modules/next/dist/docs/` before writing Next code):
- `cacheComponents: true`: anything that reads `cookies()`, `params` or `searchParams`
  must render inside `<Suspense>`, otherwise the build fails. Pattern used here: the page
  returns `<Suspense fallback={...}><Content params={props.params} /></Suspense>` and
  `Content` awaits. Keep the root layout free of request data.
- No `new Date()` / `Date.now()` / `Math.random()` in the prerendered shell.
- `error.tsx` receives `{ error, retry }` (not `reset`).
- `params` / `searchParams` are Promises; use the generated `PageProps<'/route'>` types.
- Don't fetch the backend at build time (no `'use cache'` around backend calls, static
  `sitemap.ts`), or builds fail when the API is asleep.

## Design rules (frontend)

Concept: a matchday tactics board. Night-pitch navy (`pitch` #0E1A2B, `pitch-raised`,
`pitch-deep`), chalk text (#ECEFF3 / dim #9FB0C4), lines #24364F, and one accent,
floodlight amber #FFD166. Club colours appear only as thin kit stripes (`.kit-stripe`
with `--kit`). Type: Big Shoulders Display (`.display`, headlines and scores) and
Literata (body). The one bold element is the lead story with chalk pitch lines behind it.
Avoid: all-caps eyebrow labels, `A · B · C` meta strings, identical shadowed cards,
photos (none are licensed). Sentence case, plain verbs, specific error messages.
Respect `prefers-reduced-motion`, keep keyboard focus visible, test at phone width.
