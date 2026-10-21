# Verification record

Verified locally on 15 October 2026 using Temurin JDK 21.0.12.1, Maven 3.9.11 and Spring Boot 3.5.16.

- **Integration suite:** 10 tests, 0 failures, 0 errors. Includes a real concurrent last-unit confirmation race, role/ownership/CSRF checks, actual Thymeleaf rendering and contact-data isolation.
- **Packaged application:** executable JAR built successfully with `mvn package`.
- **HTTP acceptance flow:** two independent authenticated HTTP sessions on the actual packaged server completed publication, matching alert, donor interest, hospital confirmation, automatic fulfillment and a 100-point reward. Interest had zero points before confirmation.
- **Persistence:** stopped and restarted the isolated file-backed test server; fulfilled request, donation, 100 points and First Donation badge remained available after fresh logins.
- **Browser:** signed in to the local demo, opened dashboard, mobile menu, request feed and request detail. The hospital map loaded four map tiles with visible attribution; no browser error logs were returned during that check.
- **Responsive bounds:** checked 1440px and 390px viewport widths. Document widths were 1425px and 375px respectively (the remaining 15px is the scrollbar), with no horizontal page overflow. The mobile navigation toggle opened and navigation succeeded.
- **Test isolation:** HTTP persistence checks used `.tools/smoke/bloodlink`; integration tests used in-memory H2. The main `data/bloodlink` demo database retains its original fictional seed workflow.

The manual checklist in `TESTING.md` also covers permission-denial, network failure, long-running deadlines, 3/5 badges and other scenarios that were not all individually exercised through the browser during this build. This record does not claim a production or clinical audit.

## Vercel deployment preparation

The final combined run of `./run.ps1 -PostgresTest` passed **12 tests, 0 failures, 0 errors**. The test server requires permission to launch native PostgreSQL processes; the restricted process sandbox prevented one intermediate test launch, and the final permitted run passed.

On 15 October 2026, added a Vercel container build and a separate PostgreSQL cloud profile. The original 10 H2 integration tests passed again. Two additional tests passed against a temporary real PostgreSQL 14.22 database: Flyway migration/validation, HTTPS cookie and CSRF login shared across separate instances, the full HTTP donation/reward workflow, session and reward persistence after restarting an instance, idempotent demo initialization, and simultaneous last-unit confirmations from different instances.

The cloud configuration has been exercised as real Spring application instances on this computer. Docker is not installed here, so the Docker image itself has not been built locally. A Vercel build, a live deployment URL and hosted-database acceptance checks remain dependent on connecting an authenticated Vercel account and configuring a hosted PostgreSQL database. No cloud deployment or paid resource has been created.
