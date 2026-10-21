# Ten viva questions

**1. What problem does your project solve?**  
It coordinates hospital blood requests and donor availability. Hospitals can publish needs, see responses and record completed donations; donors can discover requests and follow their contribution history.

**2. Why Spring Boot and Thymeleaf?**  
Spring Boot configures the embedded server and common Java infrastructure. Thymeleaf renders HTML on the server, so we can build one understandable application without a separate frontend framework.

**3. What is the difference between interest and a donation?**  
Interest is a donor response with no reward. A donation is a separate record created only when the request’s owning hospital confirms a completed donation after screening.

**4. Where do you use inheritance and polymorphism?**  
Entities inherit IDs/versions from `BaseEntity`; registration inherits donor form fields. `PasswordEncoder` is an interface with a BCrypt implementation, and Spring generates repository implementations from interfaces.

**5. How are database relationships represented?**  
JPA annotations express one-to-one user/profile links, hospital-to-request and request-to-response one-to-many relationships via foreign keys, and unique donation/reward links. Responses join donors and requests.

**6. How do you prevent duplicate responses and rewards?**  
The service checks state and the database enforces unique request/donor response keys, unique donation response IDs and unique reward donation IDs. A withdrawn donor reuses the original response row.

**7. Why is donation confirmation transactional?**  
Donation creation, response status, request progress, donor last donation date and reward must succeed together. `@Transactional` commits everything together or rolls it back if a check or database operation fails.

**8. What happens if two hospital actions confirm the last unit at once?**  
Each action first obtains a pessimistic write lock on the request. One proceeds; the other waits, then sees the updated remaining target or fulfilled status and is rejected. An integration test exercises this race.

**9. How does security work?**  
Spring Security authenticates BCrypt-hashed passwords, checks role routes and protects POST forms with CSRF tokens. Service ownership checks restrict hospitals to their own requests and donors to their own alerts/responses. Donor coordinates stay private.

**10. How do matching, maps and persistence work?**  
Publishing creates saved alerts for enabled donors with the same group/city, and visible donor pages poll counts every 20 seconds. Leaflet shows hospital coordinates using attributed OpenStreetMap tiles. File-mode H2 retains records between restarts. Matching does not determine medical eligibility.
