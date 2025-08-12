package com.muebleria.mctecommercebackend.model;

import java.util.stream.Stream;

public enum Role {
    DEVELOPER(100),
    ADMIN(90),
    GESTOR_SUCURSAL(50),
    GESTOR_INVENTARIO(50),
    VENDEDOR(20),
    DRIVER(20),
    AGENT(10);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static Role fromString(String roleName) {
        if (roleName == null) return null;
        return Stream.of(Role.values())
                .filter(r -> r.name().equalsIgnoreCase(roleName.replace("ROLE_", "")))
                .findFirst()
                .orElse(null);
    }
}