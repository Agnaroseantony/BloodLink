package com.bloodlink;

import com.bloodlink.controller.form.*;
import com.bloodlink.model.*;
import com.bloodlink.repository.*;
import com.bloodlink.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:bloodlink-test;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed-demo=true"})
@AutoConfigureMockMvc
class BloodLinkIntegrationTest {
    @Autowired BloodLinkService service;
    @Autowired AppUserRepository users;
    @Autowired HospitalProfileRepository hospitals;
    @Autowired DonorProfileRepository donors;
    @Autowired BloodRequestRepository requests;
    @Autowired DonorResponseRepository responses;
    @Autowired NotificationRepository notifications;
    @Autowired DonationRepository donations;
    @Autowired RewardTransactionRepository rewards;
    @Autowired PasswordEncoder passwords;
    @Autowired MockMvc mvc;
    String owner, other, donorName, city;

    @BeforeEach void fixtures() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        owner = "hospital." + suffix; other = "other." + suffix; donorName = "donor." + suffix; city = "City " + suffix;
        hospital(owner); hospital(other); donor(donorName, BloodGroup.O_POS, city, true);
    }
    void hospital(String name) {
        AppUser user = new AppUser(); user.setUsername(name); user.setPasswordHash(passwords.encode("Demo@12345")); user.setRole(Role.HOSPITAL); users.save(user);
        HospitalProfile hospital = new HospitalProfile(); hospital.setUser(user); hospital.setName("Test Hospital"); hospital.setCity(city); hospital.setAddress("1 Test Road"); hospital.setPhone("9000010001"); hospital.setLatitude(12.97); hospital.setLongitude(77.59); hospitals.save(hospital);
    }
    DonorProfile donor(String name, BloodGroup group, String cityName, boolean alerts) {
        RegistrationForm form = new RegistrationForm(); form.setUsername(name); form.setPassword("Demo@12345"); form.setFullName("Test Donor"); form.setDateOfBirth(LocalDate.of(2000,1,1));
        form.setBloodGroup(group); form.setCity(cityName); form.setPhone("9000012345"); form.setNotificationsEnabled(alerts);
        return service.register(form);
    }
    BloodRequest publish(int units) { return publishFor(owner, units); }
    BloodRequest publishFor(String hospital, int units) {
        RequestForm form = new RequestForm(); form.setBloodGroup(BloodGroup.O_POS); form.setUrgency(Urgency.URGENT); form.setUnitsRequested(units); form.setRequiredBy(BloodLinkService.now().plusDays(1));
        return service.publish(hospital, form);
    }

    @Test void completeWorkflowAwardsOnlyAfterConfirmationAndStopsAtTarget() {
        BloodRequest request = publish(2);
        assertThat(service.unread(donorName)).isEqualTo(1);
        DonorResponse response = service.respond(donorName, request.getId());
        assertThat(service.points(donorName)).isZero();
        assertThat(service.donorDonations(donorName)).isEmpty();
        assertThat(service.request(request.getId()).getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
        assertThatThrownBy(() -> service.confirm(other, request.getId(), response.getId(), 1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.confirm(owner, request.getId(), response.getId(), 3)).isInstanceOf(BusinessException.class);
        assertThat(service.points(donorName)).isZero();
        service.confirm(owner, request.getId(), response.getId(), 2);
        assertThat(service.request(request.getId()).getStatus()).isEqualTo(RequestStatus.FULFILLED);
        assertThat(service.request(request.getId()).getConfirmedUnits()).isEqualTo(2);
        assertThat(service.points(donorName)).isEqualTo(100);
        assertThat(service.donorDonations(donorName)).hasSize(1);
        assertThat(service.donor(donorName).getLastDonationDate()).isEqualTo(BloodLinkService.now().toLocalDate());
        assertThatThrownBy(() -> service.confirm(owner, request.getId(), response.getId(), 2)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.respond(donorName, request.getId())).isInstanceOf(BusinessException.class);
        assertThat(service.points(donorName)).isEqualTo(100);
    }
    @Test void duplicateConfirmationIsRejectedWhileRequestStillOpen() {
        BloodRequest request = publish(3); DonorResponse response = service.respond(donorName, request.getId());
        service.confirm(owner, request.getId(), response.getId(), 1);
        assertThatThrownBy(() -> service.confirm(owner, request.getId(), response.getId(), 1)).isInstanceOf(BusinessException.class);
        assertThat(service.points(donorName)).isEqualTo(100);
        assertThat(service.request(request.getId()).getConfirmedUnits()).isEqualTo(1);
    }
    @Test void alertsMatchGroupCityAndPreferenceAndCannotBeReadByOthers() {
        String suffix = UUID.randomUUID().toString().substring(0,8);
        donor("wrong.group."+suffix, BloodGroup.A_POS, city, true);
        donor("wrong.city."+suffix, BloodGroup.O_POS, "Elsewhere", true);
        donor("muted."+suffix, BloodGroup.O_POS, city, false);
        donor("lower.case."+suffix, BloodGroup.O_POS, city.toLowerCase(Locale.ROOT), true);
        BloodRequest request = publish(1);
        List<Notification> matching = notifications.findAll().stream().filter(n -> n.getRequest().getId().equals(request.getId())).toList();
        assertThat(matching).hasSize(2);
        Notification alert = service.alerts(donorName).getFirst();
        assertThatThrownBy(() -> service.readAlert("donor.asha", alert.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(service.readAlert(donorName, alert.getId())).isEqualTo(request.getId());
        assertThat(service.unread(donorName)).isZero();
        assertThat(service.alerts(donorName)).hasSize(1);
    }
    @Test void withdrawAndRespondAgainReusesOneResponseWithoutRewards() {
        BloodRequest request = publish(1); DonorResponse first = service.respond(donorName, request.getId());
        assertThatThrownBy(() -> service.respond(donorName, request.getId())).isInstanceOf(BusinessException.class);
        service.withdraw(donorName, request.getId());
        assertThat(service.request(request.getId()).getStatus()).isEqualTo(RequestStatus.OPEN);
        assertThatThrownBy(() -> service.confirm(owner, request.getId(), first.getId(), 1)).isInstanceOf(BusinessException.class);
        assertThat(service.respond(donorName, request.getId()).getId()).isEqualTo(first.getId());
        assertThat(service.requestResponses(owner, request.getId())).hasSize(1);
        assertThat(service.points(donorName)).isZero();
    }
    @Test void expiredAndCancelledRequestsRejectActionsAndExpiryIsPersisted() {
        BloodRequest cancelled = publish(1); DonorResponse response = service.respond(donorName, cancelled.getId());
        assertThatThrownBy(() -> service.cancel(other, cancelled.getId())).isInstanceOf(AccessDeniedException.class);
        service.cancel(owner, cancelled.getId());
        assertThatThrownBy(() -> service.respond(donorName, cancelled.getId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.confirm(owner, cancelled.getId(), response.getId(), 1)).isInstanceOf(BusinessException.class);
        BloodRequest expired = publish(1); DonorResponse expiredResponse = service.respond(donorName, expired.getId());
        BloodRequest current = service.request(expired.getId());
        current.setRequiredBy(BloodLinkService.now().minusMinutes(1)); requests.save(current);
        assertThat(service.request(expired.getId()).getDisplayStatus()).isEqualTo(RequestStatus.EXPIRED);
        assertThatThrownBy(() -> service.respond(donorName, expired.getId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.confirm(owner, expired.getId(), expiredResponse.getId(), 1)).isInstanceOf(BusinessException.class);
        service.expireRequests(); assertThat(service.request(expired.getId()).getStatus()).isEqualTo(RequestStatus.EXPIRED);
    }
    @Test void responseCannotBeConfirmedAgainstAnotherRequest() {
        BloodRequest one = publish(2), two = publish(2); DonorResponse response = service.respond(donorName, one.getId());
        assertThatThrownBy(() -> service.confirm(owner, two.getId(), response.getId(), 1)).isInstanceOf(AccessDeniedException.class);
        assertThat(service.points(donorName)).isZero();
    }
    @Test void concurrentConfirmationsCannotExceedRemainingTarget() throws Exception {
        String second = "second."+UUID.randomUUID().toString().substring(0,8); donor(second, BloodGroup.O_POS, city, true);
        BloodRequest request = publish(1); DonorResponse a = service.respond(donorName, request.getId()), b = service.respond(second, request.getId());
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Long responseId : List.of(a.getId(), b.getId())) futures.add(pool.submit(() -> {
                start.await();
                try { service.confirm(owner, request.getId(), responseId, 1); return true; }
                catch (BusinessException expected) { return false; }
            }));
            start.countDown(); int successes = 0;
            for (Future<Boolean> future : futures) if (future.get(15, TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
        }
        assertThat(service.request(request.getId()).getConfirmedUnits()).isEqualTo(1);
        assertThat(service.points(donorName) + service.points(second)).isEqualTo(100);
        assertThat(service.donorDonations(donorName).size() + service.donorDonations(second).size()).isEqualTo(1);
    }
    @Test void allPagesRenderForBothRolesAndPrivateHospitalPagesAreProtected() throws Exception {
        for (String url : List.of("/", "/login", "/register")) mvc.perform(get(url)).andExpect(status().isOk());
        for (String url : List.of("/dashboard", "/requests", "/requests?status=ALL", "/donor/profile", "/donor/responses", "/donor/rewards", "/donor/notifications"))
            mvc.perform(get(url).with(user("donor.asha").roles("DONOR"))).andExpect(status().isOk());
        for (String url : List.of("/dashboard", "/requests", "/hospital/profile", "/hospital/requests/new"))
            mvc.perform(get(url).with(user("hospital.care").roles("HOSPITAL"))).andExpect(status().isOk());
        BloodRequest request = publish(1); service.respond(donorName, request.getId());
        mvc.perform(get("/requests/"+request.getId()).with(user(owner).roles("HOSPITAL"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("9000012345")));
        mvc.perform(get("/requests/"+request.getId()).with(user("donor.asha").roles("DONOR"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("9000012345"))));
        mvc.perform(get("/requests/"+request.getId()).with(user(other).roles("HOSPITAL"))).andExpect(status().isForbidden());
        mvc.perform(get("/hospital/requests/new").with(user(donorName).roles("DONOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/notifications/count").with(user(donorName).roles("DONOR"))).andExpect(status().isOk()).andExpect(jsonPath("$.unread").value(1));
    }
    @Test void csrfAuthenticationAndInputValidationAreEnforced() throws Exception {
        BloodRequest request = publish(1);
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/donor/requests/"+request.getId()+"/respond").with(user(donorName).roles("DONOR"))).andExpect(status().isForbidden());
        mvc.perform(post("/donor/requests/"+request.getId()+"/respond").with(user(donorName).roles("DONOR")).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/hospital/requests").with(user(owner).roles("HOSPITAL")).with(csrf()).param("bloodGroup","O_POS").param("unitsRequested","0").param("urgency","URGENT").param("requiredBy","2020-01-01T10:00"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","unitsRequested","requiredBy"));
        mvc.perform(post("/register").with(csrf()).param("username","bad").param("password","short").param("fullName"," "))
            .andExpect(status().isOk()).andExpect(model().hasErrors());
        mvc.perform(post("/login").with(csrf()).param("username","donor.asha").param("password","Demo@12345")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
        assertThat(passwords.matches("Demo@12345", service.user(donorName).getPasswordHash())).isTrue();
        assertThat(service.user(donorName).getPasswordHash()).doesNotContain("Demo@12345");
    }
    @Test void approximateDistanceIsOptionalAndCorrectForSamePoint() {
        DonorProfile donor = service.donor(donorName); HospitalProfile hospital = service.hospital(owner);
        assertThat(service.distanceKm(donor,hospital)).isNull();
        donor.setLatitude(hospital.getLatitude()); donor.setLongitude(hospital.getLongitude());
        assertThat(service.distanceKm(donor,hospital)).isEqualTo(0.0);
    }
}
