# Five-minute demonstration

Start with `./run.ps1`. Use one normal browser session and one incognito/different-browser session at [localhost:8080](http://localhost:8080). Both sessions connect to the same server/database. Use `Demo@12345` for every seeded password.

1. **Introduce the problem (30 seconds).** “Hospitals publish aggregate blood needs. Donors find requests and indicate availability. Only the hospital confirms completed donations. This is a student prototype; clinical screening stays with the hospital.”
2. **Show the hospital (45 seconds).** Sign into session A as `hospital.care`. Open its profile, point to the saved location/address, then return to Overview. Create an **O+**, **1 unit**, **Urgent** request with a deadline tomorrow in IST. Use a note such as “Please call the donation desk before visiting.” Publish. Point out 0/1 confirmed and the Open status.
3. **Show the alert (45 seconds).** In session B, sign in as `donor.rahul`. His group is O+ and city is Bengaluru. If already signed in before publication, keep the tab visible and wait about 20 seconds for the bell/toast; otherwise open Notifications to see the persisted alert. Click **Open request**. The alert becomes read.
4. **Respond (45 seconds).** Show the hospital map, written address, contact number and external directions link. Explain that distance is a straight-line estimate, and that donor coordinates are private. Select **I’m available**. Open Rewards & history: no new donation/points yet. This is only interest.
5. **Confirm (45 seconds).** Return to session A and refresh the request page. The status is In progress and Rahul appears with contact details. After explaining that a real hospital would first perform screening and accept the completed donation, confirm **1 unit**. The request becomes Fulfilled at 1/1 and the confirmation action disappears.
6. **Show the result (30 seconds).** Refresh session B’s Rewards & history. Rahul now has one confirmed donation, 100 points and the First Donation badge (or an increase by those amounts if reused). Return to the request: it is closed to new responses.
7. **Explain reliability (45 seconds).** Show the service’s transaction and request lock. Mention unique keys, role/ownership checks and CSRF. If time allows, sign in as `hospital.lotus` and demonstrate that another hospital cannot open/confirm the request.
8. **Show persistence (30 seconds).** Stop with Ctrl+C, restart from the same project folder and sign back in. Show the fulfilled request and the saved reward. Do not reset the database between stopping and restarting.

For a repeat demo, create another request. Do not expect initial seed data or counters to reset. For a clean first-donation badge demo, register a new fictional donor or use a backed-up fresh database as described in the README.

Optional: withdraw/re-respond before confirmation, disable donor alerts and create another matching request, or filter requests to view Expired/Fulfilled statuses. Never claim matching or rewards establish medical eligibility.
