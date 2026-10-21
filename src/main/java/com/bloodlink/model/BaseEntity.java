package com.bloodlink.model;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    public Long getId() { return id; }
}
