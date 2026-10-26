# FootballHub

Fan-written Premier League analysis and community. Readers browse articles by club; registered
fans publish their own pieces in Markdown, upvote, comment and reply, bookmark articles and
follow clubs. Moderators work through a reports queue, and administrators manage users and
read an audit log.

**Live site:** https://footballhub-three.vercel.app  
**API health:** https://footballhub-api-coty.onrender.com/actuator/health

> The API runs on Render's free plan and sleeps when idle, so the first page load after a
> quiet spell can take up to a minute.

![CI](https://github.com/Ankur42177/footballhub/actions/workflows/ci.yml/badge.svg)

---

## Features

- **Accounts:** register, log in and out, email verification, password reset (single-use,
  expiring tokens), profile and password settings. Passwords are hashed with Argon2id.
- **Articles:** Markdown editor with live preview, drafts, publish, archive, delete, edit.
  Collision-safe slugs, reading time, SEO metadata, canonical URLs and JSON-LD.
- **Community:** anyone can upvote, signed in or not: one vote per person per article, enforced
  by the database (visitors are recognised by a random, HTTP-only `FH_VISITOR` cookie, and
  votes are rate-limited per IP), comments with one level of replies, bookmarks, club follows,
  and notifications for replies and moderation.
- **Discovery:** home feed with a lead story, latest, trending and most-upvoted sorts,
  pages for every 2026-27 Premier League club, keyword and club search, author profiles,
  and pagination everywhere.
- **Moderation:** report an article or comment; moderators resolve or dismiss reports
  and hide content; admins change roles, suspend accounts and read the audit log.
  All permissions are checked by the API, never only in the browser.

### Club badges and photos

- Every club has an original shield badge drawn in its kit colours and pattern
  (`frontend/src/components/ui/ClubBadge.tsx`, colours in `frontend/src/lib/clubs.ts`).
  They are deliberately **not** the official crests, which are trademarks that need a licence.
- Article headers and club pages are tinted with the club's colour.
- Articles can include photos from **Wikimedia Commons only** (freely licensed). Each one shows
  a caption and the photographer and licence, linked to its Commons page, as the licences
  require. Markdown format:
  `![Alt](https://upload.wikimedia.org/... "Caption||Photo: Name, CC BY-SA 4.0, via Wikimedia Commons||https://commons.wikimedia.org/wiki/File:...")`
- The site works on phones: every page fits a 375 px screen (checked with Playwright),
  the club bar scrolls sideways, and the header has a search icon.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16.4 (App Router, Cache Components, Partial Prefetching), React 19.3, TypeScript, Tailwind CSS 4 |
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Security 7, Data JPA / Hibernate 7, Validation, Actuator) |
| Sessions | Spring Session JDBC (HTTP-only `SESSION` cookie) with CSRF protection (`XSRF-TOKEN`) |
| Database | PostgreSQL 16, schema managed by Flyway |
| API docs | springdoc-openapi (Swagger UI, local only) |
| Tests | JUnit 5, Spring Boot Test, Testcontainers (real PostgreSQL) |
| Hosting | Vercel (frontend), Render (backend, Docker), Neon (PostgreSQL) |

## How it fits together

```mermaid
flowchart LR
    Browser -->|"pages + /api/v1/*"| Vercel["Next.js on Vercel"]
    Vercel -->|"rewrite /api/v1/* and server-side reads"| Render["Spring Boot API on Render"]
    Render -->|JDBC + TLS| Neon[("PostgreSQL on Neon")]
```

- The browser only talks to the Vercel domain. `next.config.ts` rewrites `/api/v1/*` to the
  API, so the session and CSRF cookies are first-party and aren't affected by third-party
  cookie blocking.
- Server Components read data directly from the API (`src/lib/server-api.ts`), forwarding
  only the visitor's `SESSION` cookie so pages are personalised (drafts, "voted by me").
