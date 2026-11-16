package com.campusshare.resource.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "campus_resource")
public class CampusResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceStatus status = ResourceStatus.ACTIVE;

    protected CampusResource() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public ResourceType getType() {
        return type;
    }

    public ResourceStatus getStatus() {
        return status;
    }
}
