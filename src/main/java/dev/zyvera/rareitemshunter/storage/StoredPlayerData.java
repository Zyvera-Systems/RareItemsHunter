package dev.zyvera.rareitemshunter.storage;

import java.util.Map;

public record StoredPlayerData(String playerName, Map<String, String> foundItems) {
}
