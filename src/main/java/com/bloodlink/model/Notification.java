package com.bloodlink.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "donor_id"}))
public class Notification extends BaseEntity {
    @ManyToOne(optional = false)
    private BloodRequest request;
    @ManyToOne(optional = false)
    private DonorProfile donor;
    @Column(nullable = false)
    private boolean seen;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    public BloodRequest getRequest() { return request; }
    public void setRequest(BloodRequest request) { this.request = request; }
    public DonorProfile getDonor() { return donor; }
    public void setDonor(DonorProfile donor) { this.donor = donor; }
    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

