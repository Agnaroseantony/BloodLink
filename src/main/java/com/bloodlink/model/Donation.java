package com.bloodlink.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Donation extends BaseEntity {
    @OneToOne(optional = false) @JoinColumn(unique = true)
    private DonorResponse response;
    @Column(nullable = false)
    private int units;
    @Column(nullable = false)
    private LocalDateTime confirmedAt;
    public DonorResponse getResponse() { return response; }
    public void setResponse(DonorResponse response) { this.response = response; }
    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}

