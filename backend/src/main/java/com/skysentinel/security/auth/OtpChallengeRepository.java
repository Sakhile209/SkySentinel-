package com.skysentinel.security.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {
    Optional<OtpChallenge> findByChallengeId(String challengeId);
    List<OtpChallenge> findByUserAndUsedAtIsNull(User user);
}
