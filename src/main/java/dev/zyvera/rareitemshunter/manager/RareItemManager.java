package dev.zyvera.rareitemshunter.manager;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.model.RareItem;
import dev.zyvera.rareitemshunter.model.RareItemCategory;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RareItemManager {

    private static final String DEFAULT_ITEMS_PATH = "items";
    private static final String CUSTOM_ITEMS_PATH = "custom-items";

    private final RareItemsHunter plugin;
    private volatile List<RareItem> items = List.of();
    private volatile Map<RareItemCategory, List<RareItem>> itemsByCategory = Map.of();
    private volatile Map<String, RareItem> itemsById = Map.of();

    // { id, Material, displayName, rarityRank, shortChance, shortDescription, occurrenceOnly, category }
    private static final Object[][] DEFAULTS = {
        { "zombie_villager_chicken_diamond", Material.IRON_SWORD,
          "&c&lBaby-Zombie-Dorfbewohner auf Huhn (volle Dia-Rüstung)", 1,
          "~0.000000000000000000000000000000034 %",
          "Seltenster Mob, der je dokumentiert wurde", true, RareItemCategory.MOB },

        { "endportal_12_eye", Material.ENDER_EYE,
          "&5&l12-Augen-Endportal", 2, "~0.000000000001 %",
          "Alle 12 Rahmen natürlich befüllt", true, RareItemCategory.STRUCTURE },

        { "dragon_egg", Material.DRAGON_EGG,
          "&5&lDrachenei", 3, "1x pro Welt",
          "Nur nach dem ersten Enderdrachen-Sieg", false, RareItemCategory.ITEM },

        { "baby_pink_sheep", Material.PINK_DYE,
          "&d&lBaby-Pinkes Schaf", 4, "~0.0082 %",
          "0.164 % rosa Spawn × 5 % Baby", true, RareItemCategory.MOB },

        { "blue_axolotl", Material.AXOLOTL_BUCKET,
          "&b&lBlaues Axolotl", 5, "~0.083 %",
          "1 von 1200 Axolotln ist blau", false, RareItemCategory.MOB },

        { "trident", Material.TRIDENT,
          "&9&lDreizack", 6, "~0.53 %",
          "Seltener Drop von Ertrunkenen", false, RareItemCategory.ITEM },

        { "wither_skeleton_skull", Material.WITHER_SKELETON_SKULL,
          "&0&lWither-Skelett-Schädel", 7, "~2.5 %",
          "Benötigt 3 Stück für die Wither-Beschwörung", false, RareItemCategory.ITEM },

        { "silence_armor_trim", Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
          "&8&lSilence-Rüstungsbesatz", 8, "~1.2 %",
          "Alte Stadt – einer der seltensten Item-Funde", false, RareItemCategory.ITEM },

        { "music_disc_creator_music_box", Material.MUSIC_DISC_CREATOR_MUSIC_BOX,
          "&d&lMusikscheibe \"Creator (Music Box)\"", 9, "~1.4 %",
          "Dekorationstopf in der Prüfungskammer", false, RareItemCategory.ITEM },

        { "music_disc_otherside", Material.MUSIC_DISC_OTHERSIDE,
          "&b&lMusikscheibe \"otherside\"", 10, "~3.1 %",
          "Verlies- und Festungstruhen", false, RareItemCategory.ITEM },

        { "music_disc_creator", Material.MUSIC_DISC_CREATOR,
          "&e&lMusikscheibe \"Creator\"", 11, "~3.0 %",
          "Unheilvoller Tresor", false, RareItemCategory.ITEM },

        { "music_disc_precipice", Material.MUSIC_DISC_PRECIPICE,
          "&6&lMusikscheibe \"Precipice\"", 12, "~3.6 %",
          "Prüfungskammer-Tresor", false, RareItemCategory.ITEM },

        { "music_disc_pigstep", Material.MUSIC_DISC_PIGSTEP,
          "&c&lMusikscheibe \"Pigstep\"", 13, "~5.6 %",
          "Bastionsüberrest – nicht per Creeper", false, RareItemCategory.ITEM },

        { "music_disc_5", Material.MUSIC_DISC_5,
          "&3&lMusikscheibe \"5\"", 14, "~0.8 % pro Fragment",
          "9 Fragmente aus Truhen der Alten Stadt", false, RareItemCategory.ITEM },

        { "music_disc_relic", Material.MUSIC_DISC_RELIC,
          "&6&lMusikscheibe \"Relic\"", 15, "~7.7 %",
          "Verdächtiger Kies in Pfadruinen", false, RareItemCategory.ITEM },

        { "music_disc_11", Material.MUSIC_DISC_11,
          "&c&lMusikscheibe \"11\"", 16, "~0.5 %",
          "Creeper muss von einem Skelett getötet werden", false, RareItemCategory.ITEM },

        { "enchanted_golden_apple", Material.ENCHANTED_GOLDEN_APPLE,
          "&6&lVerzauberter Goldapfel", 17, "~2.5 %",
          "Festung, Bastion oder Ruiniertes Portal", false, RareItemCategory.ITEM },

        { "heavy_core", Material.HEAVY_CORE,
          "&8&lSchwerer Kern", 18, "~7.5 %",
          "Unheilvoller Tresor der Prüfungskammer", false, RareItemCategory.ITEM },

        { "heart_of_the_sea", Material.HEART_OF_THE_SEA,
          "&3&lHerz des Meeres", 19, "100 %",
          "1x pro versteckter Schatztruhe", false, RareItemCategory.ITEM },

        { "echo_shard", Material.ECHO_SHARD,
          "&3&lEcho-Scherbe", 20, "~29.8 %",
          "Truhen in der Alten Stadt", false, RareItemCategory.ITEM },

        { "sniffer_egg", Material.SNIFFER_EGG,
          "&a&lSchnüffler-Ei", 21, "~6.7 %",
          "Warme Meeresruinen", false, RareItemCategory.ITEM },

        { "bolt_armor_trim", Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
          "&b&lBolt-Rüstungsbesatz", 22, "~6.2 %",
          "Tresor in der Prüfungskammer", false, RareItemCategory.ITEM },

        { "spire_armor_trim", Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
          "&d&lSpire-Rüstungsbesatz", 23, "~6.7 %",
          "Truhe in der Endstadt", false, RareItemCategory.ITEM },

        { "snout_armor_trim", Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
          "&6&lSnout-Rüstungsbesatz", 24, "~8.3 %",
          "Bastionsüberrest-Truhe", false, RareItemCategory.ITEM },

        { "ominous_trial_key", Material.OMINOUS_TRIAL_KEY,
          "&4&lUnheilvoller Prüfungsschlüssel", 25, "~30 %",
          "Unheilvoller Spawner (Bad Omen benötigt)", false, RareItemCategory.ITEM },

        { "breeze_rod", Material.BREEZE_ROD,
          "&b&lBreeze-Stab", 26, "~33 % (1–2 Drops)",
          "Nur in Prüfungskammern", false, RareItemCategory.ITEM },

        { "ancient_debris", Material.ANCIENT_DEBRIS,
          "&8&lUralter Schutt", 27, "~1.7 pro Chunk",
          "Nether, y=15 ist optimal", false, RareItemCategory.ITEM },

        { "nether_star", Material.NETHER_STAR,
          "&f&lNetherstern", 28, "100 % beim Wither",
          "Wither muss beschworen werden", false, RareItemCategory.ITEM },

        { "elytra", Material.ELYTRA,
          "&6&lElytra", 29, "100 %",
          "1x pro Endstadtschiff", false, RareItemCategory.ITEM },

        { "tide_armor_trim", Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
          "&3&lTide-Rüstungsbesatz", 30, "~20 %",
          "Drop vom Ältesten Wächter", false, RareItemCategory.ITEM },

        { "rabbit_foot", Material.RABBIT_FOOT,
          "&e&lHasenpfote", 31, "~10 %",
          "Drop bei Hasen (mit Plünderung höher)", false, RareItemCategory.ITEM },

        { "brown_mooshroom", Material.BROWN_MUSHROOM,
          "&6&lBraune Pilzkuh", 32, "~0.098 %",
          "1/1024 bei Zucht von zwei roten Pilzkühen", true, RareItemCategory.MOB },

        { "brown_panda", Material.BAMBOO,
          "&6&lBrauner Panda", 33, "~1.563 %",
          "Braune Haupt- und versteckte Gene gleichzeitig", true, RareItemCategory.MOB },

        { "baby_brown_sheep", Material.BROWN_DYE,
          "&6&lBaby-Braunes Schaf", 34, "~0.15 %",
          "3 % braun × 5 % Baby", true, RareItemCategory.MOB },

        { "pink_sheep", Material.PINK_WOOL,
          "&d&lPinkes Schaf", 35, "~0.164 %",
          "Natürlicher Spawn", true, RareItemCategory.MOB },

        { "brown_sheep", Material.BROWN_WOOL,
          "&6&lBraunes Schaf", 36, "~2.85 %",
          "Natürlicher Spawn", true, RareItemCategory.MOB },

        { "spider_jockey", Material.COBWEB,
          "&8&lSpinnen-Jockey", 37, "~1 %",
          "Skelett reitet auf Spinne", true, RareItemCategory.MOB },

        { "chicken_jockey", Material.FEATHER,
          "&a&lHuhn-Jockey", 38, "~0.25 %",
          "Baby-Zombie reitet auf Huhn", true, RareItemCategory.MOB },

        { "skeleton_horse_trap", Material.SKELETON_HORSE_SPAWN_EGG,
          "&7&lSkelettpferd-Falle", 39, "~0.75–6.75 %",
          "Natürliches Gewitter-Blitzereignis", true, RareItemCategory.MOB },

        { "charged_creeper", Material.GUNPOWDER,
          "&b&lGeladener Creeper", 40, "sehr selten",
          "Blitz trifft Creeper in der Nähe", true, RareItemCategory.MOB },

        { "cactus_5high", Material.CACTUS,
          "&2&lKaktus – 5 Blöcke hoch", 41, "~0.003 %",
          "Benötigt zusätzliche Wachstumszyklen", true, RareItemCategory.ITEM },
    };

    public RareItemManager(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    public synchronized void reload() {
        ensureEditableDefaultItems();
        migrateBuiltInCategories();

        List<RareItem> loadedItems = new ArrayList<>();
        loadConfiguredItems(loadedItems, DEFAULT_ITEMS_PATH, false, RareItemCategory.ITEM);
        loadConfiguredItems(loadedItems, CUSTOM_ITEMS_PATH, true, RareItemCategory.CUSTOM);

        loadedItems.sort(Comparator.comparingInt(RareItem::rarity));

        List<RareItem> immutableItems = List.copyOf(loadedItems);
        EnumMap<RareItemCategory, List<RareItem>> categorized = new EnumMap<>(RareItemCategory.class);
        for (RareItemCategory category : RareItemCategory.values()) {
            categorized.put(category, immutableItems.stream()
                    .filter(item -> item.category() == category)
                    .toList());
        }

        Map<String, RareItem> idIndex = new HashMap<>();
        for (RareItem item : immutableItems) {
            idIndex.put(item.id(), item);
        }

        this.items = immutableItems;
        this.itemsByCategory = Map.copyOf(categorized);
        this.itemsById = Map.copyOf(idIndex);

        plugin.getLogger().info("Loaded " + immutableItems.size() + " rare items.");
    }

    private void migrateBuiltInCategories() {
        List<Map<?, ?>> configured = plugin.getConfig().getMapList(DEFAULT_ITEMS_PATH);
        if (configured.isEmpty()) {
            return;
        }

        Map<String, String> expectedCategories = new HashMap<>();
        for (Object[] row : DEFAULTS) {
            expectedCategories.put(String.valueOf(row[0]), toConfigCategory((RareItemCategory) row[7]));
        }

        boolean changed = false;
        List<Map<String, Object>> migrated = new ArrayList<>();
        for (Map<?, ?> raw : configured) {
            Map<String, Object> entry = new LinkedHashMap<>();
            for (Map.Entry<?, ?> mapEntry : raw.entrySet()) {
                entry.put(String.valueOf(mapEntry.getKey()), mapEntry.getValue());
            }

            String id = String.valueOf(entry.getOrDefault("id", "")).trim();
            String expected = expectedCategories.get(id);
            if (expected != null) {
                Object current = entry.get("category");
                String normalizedCurrent = toConfigCategory(RareItemCategory.fromConfig(current == null ? null : String.valueOf(current)));
                if (!expected.equalsIgnoreCase(normalizedCurrent)) {
                    entry.put("category", expected);
                    changed = true;
                }
            }
            migrated.add(entry);
        }

        if (changed) {
            plugin.getConfig().set(DEFAULT_ITEMS_PATH, migrated);
            plugin.saveConfig();
            plugin.getLogger().info("Updated built-in item categories in config.yml");
        }
    }

    private void ensureEditableDefaultItems() {
        if (plugin.getConfig().isSet(DEFAULT_ITEMS_PATH) && !plugin.getConfig().getMapList(DEFAULT_ITEMS_PATH).isEmpty()) {
            return;
        }

        plugin.getConfig().set(DEFAULT_ITEMS_PATH, buildDefaultConfigEntries());
        plugin.saveConfig();
        plugin.getLogger().info("Generated editable built-in rare items in config.yml");
    }

    private List<Map<String, Object>> buildDefaultConfigEntries() {
        List<Map<String, Object>> defaults = new ArrayList<>();
        for (Object[] row : DEFAULTS) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", row[0]);
            entry.put("material", ((Material) row[1]).name());
            entry.put("name", row[2]);
            entry.put("rarity", row[3]);
            entry.put("chance", row[4]);
            entry.put("description", row[5]);
            entry.put("occurrence-only", row[6]);
            entry.put("category", toConfigCategory((RareItemCategory) row[7]));
            defaults.add(entry);
        }
        return defaults;
    }

    private void loadConfiguredItems(List<RareItem> target, String path, boolean prefixCustomId, RareItemCategory defaultCategory) {
        int fallback = maxRarity(target) + 1;
        for (Map<?, ?> raw : plugin.getConfig().getMapList(path)) {
            Object idValue = raw.containsKey("id") ? raw.get("id") : "item_" + fallback;
            String baseId = String.valueOf(idValue).trim();
            if (baseId.isEmpty()) {
                baseId = "item_" + fallback;
            }

            String id = prefixCustomId && !baseId.startsWith("custom_") ? "custom_" + baseId : baseId;
            Object materialValue = raw.containsKey("material") ? raw.get("material") : "STONE";
            String matName = String.valueOf(materialValue).trim().toUpperCase(Locale.ROOT);
            Object nameValue = raw.containsKey("name") ? raw.get("name") : "&7Unknown Item";
            String name = String.valueOf(nameValue);
            Object chanceValue = raw.containsKey("chance") ? raw.get("chance") : "?";
            String chance = String.valueOf(chanceValue);
            Object descriptionValue = raw.containsKey("description") ? raw.get("description") : "";
            String desc = String.valueOf(descriptionValue);
            Object occurrenceValue = raw.containsKey("occurrence-only") ? raw.get("occurrence-only") : "false";
            boolean occurrenceOnly = Boolean.parseBoolean(String.valueOf(occurrenceValue));

            String rawCategory = raw.containsKey("category")
                    ? String.valueOf(raw.get("category"))
                    : toConfigCategory(defaultCategory);
            RareItemCategory category = RareItemCategory.fromConfig(rawCategory);

            int rarity = parseRarity(raw.get("rarity"), fallback);
            if (!raw.containsKey("rarity")) {
                fallback++;
            }

            try {
                target.add(new RareItem(id, Material.valueOf(matName), name, rarity, chance, desc, occurrenceOnly, category));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown material '" + matName + "' in " + path + " – skipped.");
            }
        }
    }

    private int parseRarity(Object rawValue, int fallback) {
        if (rawValue == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(rawValue).trim());
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Invalid rarity '" + rawValue + "' in config.yml – using " + fallback + " instead.");
            return fallback;
        }
    }

    private String toConfigCategory(RareItemCategory category) {
        return switch (category) {
            case ITEM -> "item";
            case MOB -> "mob";
            case STRUCTURE -> "structure";
            case CUSTOM -> "individuell";
        };
    }

    public List<RareItem> getItems() {
        return items;
    }

    public List<RareItem> getItems(RareItemCategory filter) {
        if (filter == null) {
            return items;
        }
        return itemsByCategory.getOrDefault(filter, List.of());
    }

    public int count() {
        return items.size();
    }

    public Optional<RareItem> findById(String id) {
        return Optional.ofNullable(itemsById.get(id));
    }

    private int maxRarity(List<RareItem> source) {
        return source.stream().mapToInt(RareItem::rarity).max().orElse(0);
    }
}
