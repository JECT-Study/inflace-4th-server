package com.example.inflace.domain.user.domain.enums;

public enum Plan {
    FREE,
    PRO,
    ADMIN;

    public String toSpringRole() {
        return "ROLE_" + name();
    }
}
