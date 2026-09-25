package com.skysentinel.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SkySentinelApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkySentinelApplication.class, args);
    }
}
