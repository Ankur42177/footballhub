# FootballHub

Fan-written Premier League analysis and community. Readers browse articles by club, upvote
the best ones and join the discussion. New fans can start with a beginner's guide to the
game. Every story is reviewed by an editor before it goes live.

**Live site:** https://footballhub-three.vercel.app

![CI](https://github.com/Ankur42177/footballhub/actions/workflows/ci.yml/badge.svg)

## Features

- **Articles:** Markdown editor with preview, drafts, club tags, credited photos from
  Wikimedia Commons, reading time and SEO metadata.
- **Editorial review:** administrators publish directly. Stories from everyone else are sent
  to an editor, who approves or rejects them from a one-time email link or the admin page.
- **Community:** upvotes (no account needed, one per person per article), comments with
  replies, bookmarks, club follows and notifications.
- **Discovery:** a featured homepage feed, latest, trending and most-upvoted sorts, pages
  for every Premier League club, search and author profiles.
- **Learn football:** a beginner's guide (positions, the offside rule, fouls and cards, how
  a season works, a glossary) plus a "Football basics" article series.
- **Moderation:** reports, hiding content, roles, suspensions and an audit log.
- Works on phones and desktops.

## Roles

| Role | Can do |
|---|---|
| Visitor | Read, search, upvote |
| Member | Write and submit stories for review, comment, bookmark, follow clubs, report content |
| Moderator | Handle reports, hide content, remove comments |
| Administrator | Everything above, plus publish directly, approve or reject submissions, edit or delete any story, manage users |

## Tech stack

| Part | Technology |
|---|---|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| Backend | Java 21, Spring Boot 4, Spring Security 7, Spring Data JPA |
| Database | PostgreSQL 16 with Flyway migrations |
| Tests | JUnit 5 and Testcontainers (real PostgreSQL), Playwright for browser checks |
| Hosting | Vercel (frontend), Render (API, Docker), Neon (PostgreSQL) |

## Run it locally

You need Java 21 or newer, Docker and Node.js 20.9 or newer. The Maven wrapper is included.

```bash
docker compose up -d postgres                  # local database

cd backend
MAIL_LOG_BODIES=true ./mvnw spring-boot:run    # API on http://localhost:8080

cd frontend                                     # in a second terminal
cp .env.local.example .env.local
npm ci && npm run dev                           # site on http://localhost:3000
```

Locally, emails aren't sent; with `MAIL_LOG_BODIES=true` they're printed to the API log so
you can open verification and review links.

## Tests

```bash
cd backend && ./mvnw verify                     # needs Docker running
cd frontend && npm run lint && npx next typegen && npx tsc --noEmit && npm run build
```

GitHub Actions runs both on every push, and Dependabot checks dependencies weekly.

## Configuration

Settings are environment variables on the hosting platform. **Never commit real values.**

| Backend variable | Purpose |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection (JDBC URL) |
| `FRONTEND_URL` | Public site address (used in links and as the allowed browser origin) |
| `ADMIN_EMAILS` | Accounts with these emails become administrators |
| `REVIEW_EMAIL` | Where review requests for new stories are sent |
| `RESEND_API_KEY`, `MAIL_FROM` | Email delivery through Resend; without a key, mail is only logged |
| `COOKIE_SECURE` | `true` in production |
| `FEATURED_CLUB` | Club whose articles lead the homepage's featured feed |
| `API_DOCS_ENABLED`, `SEED_DEMO_DATA`, `RATE_LIMIT_ENABLED` | Optional switches |

| Frontend variable | Purpose |
|---|---|
| `BACKEND_URL` | API address (read at build time) |
| `NEXT_PUBLIC_SITE_URL` | Public site address |

## Security

- All permissions are enforced by the API, never only in the browser.
- Passwords are hashed with Argon2id. Sessions use HTTP-only, secure cookies with CSRF protection.
- Review links are single-use, expire, and are stored only as hashes.
- Login, sign-up and write requests are rate-limited.
- User content is rendered without raw HTML, and emails are plain text.
- No secrets or personal data are kept in this repository.

See [SECURITY.md](SECURITY.md) for how to report a vulnerability.

## Credits

Built by Ankur Kumar. Photos are from Wikimedia Commons, credited on each image under its
licence. FootballHub isn't affiliated with the Premier League or any club; club names are
used for identification only, and the club badges are original designs.
