package com.skysentinel.security.infrastructure;

import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "operational_areas")
class OperationalArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String description;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public OperationalArea() {}
    public OperationalArea(String name, String description) {
        this.name = name;
        this.description = description;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}

@Entity
@Table(name = "drone_bases")
class DroneBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, name = "base_type")
    private String baseType;
    @Column(nullable = false, name = "operational_area")
    private String operationalArea;
    private Double x;
    private Double y;

    public DroneBase() {}
    public DroneBase(String code, String name, String baseType, String operationalArea, Double x, Double y) {
        this.code = code;
        this.name = name;
        this.baseType = baseType;
        this.operationalArea = operationalArea;
        this.x = x;
        this.y = y;
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getBaseType() { return baseType; }
    public String getOperationalArea() { return operationalArea; }
    public Double getX() { return x; }
    public Double getY() { return y; }
}

interface OperationalAreaRepository extends JpaRepository<OperationalArea, Long> {}
interface DroneBaseRepository extends JpaRepository<DroneBase, Long> {}
