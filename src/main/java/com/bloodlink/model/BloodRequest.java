package com.bloodlink.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class BloodRequest extends BaseEntity {
    @ManyToOne(optional = false)
    private HospitalProfile hospital;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private BloodGroup bloodGroup;
    @Column(nullable = false)
    private int unitsRequested;
    @Column(nullable = false)
    private int confirmedUnits;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Urgency urgency;
    @Column(nullable = false)
    private LocalDateTime requiredBy;
    @Column(length = 1000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RequestStatus status = RequestStatus.OPEN;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    public HospitalProfile getHospital() { return hospital; }
    public void setHospital(HospitalProfile hospital) { this.hospital = hospital; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public int getUnitsRequested() { return unitsRequested; }
    public void setUnitsRequested(int unitsRequested) { this.unitsRequested = unitsRequested; }
    public int getConfirmedUnits() { return confirmedUnits; }
    public void setConfirmedUnits(int confirmedUnits) { this.confirmedUnits = confirmedUnits; }
    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public LocalDateTime getRequiredBy() { return requiredBy; }
    public void setRequiredBy(LocalDateTime requiredBy) { this.requiredBy = requiredBy; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAccepting() {
        return (status == RequestStatus.OPEN || status == RequestStatus.IN_PROGRESS)
            && requiredBy.isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")));
    }
    public int getRemainingUnits() { return Math.max(0, unitsRequested - confirmedUnits); }
    public int getProgressPercent() { return unitsRequested == 0 ? 0 : confirmedUnits * 100 / unitsRequested; }
    public RequestStatus getDisplayStatus() {
        return (status == RequestStatus.OPEN || status == RequestStatus.IN_PROGRESS) && !isAccepting()
            ? RequestStatus.EXPIRED : status;
    }
}

