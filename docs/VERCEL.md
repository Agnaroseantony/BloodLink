# Deploy BloodLink on Vercel

BloodLink keeps its Java 21 / Spring Boot / Thymeleaf architecture. Vercel's [Container Runtime](https://vercel.com/docs/functions/runtimes/container), currently in beta, builds `Dockerfile.vercel` and routes requests to the Java server. Runtime availability and billing follow the current Vercel plan and beta terms; no hosting purchase has been made by this project.

## Deployment files and changes

- `Dockerfile.vercel`: multi-stage Maven build, Java 21 runtime, non-root process and port 8080.
- `vercel.json`: selects the cloud profile and the matching `PORT=8080`.
- `.vercelignore`: uploads only Java source/build inputs; excludes the local database, tool downloads, logs and environment files.
- `.dockerignore`: excludes local artifacts and secrets from the container build context.
- `application-vercel.properties`: hosted PostgreSQL, a small connection pool, shared JDBC sessions, HTTPS cookies, proxy headers and graceful shutdown.
- `db/migration/postgresql/V1__bloodlink_and_shared_sessions.sql`: Flyway migration for all eight application tables and two session tables. Hibernate validates the migrated cloud schema instead of modifying it on every startup.
- Demo initialization locks the cloud user table before checking for seed data, so simultaneous cold starts do not seed duplicate accounts.

The default local profile still uses your existing file-backed H2 database and servlet sessions. Cloud deployment starts with a separate database; it does not upload or automatically migrate your local `data/` directory.

## Required resources

1. A Vercel account and a new project for this application. Install/connect the Vercel integration in Codex if deploying through the connected workflow.
2. A hosted PostgreSQL database reachable from Vercel. Vercel supports external PostgreSQL providers through its [Marketplace](https://vercel.com/docs/postgres). Pick a provider/plan you approve; a database purchase or paid plan is not implied by the source configuration.
3. Database credentials saved in Vercel environment settings. Do not commit credentials, paste them into public source, or put them in `vercel.json`.

## Environment variables

Add these to the Vercel project **before the application is deployed**:

| Variable | Value |
|---|---|
| `JDBC_DATABASE_URL` | `jdbc:postgresql://HOST:5432/DATABASE?sslmode=require` |
| `DATABASE_USERNAME` | The database login user |
| `DATABASE_PASSWORD` | The database login password; save securely |
| `SEED_DEMO` | `true` for a fictional classroom demo; default is true |

`PORT=8080` and `SPRING_PROFILES_ACTIVE=vercel` are already set by the deployment files. A provider's `postgres://...` or `postgresql://...` URL cannot be pasted unchanged into `JDBC_DATABASE_URL`: use JDBC format and supply username/password separately. Preserve any TLS settings required by your database provider; `sslmode=require` encrypts the connection. For certificate verification, use the provider's documented certificate settings.

Use a **separate database for Preview and Production**, so testing or a preview migration cannot alter the published demonstration database. A newly created database is migrated and seeded once. Do not point this deployment at a database containing unrelated tables/data or try to use Flyway baselining to hide migration errors.

## Publish using the Vercel integration

After installing/connecting Vercel in Codex, select the intended account/team and project and add the database environment variables securely. Deploy this project directory using the container configuration. Check the build/runtime logs, open the returned deployment URL, and complete the acceptance checks below. A source-only upload is not a successful deployment until the application starts and the workflow is verified.

## Alternative: Vercel CLI

With Node.js LTS installed, open a terminal in the project directory:

```text
npx vercel@latest login
npx vercel@latest link
```

Select the intended team and create/link the BloodLink project. Add each variable interactively; the commands prompt for the value:

```text
npx vercel@latest env add JDBC_DATABASE_URL production
npx vercel@latest env add DATABASE_USERNAME production
npx vercel@latest env add DATABASE_PASSWORD production
```

Set the values for Preview as well if you plan to use previews, using that environment's separate database. Then deploy:

```text
npx vercel@latest deploy --prod --logs
```

Vercel detects `Dockerfile.vercel` and builds the application remotely; Docker is needed only if testing the image locally. Do not select a static-site output directory or upload the Thymeleaf templates as plain HTML. They need the Java server to render them.

## Acceptance checks after publishing

1. Open the HTTPS deployment URL. Login page and local CSS/JavaScript load.
2. Sign in as `hospital.care` with `Demo@12345` and publish an O+ request in Bengaluru with one unit and a future IST deadline.
3. In a separate browser session, sign in as `donor.rahul`. Open the matching alert, view hospital location/address and select “I'm available.” No points are awarded yet.
4. Back in the hospital session, refresh and confirm one completed demonstration unit. The request is fulfilled.
5. Refresh donor rewards: one new donation event, 100 points and the appropriate badge appear.
6. Check authentication, CSRF forms and rewards again after the deployment scales down/restarts. PostgreSQL stores the application data and sessions across instances; sessions still expire after 30 minutes of inactivity.
7. Confirm another hospital cannot access the request, donor contact details remain restricted, and map attribution is visible.

These accounts and rewards are for fictional demonstrations only. The known demo password is documented by design. Do not use this public demo for real patient records or clinical decisions.

## Tests and operational behavior

Normal local checks:

```text
mvn test
```

Cloud integration checks against a temporary **real PostgreSQL** instance (no cloud credentials or Docker required):

```text
mvn -Ppostgres-test test
```

On this computer, the portable-toolchain equivalent is `./run.ps1 -PostgresTest`. The default binary selection supports x64 Windows, Linux and macOS; use `-Dpostgres.test.platform=linux-amd64-alpine` for an Alpine x64 test machine.

The opt-in Maven profile downloads a test-only PostgreSQL binary for your platform. Tests start two independent Spring application instances against the same temporary database, exercise real HTTP login/CSRF, complete the workflow, restart an instance and verify session/reward persistence. A second check races confirmations from different instances. Default builds and the deployed image do not contain the embedded test database.

Vercel container instances scale down when idle. Scheduled expiry/session cleanup tasks run while the application is active; request display and acceptance checks independently enforce deadlines, so an idle instance cannot cause an expired request to accept interest. There are no background SMS/email/push services.

If startup fails, check for missing environment variables, inaccessible PostgreSQL, a migration/schema mismatch or the wrong port. A missing database must be fixed; replacing the cloud database with an H2 file in `/tmp` would lose data and split state across instances.

## Current deployment status

The repository includes the container build and cloud configuration. A live URL requires an authenticated Vercel connection and a configured PostgreSQL database. The presence of these files alone does **not** mean the app has been published. See the build verification notes for the checks actually completed.
