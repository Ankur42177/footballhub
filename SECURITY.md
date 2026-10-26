# Security policy

## Reporting a vulnerability

Please don't open a public issue for security problems. Use GitHub's
[private vulnerability reporting](https://github.com/Ankur42177/footballhub/security/advisories/new)
for this repository instead, with enough detail to reproduce the problem. You'll get a reply
as soon as possible, and fixes are released before details are made public.

## What we do

- Permissions are checked on the server for every request (OWASP A01).
- Passwords use Argon2id; tokens are random, single-use, time-limited and stored as SHA-256
  hashes (A02, A07).
- Database access uses parameterised queries; user content is rendered without raw HTML and
  emails are plain text (A03).
- Limits on login attempts, writes and pending submissions (A04, A07).
- Production disables API docs and hides stack traces; security headers are set on every
  page (A05).
- Dependencies are checked weekly by Dependabot and on every build (A06).
- Important actions are recorded in an audit log, and one-time links are kept out of logs (A09).
- The API only makes outbound requests to a fixed email provider address (A10).

Secrets live only in the hosting platforms' environment settings, never in this repository.
