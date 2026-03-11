package dev.zyvera.rareitemshunter.model;

import org.bukkit.Material;

public record RareItem(
        String   id,
        Material material,
        String   displayName,
        int      rarity,
        String   chance,
        String   description,
        boolean  occurrenceOnly
) {
    public RareItem(String id, Material material, String displayName, int rarity, String chance, String description) {
        this(id, material, displayName, rarity, chance, description, false);
    }

    public boolean isCustom() {
        return id.startsWith("custom_");
    }
}
