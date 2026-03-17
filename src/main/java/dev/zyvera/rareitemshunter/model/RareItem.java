package dev.zyvera.rareitemshunter.model;

import org.bukkit.Material;

public record RareItem(
        String id,
        Material material,
        String displayName,
        int rarity,
        String chance,
        String description,
        boolean occurrenceOnly,
        RareItemCategory category
) {
    public RareItem(String id, Material material, String displayName, int rarity, String chance, String description) {
        this(id, material, displayName, rarity, chance, description, false, RareItemCategory.ITEM);
    }

    public RareItem(String id, Material material, String displayName, int rarity, String chance,
                    String description, boolean occurrenceOnly) {
        this(id, material, displayName, rarity, chance, description, occurrenceOnly, RareItemCategory.ITEM);
    }

    public boolean isCustom() {
        return category == RareItemCategory.CUSTOM || id.startsWith("custom_");
    }
}
