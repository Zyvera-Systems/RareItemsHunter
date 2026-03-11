package dev.zyvera.rareitemshunter.manager;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PlayerDataManager {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final RareItemsHunter        plugin;
    private final File                   dataDir;

    private final Map<UUID, Map<String, String>> cache = new HashMap<>();

    public PlayerDataManager(RareItemsHunter plugin) {
        this.plugin  = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!dataDir.exists()) dataDir.mkdirs();
    }

    public void load(Player player) {
        UUID uuid = player.getUniqueId();
        if (cache.containsKey(uuid)) return;

        Map<String, String> data = new LinkedHashMap<>();
        File file = file(uuid);

        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            List<String> legacy = cfg.getStringList("found-items");

            for (String id : legacy) {
                data.put(id, cfg.getString("timestamps." + id, ""));
            }
        }

        cache.put(uuid, data);
    }

    public void save(Player player) {
        Map<String, String> data = cache.getOrDefault(player.getUniqueId(), Map.of());
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("found-items", new ArrayList<>(data.keySet()));
        data.forEach((id, ts) -> { if (!ts.isEmpty()) cfg.set("timestamps." + id, ts); });

        try { cfg.save(file(player.getUniqueId())); }
        catch (IOException e) { plugin.getLogger().severe("Could not save data for " + player.getName()); }
    }

    public void unload(UUID uuid) { cache.remove(uuid); }

    public boolean hasFound(Player player, String itemId) {
        ensure(player);
        return cache.getOrDefault(player.getUniqueId(), Map.of()).containsKey(itemId);
    }

    public void markFound(Player player, String itemId) {
        ensure(player);
        cache.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashMap<>())
             .put(itemId, LocalDateTime.now().format(FMT));
        save(player);
    }

    public String getFoundTimestamp(Player player, String itemId) {
        ensure(player);
        return cache.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(itemId, "");
    }

    public Set<String> getFoundItems(Player player) {
        ensure(player);
        return Collections.unmodifiableSet(cache.getOrDefault(player.getUniqueId(), Map.of()).keySet());
    }

    public int countFound(Player player) { return getFoundItems(player).size(); }

    public void reset(Player player) {
        cache.put(player.getUniqueId(), new LinkedHashMap<>());
        save(player);
    }

    public boolean resetByUUID(UUID uuid) {
        cache.remove(uuid);
        File f = file(uuid);
        return !f.exists() || f.delete();
    }

    private void ensure(Player player) { if (!cache.containsKey(player.getUniqueId())) load(player); }
    private File file(UUID uuid)       { return new File(dataDir, uuid + ".yml"); }
}
