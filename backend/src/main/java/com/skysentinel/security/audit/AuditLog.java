package com.skysentinel.security.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "event_type")
    private String eventType;

    @Column(nullable = false)
    private String details;

    @Column(nullable = false)
    private String actor;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public AuditLog() {}

    public AuditLog(String eventType, String details, String actor) {
        this.eventType = eventType;
        this.details = details;
        this.actor = actor;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public String getDetails() { return details; }
    public String getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
}
