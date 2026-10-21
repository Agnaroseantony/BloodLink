# BloodLink — Blood Donation Management System

A complete, local Java project for a second-year BTech demonstration. Hospitals publish aggregate blood requests, donors receive matching in-app alerts, and the owning hospital confirms donations. Confirmations update the request and award symbolic points in one database transaction.

## Start on this computer

Open PowerShell in `C:\Users\agnar\Documents\ChatGPT\BDMS` and run:

```powershell
.\run.ps1
```

If your PowerShell execution policy blocks this local script, use a process-only override:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
```

Open [http://localhost:8080](http://localhost:8080). Stop with **Ctrl+C**. Portable JDK and Maven copies used during development are in the ignored `.tools` directory. The launcher detects them automatically; it also supports normal system installations.

```powershell
.\run.ps1 -Test             # integration tests; separate in-memory database
.\run.ps1 -PostgresTest     # local tests + real PostgreSQL cloud checks
.\run.ps1 -Package          # tests + executable JAR
.\run.ps1 -Port 8081        # choose another local port
```

## Setup on another computer

For online hosting, see [Deploy on Vercel](docs/VERCEL.md). The cloud profile retains Java/Thymeleaf and uses PostgreSQL with shared sessions; the default local setup below continues to use H2.

Vercel build inputs are `Dockerfile.vercel`, `vercel.json` and `.vercelignore`. Database migrations and secure shared sessions are enabled only by the `vercel` profile. Keep database credentials in Vercel environment settings; `.env.example` contains placeholders only.

1. Install **JDK 21** from [Eclipse Adoptium](https://adoptium.net/temurin/releases/?version=21). Set `JAVA_HOME` to the JDK directory and add its `bin` directory to `PATH`.
2. Install [Apache Maven](https://maven.apache.org/download.cgi), **3.9.11 recommended** (minimum 3.6.3), and add its `bin` directory to `PATH`.
3. Copy this project, open a terminal in the directory containing `pom.xml`, and verify `java -version` and `mvn -version` both use JDK 21.
4. Run the following. An internet connection is needed for the first Maven dependency download.

```text
mvn test
mvn spring-boot:run
```

For an executable JAR:

```text
mvn package
java -jar target/bloodlink-1.0.0.jar
```

Always start from the project directory to reuse the same relative database path. The app binds to localhost (`127.0.0.1`) by default. It is configured for a local classroom demonstration.

## Demo accounts

Every seeded account uses **`Demo@12345`**. Names, addresses, phone numbers and emails are fictional. Do not contact them.

| Role | Username | Name | Blood group | City |
|---|---|---|---|---|
| Hospital | `hospital.care` | Carebridge Hospital | — | Bengaluru |
| Hospital | `hospital.lotus` | Lotus Community Hospital | — | Bengaluru |
| Donor | `donor.asha` | Asha Rao | O+ | Bengaluru |
| Donor | `donor.rahul` | Rahul Shah | O+ | Bengaluru |
| Donor | `donor.meera` | Meera Nair | A+ | Bengaluru |
| Donor | `donor.vikram` | Vikram Das | B+ | Bengaluru |
| Donor | `donor.sana` | Sana Ali | AB− | Bengaluru |
| Donor | `donor.arjun` | Arjun Roy | O− | Mumbai |

Use a regular browser window for a hospital and an incognito window or a different browser for a donor. Tabs in the same browser profile share the login session. `donor.rahul` starts with no completed donations; `donor.asha` starts with one confirmed demonstration event and 100 points.

## Scope and completed features

- Spring Security login, BCrypt passwords, server-side role/ownership checks, CSRF-protected forms, validation and readable errors.
- Hospital profiles with location selection; request publication, response/contact management, cancellation and donation confirmation.
- Donor registration/profile/preferences; filtered requests, approximate distance, respond/withdraw/re-respond, private donor coordinates.
- File-backed H2, eight JPA entities, unique response/alert/donation/reward constraints, optimistic versions and request row locking.
- Persistent matching notifications, unread bell, 20-second polling while visible and signed in, mark one/all read.
- Vendored Leaflet 1.9.4, OpenStreetMap tiles and attribution, address fallback, manual coordinates, deliberate location permission and external directions.
- Transactional confirmation, target enforcement, automatic fulfillment, 100 points per event, badges at 1/3/5 events and donation history.
- Database-derived dashboards and a responsive blush/lavender interface with accessible focus, empty/error/success/loading states.

Public hospital registration, patient records, inventory, billing, payments, chat, SMS, email and background push are outside scope. Matching is a discovery filter, not medical compatibility or eligibility advice. Hospital screening remains necessary. Rewards have no cash value. This is an educational prototype, not a clinical deployment.

## Technology and design choices

Java 21, Spring Boot **3.5.16**, Spring MVC, Thymeleaf, Spring Data JPA/Hibernate, Spring Security, Bean Validation, H2, Maven, vanilla HTML/CSS/JavaScript and Leaflet **1.9.4**. Spring Boot's parent manages compatible transitive versions. The selected stable 3.5 line keeps the familiar Spring Security 6 stack suitable for teaching; see [Spring Boot 3.5 requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

There is one server-rendered application, no separate frontend build and no paid service. UI components live in `fragments.html`; business rules live in `BloodLinkService`, so controllers remain easy to follow. Small demonstration datasets are filtered in Java for clarity rather than introducing query builders or pagination.

## Database and configuration

No separate database installation or SQL import is required. Hibernate creates tables; `DemoData` seeds an **empty user database only**, inside a transaction. Requests are seeded with future deadlines relative to the first run. Existing data is retained and seed deadlines are not refreshed at each restart.

- Database file: `data/bloodlink.mv.db` (ignored by Git).
- Connection: `jdbc:h2:file:./data/bloodlink;DB_CLOSE_ON_EXIT=FALSE`.
- Local database credentials: `sa`, empty password; H2 web console is disabled.
- Seed class: `src/main/java/com/bloodlink/config/DemoData.java`.
- No real patient or donor data should be placed in the classroom database.
- To start an unseeded database: set environment variable `SEED_DEMO=false`. There is intentionally no public hospital registration; for an unseeded demonstration, provision hospital users and profiles in `DemoData` or another trusted setup routine.
- To make a fresh demo, **stop the application**, back up `data/`, then rename the `data` directory. The next launch creates a new database and new fictional seed data. Keep the backup if you need your previous work.
- `ddl-auto=update` is convenient for a student project. A real deployment would use reviewed migrations, account verification, backups and a broader privacy/security review.

| Setting | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | Local HTTP port |
| `SEED_DEMO` | `true` | Seed an empty database |
| `MAP_TILE_URL` | `https://tile.openstreetmap.org/{z}/{x}/{y}.png` | Switch tile provider |
| `MAP_ATTRIBUTION` | `OpenStreetMap contributors` | Provider attribution text |
| `MAP_ATTRIBUTION_URL` | `https://www.openstreetmap.org/copyright` | Provider licence link |

