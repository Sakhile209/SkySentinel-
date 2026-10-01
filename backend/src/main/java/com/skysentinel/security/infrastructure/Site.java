package com.skysentinel.security.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "sites")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, name = "operational_area")
    private String operationalArea;
    private String address;
    @Column(name = "has_drone_coverage")
    private Boolean hasDroneCoverage = true;
    @Column(name = "drone_base_code")
    private String droneBaseCode;
    private Double x;
    private Double y;

    public Site() {}

    public Site(String code, String name, String operationalArea, String address, Boolean hasDroneCoverage, String droneBaseCode, Double x, Double y) {
        this.code = code;
        this.name = name;
        this.operationalArea = operationalArea;
        this.address = address;
        this.hasDroneCoverage = hasDroneCoverage;
        this.droneBaseCode = droneBaseCode;
        this.x = x;
        this.y = y;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getOperationalArea() { return operationalArea; }
    public String getAddress() { return address; }
    public Boolean getHasDroneCoverage() { return hasDroneCoverage; }
    public String getDroneBaseCode() { return droneBaseCode; }
    public Double getX() { return x; }
    public Double getY() { return y; }
}
