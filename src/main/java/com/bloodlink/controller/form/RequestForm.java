package com.bloodlink.controller.form;

import com.bloodlink.model.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;

public class RequestForm {
    @NotNull
    private BloodGroup bloodGroup;
    @NotNull @Min(1) @Max(100)
    private Integer unitsRequested;
    @NotNull
    private Urgency urgency;
    @NotNull @Future @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime requiredBy;
    @Size(max = 1000)
    private String description;
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public Integer getUnitsRequested() { return unitsRequested; }
    public void setUnitsRequested(Integer unitsRequested) { this.unitsRequested = unitsRequested; }
    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public LocalDateTime getRequiredBy() { return requiredBy; }
    public void setRequiredBy(LocalDateTime requiredBy) { this.requiredBy = requiredBy; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

