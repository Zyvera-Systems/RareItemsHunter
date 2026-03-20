package dev.zyvera.rareitemshunter.storage;

import dev.zyvera.rareitemshunter.RareItemsHunter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class MySqlPlayerDataStorage implements PlayerDataStorage {

    private final RareItemsHunter plugin;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String table;

    public MySqlPlayerDataStorage(RareItemsHunter plugin) {
        this.plugin = plugin;
        String host = plugin.getConfig().getString("storage.mysql.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String database = plugin.getConfig().getString("storage.mysql.database", "rareitemshunter");
        this.username = plugin.getConfig().getString("storage.mysql.username", "root");
        this.password = plugin.getConfig().getString("storage.mysql.password", "change-me");
        String parameters = plugin.getConfig().getString("storage.mysql.parameters", "?useSSL=false&useUnicode=true&characterEncoding=utf8");
        this.table = sanitizeTableName(plugin.getConfig().getString("storage.mysql.table", "rih_playerdata"));

        if (parameters == null) {
            parameters = "";
        }
        if (!parameters.isBlank() && !parameters.startsWith("?")) {
            parameters = "?" + parameters;
        }
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + parameters;
    }

    @Override
    public void initialize() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` ("
                    + "`uuid` VARCHAR(36) NOT NULL,"
                    + "`player_name` VARCHAR(64) NULL,"
                    + "`item_id` VARCHAR(128) NOT NULL,"
                    + "`found_at` VARCHAR(32) NULL,"
                    + "PRIMARY KEY (`uuid`, `item_id`),"
                    + "INDEX `idx_" + table + "_player_name` (`player_name`)"
                    + ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    @Override
    public void shutdown() {
        // connections are short-lived per operation
    }

    @Override
    public Map<String, String> load(UUID uuid) {
        Map<String, String> found = new TreeMap<>();
        String sql = "SELECT item_id, found_at FROM `" + table + "` WHERE uuid = ?";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    found.put(rs.getString("item_id"), safe(rs.getString("found_at")));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not load MySQL player data for " + uuid + ": " + ex.getMessage());
        }

        return found;
    }

    @Override
    public Map<UUID, StoredPlayerData> loadAll() {
        Map<UUID, StoredPlayerData> all = new LinkedHashMap<>();
        String sql = "SELECT uuid, player_name, item_id, found_at FROM `" + table + "` ORDER BY uuid";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(rs.getString("uuid"));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Skipping invalid UUID row in MySQL storage: " + rs.getString("uuid"));
                    continue;
                }

                StoredPlayerData current = all.get(uuid);
                Map<String, String> found = current == null
                        ? new TreeMap<>()
                        : new TreeMap<>(current.foundItems());
                found.put(rs.getString("item_id"), safe(rs.getString("found_at")));

                String playerName = rs.getString("player_name");
                if ((playerName == null || playerName.isBlank()) && current != null) {
                    playerName = current.playerName();
                }
                all.put(uuid, new StoredPlayerData(safe(playerName, "unknown"), found));
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not load MySQL player data: " + ex.getMessage());
        }

        return all;
    }

    @Override
    public void save(UUID uuid, String playerName, Map<String, String> snapshot) {
        String deleteSql = "DELETE FROM `" + table + "` WHERE uuid = ?";
        String insertSql = "INSERT INTO `" + table + "` (uuid, player_name, item_id, found_at) VALUES (?, ?, ?, ?)";

        Connection connection = null;
        try {
            connection = connection();
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(deleteSql)) {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
            }

            if (!snapshot.isEmpty()) {
                try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                    for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                        insert.setString(1, uuid.toString());
                        insert.setString(2, safe(playerName, "unknown"));
                        insert.setString(3, entry.getKey());
                        insert.setString(4, safe(entry.getValue()));
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }

            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
            }
            plugin.getLogger().severe("Could not save MySQL player data for " + uuid + ": " + ex.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public boolean reset(UUID uuid) {
        String sql = "DELETE FROM `" + table + "` WHERE uuid = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not reset MySQL player data for " + uuid + ": " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasStoredData(UUID uuid) {
        String sql = "SELECT 1 FROM `" + table + "` WHERE uuid = ? LIMIT 1";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not check MySQL player data for " + uuid + ": " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String storageName() {
        return "MYSQL";
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private String sanitizeTableName(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_]+")) {
            plugin.getLogger().warning("Invalid MySQL table name configured, falling back to rih_playerdata.");
            return "rih_playerdata";
        }
        return raw;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