- Mutations send the CSRF token as the `X-XSRF-TOKEN` header (`src/lib/api.ts`).
- Every error from the API has the same shape:
  `{ status, code, message, timestamp, fieldErrors[] }`.

## Repository layout

```text
footballhub/
├── backend/                 Spring Boot API
│   ├── Dockerfile           multi-stage build (JDK 21 build, JRE 21 runtime)
│   ├── mvnw                 Maven wrapper (no global Maven needed)
│   └── src/main/java/com/footballhub/
│       ├── auth/  user/  blog/  club/  comment/  vote/
│       ├── bookmark/  notification/  moderation/  demo/
│       └── security/  config/  common/
│   └── src/main/resources/db/migration/   Flyway V1 to V4
├── frontend/                Next.js app
│   └── src/
│       ├── app/             routes (see below)
│       ├── components/      blog, comments, club, forms, admin, layout, ui
│       ├── lib/             API clients, formatting, navigation helpers
│       └── types/           API contracts
├── docs/implementation-plan.md
├── docker-compose.yml       local PostgreSQL 16
├── render.yaml              Render Blueprint for the API
└── .github/workflows/ci.yml backend tests + frontend lint, types and build
```

### Pages

| Route | What it does |
|---|---|
| `/` | Lead story, sorted feed (latest, trending, most upvoted), trending list |
| `/blogs/[slug]` | Article, upvote, bookmark, report, comments and replies, author controls |
| `/clubs`, `/clubs/[slug]` | All clubs with follow buttons; a club's feed |
| `/write`, `/write?edit=<slug>` | Markdown editor with preview, drafts and publishing |
| `/profile/[username]` | Public author profile and their articles |
| `/bookmarks` | Saved articles |
| `/dashboard` | Your posts by status, notifications |
| `/settings` | Profile, followed clubs, password, email verification |
| `/admin` | Reports queue; users and audit log for admins |
| `/search` | Keyword, club and order filters |
| `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email` | Account flows |

## Run it locally

**You need:** Java 21 or newer, Docker (running), Node.js 20.9 or newer with npm.
Maven is not required; the project includes the Maven wrapper.

```bash
# 1. Database
docker compose up -d postgres

# 2. API on http://localhost:8080 (Flyway creates the schema on first start)
cd backend
SEED_DEMO_DATA=true ADMIN_EMAILS=you@example.com ./mvnw spring-boot:run

# 3. Frontend on http://localhost:3000, in a second terminal
cd frontend
cp .env.local.example .env.local
npm ci
npm run dev
```

On Windows PowerShell use `.\mvnw.cmd spring-boot:run` and set variables with `$env:NAME="value"`.

- Register at http://localhost:3000/register. If your email is in `ADMIN_EMAILS`, the account
  becomes an administrator (existing accounts are promoted when the API restarts).
- Emails (verification, password reset) are written to the API log in development; copy the
  link from there.
- Health check: http://localhost:8080/actuator/health. Swagger UI:
  http://localhost:8080/swagger-ui/index.html.

## Tests and checks

```bash
cd backend && ./mvnw verify        # 41 tests; Docker must be running (Testcontainers)

cd frontend
npm run lint
npx next typegen && npx tsc --noEmit
npm run build
```

The backend suite covers the plan's test matrix: registration and login, ownership checks
on articles and comments, duplicate and concurrent votes, cross-article replies, duplicate
bookmarks, draft privacy, moderation with auditing, and suspended accounts. GitHub Actions
runs everything on each push to `main` and on pull requests.

## Configuration

### Backend (Render)

