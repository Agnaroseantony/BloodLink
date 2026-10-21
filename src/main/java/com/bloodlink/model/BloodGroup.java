package com.bloodlink.model;
public enum BloodGroup {
    A_POS("A+"), A_NEG("A−"), B_POS("B+"), B_NEG("B−"),
    AB_POS("AB+"), AB_NEG("AB−"), O_POS("O+"), O_NEG("O−");
    private final String label;
    BloodGroup(String label) { this.label = label; }
    public String getLabel() { return label; }
}
