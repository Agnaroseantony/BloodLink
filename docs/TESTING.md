# Testing checklist

## Automated tests

Run `./run.ps1 -Test` on this computer, or `mvn test` with JDK 21 and Maven installed. Tests use `jdbc:h2:mem:bloodlink-test`, independent of the persistent demonstration file. The integration suite starts Spring, repositories, security and Thymeleaf; page checks render the real templates.

Coverage includes the full publish/alert/respond/confirm/reward flow; points only after confirmation; duplicate response/confirmation prevention; wrong-hospital and wrong-request rejection; unit target checks and rollback; preference/group/city alert matching; alert ownership; withdrawal/re-response; cancellation and expiry; simultaneous last-unit confirmations; both role page rendering; donor contact isolation; CSRF/authentication; invalid input; password hashing; and optional approximate distances.

## Manual test matrix

For the separate Vercel/PostgreSQL profile, run `./run.ps1 -PostgresTest` (or `mvn -Ppostgres-test test`). This adds two real PostgreSQL integration checks covering shared login/CSRF across instances, a full HTTP workflow, restart persistence, migrations, and a confirmation race across instances. See [Vercel deployment](VERCEL.md).

Use a hospital session and a separate donor browser session. All times entered are IST. Tick each item when verified.

### Setup, authentication and persistence

- [ ] Start the app and sign in with each of the two hospital accounts and a donor account.
- [ ] Invalid credentials show an error. Signing out ends the session.
- [ ] Register a new fictional donor; duplicate username, blank fields, future DOB, malformed phone/email and short password show validation errors.
- [ ] Attempt a hospital URL as a donor and a donor URL as a hospital: access denied.
- [ ] Restart the server from the same folder. New profile, request, response, donation, read status and reward remain saved.
- [ ] Confirm tests do not change your demonstration data.

### Hospital workflow

- [ ] Edit hospital name/address/city/phone and pick its location by clicking the map; save and reload.
- [ ] Invalid or missing coordinates and whitespace-only required fields are rejected.
- [ ] Publish an urgent O+ request for Bengaluru with a future deadline and target of one unit.
- [ ] Zero/negative/over-100 units, missing group and past deadline are rejected.
- [ ] The hospital’s saved location appears on the new request.
- [ ] A matching donor’s response appears after refresh, with name/group/contact and Interested status.
- [ ] Confirm only after explaining/simulating completed donation. Progress becomes 1/1 and status Fulfilled.
- [ ] Refresh/resubmit confirmation: no second donation or additional points are recorded.
- [ ] For a two-unit request, confirm one unit: status remains In progress at 1/2.
- [ ] Confirmation beyond the remaining target or with nonpositive units is rejected without changing points/progress.
- [ ] A second hospital cannot open, cancel or confirm the first hospital’s request even with a manually typed URL.
- [ ] Cancel an open request: future response/confirmation actions are unavailable and rejected server-side.

### Donor workflow and privacy

- [ ] Update donor details/preferences and save; leaving both optional coordinates empty succeeds, supplying only one fails.
- [ ] Filter by blood group, city, urgency and status; Reset restores the active feed.
- [ ] Optional coordinates show an explicitly approximate distance; without coordinates, distance is omitted.
- [ ] Request details contain hospital address/contact, map, deadline, progress and directions.
- [ ] Select “I’m available”: one response is saved, with no new points or donation event.
- [ ] Repeat the same action: duplicate interest is rejected.
- [ ] Withdraw before confirmation: no reward; respond again: the original response is reused.
- [ ] After hospital confirmation, withdrawal and reconfirmation are unavailable.
- [ ] Donors cannot see other donors’ contact details or precise coordinates.
- [ ] Owning hospitals see contact details only for donors responding to their requests; withdrawn contact details are hidden.
- [ ] Donor rewards show 100 points per confirmed event (even if an event has multiple units), history and First Donation badge.
- [ ] On a fresh fictional account, confirm three and then five demonstration events: milestone badges unlock at the exact event count. These accelerated events are classroom simulation, not donation-interval advice.

### Alerts and maps

- [ ] Keep the matching donor’s page visible while a hospital publishes a request. Within about 20 seconds the bell count updates and a toast links to alerts.
- [ ] Different blood group, different city and disabled preference produce no new alert. City case differences still match.
- [ ] Reopening pages/polling does not create duplicate alerts.
- [ ] Open an alert: it is marked read and opens its request; mark-all affects only that donor.
- [ ] New alerts still arrive when a tab becomes visible again. Alert failure text appears if connection/session fails.
- [ ] No browser location prompt appears on page load. It appears only after selecting “Use my location.”
- [ ] Decline permission: manual city/coordinate entry and map clicking remain usable.
- [ ] Disconnect internet: written hospital address/contact remain visible and online-map fallback text is clear.
- [ ] Open directions: hospital destination coordinates are correct and the external page opens in a new tab.
- [ ] OpenStreetMap attribution remains visible and no tile download/offline-pack feature exists.

### Deadlines, UI and edge cases

- [ ] Set a request deadline a minute ahead. After it passes, refresh: Expired appears and actions are rejected immediately; stored expiry follows within roughly a minute.
- [ ] Empty filters, response history, rewards and notifications show meaningful empty states.
- [ ] Unknown IDs, forbidden operations and stale/conflicting actions show clear error pages without personal data or stack traces.
- [ ] Keyboard focus is visible and form labels are associated; submit buttons show Working while submitting.
- [ ] At desktop, tablet and 390px mobile width, navigation, cards, forms and maps fit; wide tables scroll within their panel.
- [ ] Mobile menu opens/closes and Escape dismisses it. Reduced-motion preference suppresses hover animation.

## Practical limits

Polling is visible-app polling, not background push. A previously created request does not generate a new alert when a donor later changes preferences. Location is a straight-line estimate, not a travel time or eligibility decision. The H2 file supports one application process; run two browser sessions against that single process.