| Variable | Purpose | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://<host>/<db>?sslmode=require` | local Docker DB |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Database credentials | local dev values |
| `FRONTEND_URL` | Public site URL, used in email links | `http://localhost:3000` |
| `ADMIN_EMAILS` | Comma-separated emails that become administrators | empty |
| `COOKIE_SECURE` | `true` in production (HTTPS-only cookies) | `false` |
| `SEED_DEMO_DATA` | Publish sample articles once on an empty database | `false` |
| `API_DOCS_ENABLED` | Swagger UI and OpenAPI JSON | `true` |
| `REQUIRE_VERIFIED_EMAIL` | Block publishing until the email is verified | `false` |
| `CORS_ALLOWED_ORIGINS` | Only needed if something calls the API cross-origin | `http://localhost:3000` |
| `RATE_LIMIT_ENABLED` | Per-IP limits on auth and write endpoints | `true` |

### Frontend (Vercel)

| Variable | Purpose |
|---|---|
| `BACKEND_URL` | The API's URL. Read at **build** time by the rewrite, so redeploy after changing it |
| `NEXT_PUBLIC_SITE_URL` | Public site URL for canonical links, the sitemap and metadata |

## Deployment

The frontend runs on Vercel, but Vercel can't run a long-lived Java server, so the API runs
on Render in Docker and the database is on Neon.

1. **Neon:** create a project (region close to Render, e.g. AWS Singapore) and a database.
   From the connection details, use the **direct** host (not `-pooler`) and convert it to
   JDBC form: `jdbc:postgresql://<host>/<db>?sslmode=require`, with the user and password as
   separate variables. Flyway creates the tables on the API's first start.
2. **Render:** New > Blueprint, pick this repository; `render.yaml` defines a Docker web
   service from `backend/` with a health check on `/actuator/health`. Fill in the database
   variables, `ADMIN_EMAILS` and (after step 3) `FRONTEND_URL`.
3. **Vercel:** import the repository with **Root Directory** `frontend`. Set `BACKEND_URL`
   to the Render URL and `NEXT_PUBLIC_SITE_URL` to the Vercel URL, then deploy.
4. Set `FRONTEND_URL` on Render to the Vercel URL and let it redeploy.

**This deployment:**

| Piece | Service | Where |
|---|---|---|
| Frontend | Vercel project `footballhub` (root `frontend`, Node 22) | https://footballhub-three.vercel.app |
| API | Render web service `footballhub-api` (Docker, free, Ohio) | https://footballhub-api-coty.onrender.com |
| Database | Neon project `football_blog`, database `footballhub` (AWS us-east-2) | direct host, `sslmode=require` |

Pushing to `main` redeploys both the frontend (Vercel) and the API (Render).

Free-tier notes: the Render free plan sleeps after about 15 minutes without traffic and takes
up to a minute to wake, so the first request after a quiet spell is slow (the site shows a
"waking up" message rather than failing). Neon's free tier also suspends idle compute.

## Performance on free hosting

The API runs on Render's free plan: 0.1 of a CPU core and 512 MB of memory. Spring Boot is
CPU-hungry while it starts, so a plain image took **over 5 minutes** to start on that much
CPU (measured locally with `docker run --cpus=0.1 --memory=512m`). These changes fix the
experience:

