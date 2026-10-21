package com.bloodlink.controller.form;

import com.bloodlink.model.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;

public class HospitalForm {
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Size(max = 250)
    private String address;
    @NotBlank @Size(max = 80)
    private String city;
    @NotBlank @Pattern(regexp = "[+0-9 ()-]{7,20}")
    private String phone;
    @NotNull @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;
    @NotNull @DecimalMin("-180") @DecimalMax("180")
    private Double longitude;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}

