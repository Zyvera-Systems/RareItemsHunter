package dev.zyvera.rareitemshunter.storage;

import java.util.Map;
import java.util.UUID;

public interface PlayerDataStorage {

    void initialize() throws Exception;

    void shutdown();

    Map<String, String> load(UUID uuid);

    Map<UUID, StoredPlayerData> loadAll();

    void save(UUID uuid, String playerName, Map<String, String> snapshot);

    boolean reset(UUID uuid);

    boolean hasStoredData(UUID uuid);

    String storageName();
}
