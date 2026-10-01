package com.skysentinel.security.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "panic_buttons")
public class PanicButton {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, name = "device_code")
    private String deviceCode;
    @Column(nullable = false, name = "site_code")
    private String siteCode;
    @Column(nullable = false)
    private String zone;
    private Boolean active = true;

    public PanicButton() {}

    public PanicButton(String deviceCode, String siteCode, String zone) {
        this.deviceCode = deviceCode;
        this.siteCode = siteCode;
        this.zone = zone;
        this.active = true;
    }

    public Long getId() { return id; }
    public String getDeviceCode() { return deviceCode; }
    public String getSiteCode() { return siteCode; }
    public String getZone() { return zone; }
    public Boolean getActive() { return active; }
}
