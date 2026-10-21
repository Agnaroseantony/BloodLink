package com.bloodlink.model;
public enum RequestStatus {
    OPEN("Open"), IN_PROGRESS("In progress"), FULFILLED("Fulfilled"), CANCELLED("Cancelled"), EXPIRED("Expired");
    private final String label;
    RequestStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
