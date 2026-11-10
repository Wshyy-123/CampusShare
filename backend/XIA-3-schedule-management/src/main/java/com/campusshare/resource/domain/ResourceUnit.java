package com.campusshare.resource.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "resource_unit")
public class ResourceUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "unit_code", nullable = false, length = 64)
    private String unitCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceStatus status = ResourceStatus.ACTIVE;

    protected ResourceUnit() {
    }

    public Long getId() {
        return id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getName() {
        return name;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public ResourceStatus getStatus() {
        return status;
    }
}
