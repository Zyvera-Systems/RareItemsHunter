package dev.zyvera.rareitemshunter.command;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.model.RareItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class RiCommand implements CommandExecutor, TabCompleter {

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
            if (!player.hasPermission("rih.use")) {
                send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
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
            case "reset" -> {
                if (!sender.hasPermission("rih.admin")) {
                    send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                    return true;
                }
                if (args.length < 2) {
                    send(sender, lang.get("messages.unknown-command", Map.of("prefix", lang.prefix())));
                    return true;
                }
                handleReset(sender, args[1]);
            }
            case "reload" -> {
                if (!sender.hasPermission("rih.admin")) {
                    send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getConfig().options().copyDefaults(true);
                plugin.saveConfig();
                plugin.getLang().load();
                plugin.getRareItemManager().reload();
                send(sender, lang.get("messages.reloaded", Map.of(
                        "prefix", lang.prefix(),
                        "count", String.valueOf(plugin.getRareItemManager().count()))));
            }
            case "give" -> {
                if (!sender.hasPermission("rih.admin")) {
                    send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                    return true;
                }
                if (args.length < 3) {
                    send(sender, lang.get("messages.unknown-command", Map.of("prefix", lang.prefix())));
                    return true;
                }
                handleGive(sender, args[1], args[2]);
            }
            case "rewardmode" -> {
                if (!sender.hasPermission("rih.admin")) {
                    send(sender, lang.get("messages.no-permission", Map.of("prefix", lang.prefix())));
                    return true;
                }
                if (args.length < 2) {
                    send(sender, lang.get("messages.rewardmode-usage", Map.of("prefix", lang.prefix())));
                    return true;
                }
                handleRewardMode(sender, args[1]);
            }
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
        send(sender, lang.get("messages.help-header"));
        send(sender, lang.get("messages.help-footer"));
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
        UUID uuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("help", "reset", "reload", "give", "rewardmode"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("give"))) {
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
}
