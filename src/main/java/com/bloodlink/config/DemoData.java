package com.bloodlink.config;

import com.bloodlink.controller.form.*;
import com.bloodlink.model.*;
import com.bloodlink.repository.*;
import com.bloodlink.service.BloodLinkService;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

/** Fictional classroom data. Existing databases are never reset on startup. */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoData implements CommandLineRunner {
    private final AppUserRepository users;
    private final HospitalProfileRepository hospitals;
    private final BloodLinkService service;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    @Value("${app.seed-lock:false}") private boolean seedLock;
    public DemoData(AppUserRepository users, HospitalProfileRepository hospitals, BloodLinkService service, PasswordEncoder passwords, JdbcTemplate jdbc) {
        this.users = users; this.hospitals = hospitals; this.service = service; this.passwords = passwords;
        this.jdbc = jdbc;
    }
    @Override @Transactional
    public void run(String... args) {
        // Two cold-starting cloud instances must not seed the same empty database twice.
        if (seedLock) jdbc.execute("LOCK TABLE app_users IN SHARE ROW EXCLUSIVE MODE");
        if (users.count() > 0) return;
        hospital("hospital.care", "Carebridge Hospital", "24, Garden Avenue, Indiranagar", 12.9784, 77.6408, "+91 80000 10001");
        hospital("hospital.lotus", "Lotus Community Hospital", "18, Lakeview Road, Jayanagar", 12.9250, 77.5938, "+91 80000 10002");
        donor("donor.asha", "Asha Rao", BloodGroup.O_POS, "Bengaluru", 12.9716, 77.5946, 1);
        donor("donor.rahul", "Rahul Shah", BloodGroup.O_POS, "Bengaluru", 12.9352, 77.6245, 2);
        donor("donor.meera", "Meera Nair", BloodGroup.A_POS, "Bengaluru", null, null, 3);
        donor("donor.vikram", "Vikram Das", BloodGroup.B_POS, "Bengaluru", null, null, 4);
        donor("donor.sana", "Sana Ali", BloodGroup.AB_NEG, "Bengaluru", null, null, 5);
        donor("donor.arjun", "Arjun Roy", BloodGroup.O_NEG, "Mumbai", null, null, 6);
        publish("hospital.care", BloodGroup.O_POS, 3, Urgency.URGENT, 2, "Please report to the donation desk near the main reception. Call the hospital to arrange your visit.");
        publish("hospital.lotus", BloodGroup.A_POS, 2, Urgency.NORMAL, 4, "Our donation desk is open during hospital hours. Please contact us before visiting.");
        publish("hospital.care", BloodGroup.B_POS, 1, Urgency.URGENT, 1, "Visit the blood donation centre on the ground floor. Hospital screening is required.");
        publish("hospital.lotus", BloodGroup.O_NEG, 2, Urgency.URGENT, 3, "Contact our donation coordinator for directions and scheduling.");
        BloodRequest completed = publish("hospital.care", BloodGroup.O_POS, 1, Urgency.NORMAL, 5, "A fulfilled demonstration request showing confirmation and rewards.");
        DonorResponse response = service.respond("donor.asha", completed.getId());
        service.confirm("hospital.care", completed.getId(), response.getId(), 1);
    }
    private void hospital(String username, String name, String address, double lat, double lng, String phone) {
        AppUser user = new AppUser(); user.setUsername(username); user.setPasswordHash(passwords.encode("Demo@12345")); user.setRole(Role.HOSPITAL); users.save(user);
        HospitalProfile hospital = new HospitalProfile(); hospital.setUser(user); hospital.setName(name); hospital.setAddress(address);
        hospital.setCity("Bengaluru"); hospital.setPhone(phone); hospital.setLatitude(lat); hospital.setLongitude(lng); hospitals.save(hospital);
    }
    private void donor(String username, String name, BloodGroup group, String city, Double lat, Double lng, int index) {
        RegistrationForm form = new RegistrationForm(); form.setUsername(username); form.setPassword("Demo@12345");
        form.setFullName(name); form.setDateOfBirth(LocalDate.of(2000, 4, index)); form.setBloodGroup(group);
        form.setPhone("+91 90000 1000" + index); form.setEmail(username + "@example.com"); form.setCity(city);
        form.setLatitude(lat); form.setLongitude(lng); service.register(form);
    }
    private BloodRequest publish(String username, BloodGroup group, int units, Urgency urgency, int days, String description) {
        RequestForm form = new RequestForm(); form.setBloodGroup(group); form.setUnitsRequested(units); form.setUrgency(urgency);
        form.setRequiredBy(BloodLinkService.now().plusDays(days)); form.setDescription(description); return service.publish(username, form);
    }
}
