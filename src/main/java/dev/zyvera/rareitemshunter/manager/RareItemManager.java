package dev.zyvera.rareitemshunter.manager;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.model.RareItem;
import org.bukkit.Material;

import java.util.*;

public class RareItemManager {

    private final RareItemsHunter plugin;
    private final List<RareItem>  items = new ArrayList<>();

    private static final Object[][] DEFAULTS = {
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