Example: `java -jar target/bloodlink-1.0.0.jar --server.port=8081`. The launcher, deadlines, confirmations and displays use **Asia/Kolkata (IST)**. Tests run with an isolated in-memory database and do not edit your local demo database.

Maps need internet for tiles, but the app, forms, alerts and rewards run locally. Leaflet itself is bundled. Tile usage follows the [OpenStreetMap policy](https://operations.osmfoundation.org/policies/tiles/): visible attribution, HTTPS, normal browser caching/referrer, no tile proxy, no bulk downloads, and no offline tile packs. If you change provider, update both its URL and attribution and review that provider's policy. Leaflet's licence is included under `static/vendor/leaflet/LICENSE`.

## Project structure

```text
BDMS/
├── pom.xml
├── run.ps1
├── README.md
├── .gitignore
├── docs/
│   ├── PROJECT_GUIDE.md          # OOP, ER diagram, lifecycle, route map
│   ├── TESTING.md                # automated coverage + manual checklist
│   ├── DEMO.md                   # two-session presentation script
│   ├── VIVA.md                   # ten questions and concise answers
│   └── VERIFICATION.md           # checks performed on this build
├── src/main/java/com/bloodlink/
│   ├── BloodLinkApplication.java
│   ├── config/{SecurityConfig,DemoData}.java
│   ├── controller/{PageController,PageAdvice}.java
│   ├── controller/form/{DonorForm,RegistrationForm,HospitalForm,RequestForm}.java
│   ├── model/
│   │   ├── BaseEntity.java
│   │   ├── AppUser.java
│   │   ├── DonorProfile.java
│   │   ├── HospitalProfile.java
│   │   ├── BloodRequest.java
│   │   ├── DonorResponse.java
│   │   ├── Notification.java
│   │   ├── Donation.java
│   │   ├── RewardTransaction.java
│   │   └── {Role,BloodGroup,RequestStatus,ResponseStatus,Urgency}.java
│   ├── repository/               # one Spring Data interface per entity
│   └── service/{BloodLinkService,BusinessException}.java
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   │   ├── fragments.html
│   │   ├── login.html
│   │   ├── register.html
│   │   ├── dashboard.html
│   │   ├── requests.html
│   │   ├── request-detail.html
│   │   ├── request-form.html
│   │   ├── donor-profile.html
│   │   ├── hospital-profile.html
│   │   ├── notifications.html
│   │   ├── responses.html
│   │   ├── rewards.html
│   │   └── error.html
│   └── static/
│       ├── css/app.css
│       ├── js/app.js
│       └── vendor/leaflet/{leaflet.js,leaflet.css,LICENSE}
├── src/test/java/com/bloodlink/BloodLinkIntegrationTest.java
├── data/                        # generated persistent database, ignored
├── target/                      # build outputs, ignored
└── .tools/                      # optional portable toolchain/cache, ignored
```

Start learning with [Project guide](docs/PROJECT_GUIDE.md), then run the [Testing checklist](docs/TESTING.md), rehearse [Demo](docs/DEMO.md), and review [Viva questions](docs/VIVA.md).

See [Verification record](docs/VERIFICATION.md) for the automated, HTTP persistence and browser checks actually performed.

## Troubleshooting

| Symptom | Action |
|---|---|
| Java/Maven not found | Reopen your terminal after installing; verify `JAVA_HOME`, `PATH`, `java -version`, `mvn -version`. |
| Port 8080 in use | Run `./run.ps1 -Port 8081` or pass `--server.port=8081` to the JAR. |
| Database already in use | Stop the other BloodLink process. Two browser sessions should connect to one running server. |
| Maps are grey | Check internet access; use the written address/directions or enter coordinates manually. |
| No matching alert | Check donor group, exact city (case insensitive), notification preference and whether the request was published after enabling alerts. Existing requests are not backfilled. |
| Old requests expired | Create a new request with a future IST deadline. Old seed deadlines remain unchanged by design. |
| Forbidden after a long pause | Sign in again and reload the form to get a fresh session/CSRF token. |
| Maven dependency download fails | Check network/proxy settings; dependencies must download successfully once before offline execution. |
