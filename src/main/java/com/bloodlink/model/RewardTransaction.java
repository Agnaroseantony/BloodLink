package com.bloodlink.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class RewardTransaction extends BaseEntity {
    @OneToOne(optional = false) @JoinColumn(unique = true)
    private Donation donation;
    @Column(nullable = false)
    private int points;
    public Donation getDonation() { return donation; }
    public void setDonation(Donation donation) { this.donation = donation; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}

