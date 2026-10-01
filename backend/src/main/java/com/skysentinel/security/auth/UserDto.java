package com.skysentinel.security.auth;

public record UserDto(
        Long id,
        String email,
        String fullName,
        String cellphoneNumber,
        String role,
        String badgeNumber
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCellphoneNumber(),
                user.getRole(),
                user.getBadgeNumber()
        );
    }
}
