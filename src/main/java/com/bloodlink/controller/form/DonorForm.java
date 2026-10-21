package com.bloodlink.controller.form;

import com.bloodlink.model.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;

public class DonorForm {
    @NotBlank @Size(max = 80)
    private String fullName;
    @NotNull @Past @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;
    @NotNull
    private BloodGroup bloodGroup;
    @NotBlank @Pattern(regexp = "[+0-9 ()-]{7,20}", message = "Enter a valid phone number (7–20 characters).")
    private String phone;
    @Email @Size(max = 120)
    private String email;
    @NotBlank @Size(max = 80)
    private String city;
    @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;
    @DecimalMin("-180") @DecimalMax("180")
    private Double longitude;
    @PastOrPresent @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lastDonationDate;
    
    private boolean notificationsEnabled = true;
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public LocalDate getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(LocalDate lastDonationDate) { this.lastDonationDate = lastDonationDate; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    @AssertTrue(message = "Provide both coordinates or leave both empty.")
    public boolean isCoordinatePairValid() { return (latitude == null) == (longitude == null); }
    @AssertTrue(message = "Last donation date must follow your date of birth.")
    public boolean isDonationDateValid() { return lastDonationDate == null || dateOfBirth == null || lastDonationDate.isAfter(dateOfBirth); }
}

