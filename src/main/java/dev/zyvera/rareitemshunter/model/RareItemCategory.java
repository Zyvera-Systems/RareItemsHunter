package dev.zyvera.rareitemshunter.model;

import java.util.Locale;

public enum RareItemCategory {
    ITEM,
    MOB,
    STRUCTURE,
    CUSTOM;

    public static RareItemCategory fromConfig(String raw) {
        if (raw == null || raw.isBlank()) return CUSTOM;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "item", "items" -> ITEM;
            case "mob", "mobs", "monster", "monsters" -> MOB;
            case "structure", "structures", "struktur", "strukturen" -> STRUCTURE;
            case "custom", "individuell", "individual", "individuals" -> CUSTOM;
            default -> CUSTOM;
        };
    }
}