| Change | Where | Effect |
|---|---|---|
| JVM flags for small machines: serial GC, C1-only JIT, one compiler thread, smaller stacks | `backend/Dockerfile` | Startup 5+ min to about 3 min |
| Java 25 AOT cache: a training start at build time saves loaded and linked classes and method profiles; real starts reuse them (the code still targets Java 21) | `backend/Dockerfile` | About 3 min to **about 56 s** (90 s with Java 21 class data sharing), using under 300 MB of memory |
| Public pages cached with `'use cache: remote'` (fresh 60 s, refreshed in the background, kept 7 days) | `frontend/src/lib/server-api.ts` | Visitors who aren't signed in get pages in about half a second, even while the API sleeps |
| Keep-awake ping every 10 minutes to `/actuator/health/liveness` (doesn't touch the database) | `.github/workflows/keep-warm.yml` | The API rarely sleeps, so visitors don't wait for a cold start |
| Server-side API calls time out after 12 s; the error page says the server is starting and retries every 10 s | `frontend/src/lib/server-api.ts`, `frontend/src/app/error.tsx` | No more 60-second blank page; the page recovers by itself |

All three services sit in the same area (Vercel functions in Washington, Render in Ohio,
Neon in AWS us-east-2), so once the API is awake a page needs only a few milliseconds per
API call. Neon's free compute suspends after a few idle minutes and wakes in about a second.

If you upgrade Render to a paid instance (no sleep, a full CPU), startup drops to a few
seconds and the keep-awake workflow can be removed.

## Roles and admin

| Role | Can do |
|---|---|
| Member | Write, edit and delete their own articles; vote, comment, reply, bookmark, follow clubs, report content |
| Moderator | Everything a member can, plus the reports queue, hiding and unhiding articles and comments, deleting any comment |
| Admin | Everything a moderator can, plus editing, archiving and deleting **any** article, changing roles, suspending accounts and reading the audit log |

Accounts registered with an email listed in `ADMIN_EMAILS` become admins automatically, and
existing accounts with those emails are promoted when the API restarts. The live site's admin
is the owner's account (`@ankur`).

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Sign-up, login or any button that saves something says "permission" / 403 in the browser, but works from scripts | The browser sends `Origin: <site URL>`; if that origin isn't allowed, Spring's CORS filter answers "Invalid CORS request" | The site's `FRONTEND_URL` is always allowed (`AppProperties.allowedOrigins()`); make sure `FRONTEND_URL` on Render is exactly the live site URL |
| Every page hangs, Render logs show `password authentication failed for user 'neondb_owner'` | `DATABASE_PASSWORD` on Render doesn't match Neon (often a bad paste, with extra characters or spaces) | Delete the variable on Render, add it again with the password from Neon (**Connect**, then **Show password**), then **Save, rebuild, and deploy** |
| `DATABASE_URL` errors such as `No suitable driver` | Neon's `postgresql://user:pass@...` string was used | Use the JDBC form `jdbc:postgresql://<host>/<db>?sslmode=require`, with the user and password in their own variables |
| Pushes to `main` don't redeploy the API | Render's GitHub app isn't installed on the repository | GitHub, then Settings, Applications, Render, Configure: give it access to `footballhub` (or use **Manual Deploy** on Render) |
| First visit after a quiet spell shows "Warming up the floodlights" | The free Render instance was asleep | Wait about a minute; the page reloads by itself. The keep-awake workflow makes this rare |
| `/actuator/health/liveness` returns 404 | Health probes are off | `management.endpoint.health.probes.enabled: true` in `application.yml` (already set) |
| A changed `BACKEND_URL` has no effect | The rewrite reads it at build time | Redeploy the Vercel project |

GitHub runs scheduled workflows on a best-effort basis: on 25-26 Oct 2026 the 10-minute ping
ran only about every 2-3 hours, so the API still sleeps between runs. For a dependable
keep-awake, add a free uptime monitor (for example UptimeRobot, every 5 minutes) on
`/actuator/health/liveness`, or move the API to Render's Starter plan, which never sleeps.

Render's free plan includes 750 instance hours a month, and one service kept awake all month uses
about 744 of them. Other free services in the same workspace share that budget.

## Security notes

- Server-side authorisation on every endpoint; the UI's role checks are hints only.
  Suspensions and role changes apply immediately (the user is reloaded on each mutation).
- Session cookie is HTTP-only, `SameSite=Lax`, `Secure` in production; CSRF tokens on all
  state-changing requests.
- Markdown is rendered without raw HTML or images, and links get `rel="nofollow ugc"`.
- Rate limits on login, registration and writes. No secrets in the repository.

## Known limitations

- Emails are logged rather than sent; plug a real provider into `MailService` before relying
  on verification or password resets in production.
- No image uploads (and no club logos or photos, which would need licences).
- Rate limiting is in memory, so it resets on restart and is per instance.
- The browser end-to-end journey from the plan (Playwright) isn't written yet.

FootballHub isn't affiliated with the Premier League or any club. Club names are used for
identification only.
