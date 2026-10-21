package com.bloodlink.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class HospitalProfile extends BaseEntity {
    @OneToOne(optional = false) @JoinColumn(unique = true)
    private AppUser user;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 250)
    private String address;
    @Column(nullable = false, length = 80)
    private String city;
    @Column(nullable = false, length = 20)
    private String phone;
    
    private double latitude;
    
    private double longitude;
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}

