package dev.zyvera.rareitemshunter.lang;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LangManager {

    private final RareItemsHunter plugin;
    private YamlConfiguration lang;
    private YamlConfiguration defaults;
    private File currentFile;

    public LangManager(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String language = plugin.getConfig().getString("language", "de");
        String fileName = "lang/" + language + ".yml";
        currentFile = new File(plugin.getDataFolder(), fileName);

        // Only create the file if it doesn't exist yet – never overwrite user edits
        plugin.saveResource(fileName, false);
        lang = YamlConfiguration.loadConfiguration(currentFile);

        InputStream stream = plugin.getResource(fileName);
        defaults = null;
        if (stream != null) {
            defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            mergeMissingDefaults(lang, defaults, "");
            try {
                lang.save(currentFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not update language file '" + fileName + "': " + e.getMessage());
            }
        }
    }

    public String get(String key) {
        return color(resolve(key));
    }

    // Translates color codes AFTER placeholder substitution so item names with & codes render correctly
    public String get(String key, Map<String, String> placeholders) {
        String raw = resolve(key);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        }
        return color(raw);
    }

    public String prefix() {
        return get("prefix");
    }

    private String resolve(String key) {
        String value = lang != null ? lang.getString(key) : null;
        if (value == null && defaults != null) {
            value = defaults.getString(key);
        }
        return value != null ? value : "&cMissing: " + key;
    }

    private void mergeMissingDefaults(ConfigurationSection target, ConfigurationSection source, String path) {
        for (String key : source.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            Object sourceValue = source.get(key);
            if (sourceValue instanceof ConfigurationSection sourceSection) {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                if (targetSection == null) {
                    targetSection = target.createSection(key);
                }
                mergeMissingDefaults(targetSection, sourceSection, fullPath);
                continue;
            }

            if (!lang.contains(fullPath, true)) {
                lang.set(fullPath, sourceValue);
            }
        }
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
