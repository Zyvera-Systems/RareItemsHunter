package dev.zyvera.rareitemshunter.lang;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LangManager {

    private final RareItemsHunter plugin;
    private YamlConfiguration lang;

    public LangManager(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String language = plugin.getConfig().getString("language", "de");
        String fileName = "lang/" + language + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);

        plugin.saveResource(fileName, true);
        lang = YamlConfiguration.loadConfiguration(file);

        InputStream stream = plugin.getResource(fileName);
        if (stream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            lang.setDefaults(defaults);
        }
    }

    public String get(String key) {
        return color(lang.getString(key, "&cMissing: " + key));
    }

    public String get(String key, Map<String, String> placeholders) {
        String raw = lang.getString(key, "&cMissing: " + key);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        }
        return color(raw);
    }

    public String prefix() {
        return get("prefix");
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
