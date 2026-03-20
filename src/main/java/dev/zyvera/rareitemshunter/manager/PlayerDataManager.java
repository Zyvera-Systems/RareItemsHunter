package dev.zyvera.rareitemshunter.manager;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.storage.MySqlPlayerDataStorage;
import dev.zyvera.rareitemshunter.storage.PlayerDataStorage;
import dev.zyvera.rareitemshunter.storage.StoredPlayerData;
import dev.zyvera.rareitemshunter.storage.YamlPlayerDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerDataManager {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final RareItemsHunter plugin;
    private final File dataDir;

    // uuid -> itemId -> timestamp (or empty string for legacy entries)
    private final ConcurrentMap<UUID, ConcurrentMap<String, String>> cache = new ConcurrentHashMap<>();
    private volatile PlayerDataStorage storage;

    public PlayerDataManager(RareItemsHunter plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        reloadStorage(false);
    }

    public synchronized void reloadStorage() {
        reloadStorage(true);
    }

    private synchronized void reloadStorage(boolean migrateExistingData) {
        Map<UUID, StoredPlayerData> migrationData = new LinkedHashMap<>();
        if (migrateExistingData && storage != null) {
            migrationData.putAll(storage.loadAll());
            for (Map.Entry<UUID, ConcurrentMap<String, String>> entry : cache.entrySet()) {
                migrationData.put(entry.getKey(), new StoredPlayerData(resolvePlayerName(entry.getKey()), snapshot(entry.getKey())));
            }
            storage.shutdown();
        }

        this.storage = createStorageWithFallback();

        if (!migrationData.isEmpty()) {
            for (Map.Entry<UUID, StoredPlayerData> entry : migrationData.entrySet()) {
                storage.save(entry.getKey(), entry.getValue().playerName(), entry.getValue().foundItems());
            }
        }

        plugin.getLogger().info("Player data storage active: " + storage.storageName());
    }

    public void load(Player player) {
        UUID uuid = player.getUniqueId();
        cache.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>(storage.load(uuid)));
    }

    public void save(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, String> snapshot = snapshot(uuid);
        storage.save(uuid, player.getName(), snapshot);
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

    public Map<String, String> getFoundItemsWithTimestamps(Player player) {
        ensure(player);
        return getFoundItemsWithTimestamps(player.getUniqueId());
    }

    public Map<String, String> getFoundItemsWithTimestamps(UUID uuid) {
        return Collections.unmodifiableMap(snapshot(uuid));
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
        return storage.reset(uuid);
    }

    public boolean hasStoredData(UUID uuid) {
        Map<String, String> data = cache.get(uuid);
        if (data != null && !data.isEmpty()) {
            return true;
        }
        return storage.hasStoredData(uuid);
    }

    public String currentStorageName() {
        return storage == null ? "UNKNOWN" : storage.storageName();
    }

    private PlayerDataStorage createStorageWithFallback() {
        String rawType = plugin.getConfig().getString("storage.type", "YAML");
        String normalized = rawType == null ? "YAML" : rawType.trim().toUpperCase();

        if (normalized.equals("MYSQL") || normalized.equals("MARIADB")) {
            try {
                PlayerDataStorage mysql = new MySqlPlayerDataStorage(plugin);
                mysql.initialize();
                return mysql;
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not initialize MySQL storage, falling back to YAML: " + ex.getMessage());
            }
        }

        PlayerDataStorage yaml = new YamlPlayerDataStorage(plugin, dataDir);
        try {
            yaml.initialize();
            return yaml;
        } catch (Exception ex) {
            plugin.getLogger().severe("Could not initialize fallback YAML storage: " + ex.getMessage());
            throw new IllegalStateException("No usable player data storage available", ex);
        }
    }

    private void ensure(Player player) {
        if (!cache.containsKey(player.getUniqueId())) {
            load(player);
        }
    }

    private Map<String, String> snapshot(UUID uuid) {
        Map<String, String> source = cache.containsKey(uuid)
                ? cache.getOrDefault(uuid, new ConcurrentHashMap<>())
                : storage.load(uuid);

        Map<String, String> sorted = new TreeMap<>();
        sorted.putAll(source);
        return sorted;
    }

    private String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : "unknown";
    }
}
