package dev.zyvera.rareitemshunter.manager;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerDataManager {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final RareItemsHunter        plugin;
    private final File                   dataDir;

    // uuid -> itemId -> timestamp (or empty string for legacy entries)
    private final ConcurrentMap<UUID, ConcurrentMap<String, String>> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Object> fileLocks = new ConcurrentHashMap<>();

    public PlayerDataManager(RareItemsHunter plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public void load(Player player) {
        UUID uuid = player.getUniqueId();
        cache.computeIfAbsent(uuid, ignored -> loadFromDisk(uuid));
    }

    public void save(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, String> snapshot = Map.copyOf(cache.getOrDefault(uuid, new ConcurrentHashMap<>()));
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("found-items", new ArrayList<>(snapshot.keySet()));
        snapshot.forEach((id, ts) -> {
            if (!ts.isEmpty()) {
                cfg.set("timestamps." + id, ts);
            }
        });

        synchronized (lock(uuid)) {
            try {
                cfg.save(file(uuid));
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data for " + player.getName());
            }
        }
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public boolean hasFound(Player player, String itemId) {
        ensure(player);
        return cache.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>()).containsKey(itemId);
    }

    public void markFound(Player player, String itemId) {
        ensure(player);
        cache.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(itemId, LocalDateTime.now().format(FMT));
        save(player);
    }

    public String getFoundTimestamp(Player player, String itemId) {
        ensure(player);
        return cache.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>()).getOrDefault(itemId, "");
    }

    public Set<String> getFoundItems(Player player) {
        ensure(player);
        return Collections.unmodifiableSet(new HashSet<>(
                cache.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>()).keySet()
        ));
    }

    public int countFound(Player player) {
        ensure(player);
        return cache.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>()).size();
    }

    public void reset(Player player) {
        cache.put(player.getUniqueId(), new ConcurrentHashMap<>());
        save(player);
    }

    public boolean resetByUUID(UUID uuid) {
        cache.remove(uuid);
        synchronized (lock(uuid)) {
            File file = file(uuid);
            return !file.exists() || file.delete();
        }
    }

    private void ensure(Player player) {
        if (!cache.containsKey(player.getUniqueId())) {
            load(player);
        }
    }

    private ConcurrentMap<String, String> loadFromDisk(UUID uuid) {
        ConcurrentMap<String, String> data = new ConcurrentHashMap<>();
        File file = file(uuid);
        if (!file.exists()) {
            return data;
        }

        synchronized (lock(uuid)) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String id : cfg.getStringList("found-items")) {
                data.put(id, cfg.getString("timestamps." + id, ""));
            }
        }
        return data;
    }

    private Object lock(UUID uuid) {
        return fileLocks.computeIfAbsent(uuid, ignored -> new Object());
    }

    private File file(UUID uuid) {
        return new File(dataDir, uuid + ".yml");
    }
}
