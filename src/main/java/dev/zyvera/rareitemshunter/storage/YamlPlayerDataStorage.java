package dev.zyvera.rareitemshunter.storage;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class YamlPlayerDataStorage implements PlayerDataStorage {

    private final RareItemsHunter plugin;
    private final File dataDir;
    private final ConcurrentMap<UUID, Object> fileLocks = new ConcurrentHashMap<>();

    public YamlPlayerDataStorage(RareItemsHunter plugin, File dataDir) {
        this.plugin = plugin;
        this.dataDir = dataDir;
    }

    @Override
    public void initialize() {
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            plugin.getLogger().warning("Could not create playerdata directory: " + dataDir.getAbsolutePath());
        }
    }

    @Override
    public void shutdown() {
        // nothing to close
    }

    @Override
    public Map<String, String> load(UUID uuid) {
        Map<String, String> data = new ConcurrentHashMap<>();
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

    @Override
    public Map<UUID, StoredPlayerData> loadAll() {
        Map<UUID, StoredPlayerData> all = new LinkedHashMap<>();
        File[] files = dataDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return all;
        }

        for (File file : files) {
            String raw = file.getName().substring(0, file.getName().length() - 4);
            try {
                UUID uuid = UUID.fromString(raw);
                synchronized (lock(uuid)) {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                    Map<String, String> found = new TreeMap<>();
                    for (String id : cfg.getStringList("found-items")) {
                        found.put(id, cfg.getString("timestamps." + id, ""));
                    }
                    String playerName = cfg.getString("player-name", "unknown");
                    all.put(uuid, new StoredPlayerData(playerName, found));
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping invalid playerdata file name: " + file.getName());
            }
        }

        return all;
    }

    @Override
    public void save(UUID uuid, String playerName, Map<String, String> snapshot) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("player-name", playerName == null ? "unknown" : playerName);
        cfg.set("found-items", new ArrayList<>(snapshot.keySet()));
        snapshot.forEach((id, ts) -> {
            if (ts != null && !ts.isBlank()) {
                cfg.set("timestamps." + id, ts);
            }
        });

        synchronized (lock(uuid)) {
            try {
                cfg.save(file(uuid));
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save YAML data for " + uuid + ": " + e.getMessage());
            }
        }
    }

    @Override
    public boolean reset(UUID uuid) {
        synchronized (lock(uuid)) {
            File file = file(uuid);
            return !file.exists() || file.delete();
        }
    }

    @Override
    public boolean hasStoredData(UUID uuid) {
        File file = file(uuid);
        if (!file.exists()) {
            return false;
        }
        synchronized (lock(uuid)) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            return !cfg.getStringList("found-items").isEmpty();
        }
    }

    @Override
    public String storageName() {
        return "YAML";
    }

    private Object lock(UUID uuid) {
        return fileLocks.computeIfAbsent(uuid, ignored -> new Object());
    }

    private File file(UUID uuid) {
        return new File(dataDir, uuid + ".yml");
    }
}
