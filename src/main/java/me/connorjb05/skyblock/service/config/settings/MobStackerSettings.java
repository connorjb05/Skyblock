package me.connorjb05.skyblock.service.config.settings;

public record MobStackerSettings(
        boolean enabled,
        double radius,
        double radiusY,
        String name
) {
}
