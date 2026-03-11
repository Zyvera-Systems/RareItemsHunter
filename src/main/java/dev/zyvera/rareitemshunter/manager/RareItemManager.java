package dev.zyvera.rareitemshunter.manager;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.model.RareItem;
import org.bukkit.Material;

import java.util.*;

public class RareItemManager {

    private final RareItemsHunter plugin;
    private final List<RareItem>  items = new ArrayList<>();

    private static final Object[][] DEFAULTS = {
            { "zombie_villager_chicken_diamond", Material.IRON_SWORD,
                    "&c&lBaby-Zombie-Dorfbewohner auf Huhn (full Dia)", 1,
                    "~0.000000000000000000000000000000034 %",
                    "Seltenster Mob der je gesehen wurde", true },

            { "endportal_12_eye", Material.ENDER_EYE,
                    "&5&l12-Augen-Endportal", 2, "~0.000000000001 %",
                    "Alle 12 Rahmen natuerlich befuellt", true },

            { "dragon_egg", Material.DRAGON_EGG,
                    "&5&lDrachen", 3, "1x pro Welt",
                    "Nur nach erstem Enderdrachen-Sieg", false },

            { "baby_pink_sheep", Material.PINK_DYE,
                    "&d&lBaby-Pinkes Schaf", 4, "~0.0082 %",
                    "0.0164 % rosa Spawn x 50 % Baby", true },

            { "blue_axolotl", Material.AXOLOTL_BUCKET,
                    "&b&lBlaues Axolotl", 5, "~0.083 %",
                    "1 von 1200 gespawnten Axolotln", false },

            { "trident", Material.TRIDENT,
                    "&9&lDreizack", 6, "~0.53 %",
                    "Drop bei Ertrunkenen", false },

            { "wither_skeleton_skull", Material.WITHER_SKELETON_SKULL,
                    "&0&lWither-Skelett-Schaedel", 7, "~2.5 %",
                    "Benoetigt 3 Stueck fuer Wither-Beschwoerung", false },

            { "music_disc_creator_music_box", Material.MUSIC_DISC_CREATOR_MUSIC_BOX,
                    "&d&lMusikscheibe \"Creator (Music Box)\"", 8, "~1.4 %",
                    "Dekorationstopf in Pruefungskammer", false },

            { "music_disc_otherside", Material.MUSIC_DISC_OTHERSIDE,
                    "&b&lMusikscheibe \"otherside\"", 9, "~3.1 %",
                    "Verlies- und Festungstruhen", false },

            { "music_disc_creator", Material.MUSIC_DISC_CREATOR,
                    "&e&lMusikscheibe \"Creator\"", 10, "~3.0 %",
                    "Unheilsvoller Tresor", false },

            { "music_disc_precipice", Material.MUSIC_DISC_PRECIPICE,
                    "&6&lMusikscheibe \"Precipice\"", 11, "~3.6 %",
                    "Pruefungskammer-Tresor", false },

            { "music_disc_pigstep", Material.MUSIC_DISC_PIGSTEP,
                    "&c&lMusikscheibe \"Pigstep\"", 12, "~5.6 %",
                    "Bastion Remnant - nicht per Creeper", false },

            { "music_disc_5", Material.MUSIC_DISC_5,
                    "&3&lMusikscheibe \"5\"", 13, "~0.8 % pro Scherbe",
                    "9 Fragmente aus Alter-Stadt-Truhen", false },

            { "music_disc_relic", Material.MUSIC_DISC_RELIC,
                    "&6&lMusikscheibe \"Relic\"", 14, "~7.7 %",
                    "Bastion Remnant", false },

            { "music_disc_11", Material.MUSIC_DISC_11,
                    "&c&lMusikscheibe \"11\"", 15, "~0.5 %",
                    "Creeper muss von Skelett getoetet werden", false },

            { "enchanted_golden_apple", Material.ENCHANTED_GOLDEN_APPLE,
                    "&6&lVerzauberter Goldapfel", 16, "~2.5 %",
                    "Festung, Bastion, Ruiniertes Portal", false },

            { "heavy_core", Material.HEAVY_CORE,
                    "&8&lSchwerer Kern", 17, "~7.5 %",
                    "Unheilsvoller Tresor (Pruefungskammer)", false },

            { "heart_of_the_sea", Material.HEART_OF_THE_SEA,
                    "&3&lHerz des Meeres", 18, "1x pro Schatz-Truhe",
                    "Versteckter-Schatz via Schatzkarte", false },

            { "echo_shard", Material.ECHO_SHARD,
                    "&3&lEcho-Scherbe", 19, "~29.8 %",
                    "Alte-Stadt-Truhen", false },

            { "sniffer_egg", Material.SNIFFER_EGG,
                    "&a&lSchnueffler-Ei", 20, "~6.7 %",
                    "Ausgraben in warmen Meeresruinen", false },

            { "ominous_trial_key", Material.OMINOUS_TRIAL_KEY,
                    "&4&lUnheilsvoller Pruefungsschluessel", 21, "~30 %",
                    "Unheilsvoller Spawner (Bad-Omen benoetigt)", false },

            { "breeze_rod", Material.BREEZE_ROD,
                    "&b&lBreeze-Stab", 22, "~33 % (1-2 Drops)",
                    "Nur in Pruefungskammern", false },

            { "ancient_debris", Material.ANCIENT_DEBRIS,
                    "&8&lAntiker Schrott", 23, "~1.7 pro Chunk",
                    "Nether y=15 optimal", false },

            { "nether_star", Material.NETHER_STAR,
                    "&f&lNetherstern", 24, "100 % beim Wither",
                    "Wither muss beschworen werden", false },

            { "elytra", Material.ELYTRA,
                    "&6&lElytra", 25, "1x pro Endstadtschiff",
                    "Endstadtschiff nach Enderdrachen-Sieg", false },

            { "rabbit_foot", Material.RABBIT_FOOT,
                    "&e&lHasenpfote", 26, "~10 %",
                    "Drop bei Hasen (mit Pluenderung hoeher)", false },

            { "baby_brown_sheep", Material.BROWN_DYE,
                    "&6&lBaby-Braunes Schaf", 27, "~0.15 %",
                    "3 % braun x 50 % Baby", true },

            { "pink_sheep", Material.PINK_WOOL,
                    "&d&lPinkes Schaf", 28, "~0.16 %",
                    "Natuerlicher Spawn", false },

            { "brown_sheep", Material.BROWN_WOOL,
                    "&6&lBraunes Schaf", 29, "~2.85 %",
                    "Natuerlicher Spawn", false },

            { "spider_jockey", Material.COBWEB,
                    "&8&lSpinnen-Jockey", 30, "~1 %",
                    "Skelett reitet auf Spinne", true },

            { "chicken_jockey", Material.FEATHER,
                    "&a&lHuhn-Jockey", 31, "~0.25 %",
                    "Baby-Zombie reitet auf Huhn", true },

            { "charged_creeper", Material.GUNPOWDER,
                    "&b&lGeladener Creeper", 32, "~0.001 %",
                    "Blitz trifft Creeper in 4-Block-Naehe", true },

            { "cactus_5high", Material.CACTUS,
                    "&2&lKaktus 5 Bloecke hoch", 33, "~0.003 %",
                    "Benoetigt 2 zusaetzliche Wachstumszyklen", false },
    };

