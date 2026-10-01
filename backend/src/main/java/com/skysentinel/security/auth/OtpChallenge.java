package com.skysentinel.security.auth;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, name = "challenge_id")
    private String challengeId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, name = "code_hash")
    private String codeHash;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false, name = "resend_count")
    private Integer resendCount = 0;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "last_sent_at")
    private Instant lastSentAt = Instant.now();

    public OtpChallenge() {}

    public OtpChallenge(String challengeId, User user, String codeHash, Instant expiresAt) {
        this.challengeId = challengeId;
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.lastSentAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getChallengeId() { return challengeId; }
    public User getUser() { return user; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public Integer getResendCount() { return resendCount; }
    public void setResendCount(Integer resendCount) { this.resendCount = resendCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
}
