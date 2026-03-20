package dev.zyvera.rareitemshunter.command;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.model.RareItem;
import dev.zyvera.rareitemshunter.model.RareItemCategory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RiCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final RareItemsHunter plugin;

    public RiCommand(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var lang = plugin.getLang();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                send(sender, lang.get("messages.player-only", Map.of("prefix", lang.prefix())));
                return true;
            }
            plugin.setGuiFilter(player.getUniqueId(), null);
            plugin.getGui().open(player, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reset" -> handleResetCommand(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("rih.reload")) {
                    send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getConfig().options().copyDefaults(true);
                plugin.saveConfig();
                plugin.getLang().load();
                plugin.getRareItemManager().reload();
                plugin.getPlayerDataManager().reloadStorage();
                send(sender, lang.get("messages.reloaded", Map.of(
                        "prefix", lang.prefix(),
                        "count", String.valueOf(plugin.getRareItemManager().count()),
                        "storage", plugin.getPlayerDataManager().currentStorageName())));
            }
            case "give" -> {
                if (!sender.hasPermission("rih.give")) {
                    send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                    return true;
                }
                if (args.length < 3) {
                    send(sender, lang.get("messages.give-usage", Map.of("prefix", lang.prefix())));
                    return true;
                }
                handleGive(sender, args[1], args[2]);
            }
            case "rewardmode" -> {
                if (!sender.hasPermission("rih.rewardmode")) {
                    send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                    return true;
                }
                if (args.length < 2) {
                    send(sender, lang.get("messages.rewardmode-usage", Map.of("prefix", lang.prefix())));
                    return true;
                }
                handleRewardMode(sender, args[1]);
            }
            case "stats" -> handleStats(sender, args);
            default -> send(sender, lang.get("messages.unknown-command", Map.of("prefix", lang.prefix())));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        var lang = plugin.getLang();
        send(sender, lang.get("messages.help-header"));
        send(sender, lang.get("messages.help-line1"));
        send(sender, lang.get("messages.help-line2"));
        send(sender, lang.get("messages.help-line3"));
        send(sender, lang.get("messages.help-line4"));
        send(sender, lang.get("messages.help-line5"));
        send(sender, lang.get("messages.help-line6"));
        send(sender, lang.get("messages.help-line7"));
        send(sender, lang.get("messages.help-line8"));
        send(sender, lang.get("messages.help-footer"));
    }

    private void handleResetCommand(CommandSender sender, String[] args) {
        var lang = plugin.getLang();

        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                send(sender, lang.get("messages.reset-usage", Map.of("prefix", lang.prefix())));
                return;
            }
            if (!sender.hasPermission("rih.reset.self")) {
                send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                return;
            }
            plugin.getPlayerDataManager().reset(player);
            send(sender, lang.get("messages.reset-self", Map.of("prefix", lang.prefix())));
            return;
        }

        if (!sender.hasPermission("rih.reset.others")) {
            send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
            return;
        }

        handleReset(sender, args[1]);
    }

    private void handleReset(CommandSender sender, String targetName) {
        var lang = plugin.getLang();
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            plugin.getPlayerDataManager().reset(online);
            send(sender, lang.get("messages.reset-online", Map.of("prefix", lang.prefix(), "player", online.getName())));
            plugin.getSchedulerBridge().runForEntity(online, () ->
                    send(online, lang.get("messages.reset-notify", Map.of("prefix", lang.prefix()))));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = offline.getUniqueId();
        if (uuid != null) {
            plugin.getPlayerDataManager().resetByUUID(uuid);
            send(sender, lang.get("messages.reset-offline", Map.of("prefix", lang.prefix(), "player", targetName)));
        } else {
            send(sender, lang.get("messages.player-not-found", Map.of("prefix", lang.prefix(), "player", targetName)));
        }
    }

    private void handleGive(CommandSender sender, String targetName, String itemId) {
        var lang = plugin.getLang();
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            send(sender, lang.get("messages.player-not-found", Map.of("prefix", lang.prefix(), "player", targetName)));
            return;
        }

        Optional<RareItem> opt = plugin.getRareItemManager().findById(itemId);
        if (opt.isEmpty()) {
            send(sender, lang.get("messages.give-unknown-id", Map.of("prefix", lang.prefix(), "id", itemId)));
            return;
        }

        RareItem ri = opt.get();
        if (plugin.getPlayerDataManager().hasFound(target, ri.id())) {
            send(sender, lang.get("messages.give-already", Map.of(
                    "prefix", lang.prefix(),
                    "player", target.getName(),
                    "item", ri.displayName())));
            return;
        }

        plugin.getPlayerDataManager().markFound(target, ri.id());
        send(sender, lang.get("messages.give-done", Map.of(
                "prefix", lang.prefix(),
                "player", target.getName(),
                "item", ri.displayName())));
        plugin.getSchedulerBridge().runForEntity(target, () ->
                send(target, lang.get("messages.give-notify", Map.of("prefix", lang.prefix(), "item", ri.displayName()))));
    }

    private void handleRewardMode(CommandSender sender, String rawMode) {
        var lang = plugin.getLang();
        String normalized = normalizeRewardMode(rawMode);
        if (normalized == null) {
            send(sender, lang.get("messages.rewardmode-usage", Map.of("prefix", lang.prefix())));
            return;
        }

        plugin.getConfig().set("rewards.mode", normalized);
        plugin.saveConfig();

        String modeLabel = normalized.equals("GLOBAL")
                ? lang.get("messages.rewardmode-global")
                : lang.get("messages.rewardmode-peritem");
        send(sender, lang.get("messages.rewardmode-set", Map.of(
                "prefix", lang.prefix(),
                "mode", modeLabel)));
    }

    private void handleStats(CommandSender sender, String[] args) {
        var lang = plugin.getLang();

        UUID uuid;
        String displayName;

        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                send(sender, lang.get("messages.stats-usage", Map.of("prefix", lang.prefix())));
                return;
            }
            if (!sender.hasPermission("rih.stats.self")) {
                send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                return;
            }
            uuid = player.getUniqueId();
            displayName = player.getName();
        } else {
            if (!sender.hasPermission("rih.stats.others")) {
                send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                return;
            }
            Player online = Bukkit.getPlayerExact(args[1]);
            if (online != null) {
                uuid = online.getUniqueId();
                displayName = online.getName();
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                uuid = offline.getUniqueId();
                displayName = offline.getName() != null ? offline.getName() : args[1];
            }
        }

        Map<String, String> rawFoundData = plugin.getPlayerDataManager().getFoundItemsWithTimestamps(uuid);
        Set<String> trackedIds = plugin.getRareItemManager().getItems().stream()
                .map(RareItem::id)
                .collect(Collectors.toSet());
        Map<String, String> foundData = rawFoundData.entrySet().stream()
                .filter(entry -> trackedIds.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int total = plugin.getRareItemManager().count();
        int found = foundData.size();
        String pct = total <= 0 ? "0.0" : String.format(Locale.US, "%.1f", (found * 100.0D) / total);

        send(sender, lang.get("messages.stats-header", Map.of(
                "prefix", lang.prefix(),
                "player", displayName)));
        send(sender, lang.get("messages.stats-summary", Map.of(
                "found", String.valueOf(found),
                "total", String.valueOf(total),
                "percent", pct)));

        for (RareItemCategory category : RareItemCategory.values()) {
            List<RareItem> items = plugin.getRareItemManager().getItems(category);
            int foundInCategory = (int) items.stream()
                    .filter(item -> foundData.containsKey(item.id()))
                    .count();

            send(sender, lang.get("messages.stats-category", Map.of(
                    "category", plugin.getLang().get(categoryKey(category)),
                    "found", String.valueOf(foundInCategory),
                    "total", String.valueOf(items.size()))));
        }

        Optional<RareItem> rarest = plugin.getRareItemManager().getItems().stream()
                .filter(item -> foundData.containsKey(item.id()))
                .min(Comparator.comparingInt(RareItem::rarity));

        if (rarest.isPresent()) {
            send(sender, lang.get("messages.stats-rarest", Map.of(
                    "item", rarest.get().displayName(),
                    "rank", String.valueOf(rarest.get().rarity()))));
        } else {
            send(sender, lang.get("messages.stats-rarest-none"));
        }

        Optional<FoundEntry> latestFound = plugin.getRareItemManager().getItems().stream()
                .filter(item -> foundData.containsKey(item.id()))
                .map(item -> new FoundEntry(item, foundData.getOrDefault(item.id(), "")))
                .filter(entry -> entry.parsedTime() != null)
                .max(Comparator.comparing(FoundEntry::parsedTime));

        if (latestFound.isPresent()) {
            send(sender, lang.get("messages.stats-last", Map.of(
                    "item", latestFound.get().item().displayName(),
                    "time", latestFound.get().timestamp())));
        } else {
            send(sender, lang.get("messages.stats-last-none"));
        }
    }

    private String normalizeRewardMode(String rawMode) {
        if (rawMode == null) {
            return null;
        }
        return switch (rawMode.trim().toLowerCase(Locale.ROOT)) {
            case "global", "all", "alle" -> "GLOBAL";
            case "peritem", "item", "proitem", "einzeln" -> "PER_ITEM";
            default -> null;
        };
    }

    private String categoryKey(RareItemCategory category) {
        return switch (category) {
            case ITEM -> "gui.filter-item";
            case MOB -> "gui.filter-mob";
            case STRUCTURE -> "gui.filter-structure";
            case CUSTOM -> "gui.filter-custom";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("help", "reset", "reload", "give", "rewardmode", "stats"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("stats"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rewardmode")) {
            return filter(List.of("peritem", "global"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return plugin.getRareItemManager().getItems().stream()
                    .map(RareItem::id)
                    .filter(id -> id.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg == null ? "" : msg));
    }

    private List<String> filter(List<String> list, String prefix) {
        String loweredPrefix = prefix.toLowerCase(Locale.ROOT);
        return list.stream()
                .filter(s -> s.startsWith(loweredPrefix))
                .collect(Collectors.toList());
    }

    private record FoundEntry(RareItem item, String timestamp) {
        private LocalDateTime parsedTime() {
            if (timestamp == null || timestamp.isBlank()) {
                return null;
            }
            try {
                return LocalDateTime.parse(timestamp, FMT);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