    public RareItemManager(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        items.clear();

        for (Object[] row : DEFAULTS) {
            Material mat = (Material) row[1];
            if (mat == null) continue;
            items.add(new RareItem(
                    (String) row[0], mat, (String) row[2],
                    (int)    row[3], (String) row[4],
                    (String) row[5], (boolean) row[6]
            ));
        }

        List<?> customList = plugin.getConfig().getList("custom-items");
        if (customList != null) {
            int fallback = maxRarity() + 1;
            for (Object obj : customList) {
                if (!(obj instanceof Map<?, ?> raw)) continue;

                @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) raw;
                String id = "custom_" + map.getOrDefault("id", "item_" + fallback);
                String matName = String.valueOf(map.getOrDefault("material", "STONE")).toUpperCase();
                String name = String.valueOf(map.getOrDefault("name", "&7Unknown Item"));
                String chance = String.valueOf(map.getOrDefault("chance", "?"));
                String desc = String.valueOf(map.getOrDefault("description", ""));

                boolean  occOnly = Boolean.parseBoolean(String.valueOf(map.getOrDefault("occurrence-only", "false")));
                int      rarity  = map.containsKey("rarity")
                        ? Integer.parseInt(String.valueOf(map.get("rarity"))) : fallback++;
                try {
                    items.add(new RareItem(id, Material.valueOf(matName), name, rarity, chance, desc, occOnly));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown material '" + matName + "' in custom-items – skipped.");
                }
            }
        }

        items.sort(Comparator.comparingInt(RareItem::rarity));
        plugin.getLogger().info("Loaded " + items.size() + " rare items.");
    }

    public List<RareItem> getItems() { return Collections.unmodifiableList(items); }
    public int count() { return items.size(); }
    public Optional<RareItem> findById(String id) {return items.stream().filter(r -> r.id().equals(id)).findFirst();}

    private int maxRarity() {
        return items.stream().mapToInt(RareItem::rarity).max().orElse(0);
    }
}
