package com.skysentinel.security.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PanicButtonRepository extends JpaRepository<PanicButton, Long> {
    Optional<PanicButton> findByDeviceCode(String deviceCode);
}
