package com.bloodlink.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "donor_id"}))
public class DonorResponse extends BaseEntity {
    @ManyToOne(optional = false)
    private BloodRequest request;
    @ManyToOne(optional = false)
    private DonorProfile donor;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ResponseStatus status;
    @Column(nullable = false)
    private LocalDateTime respondedAt;
    public BloodRequest getRequest() { return request; }
    public void setRequest(BloodRequest request) { this.request = request; }
    public DonorProfile getDonor() { return donor; }
    public void setDonor(DonorProfile donor) { this.donor = donor; }
    public ResponseStatus getStatus() { return status; }
    public void setStatus(ResponseStatus status) { this.status = status; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}

