package dev.zyvera.rareitemshunter.gui;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.model.RareItem;
import dev.zyvera.rareitemshunter.model.RareItemCategory;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RareItemsGUI {

    public static final int ITEMS_PER_PAGE = 18;
    public static final int PANE_TOP_START = 0;
    public static final int ITEM_TOP_START = 9;
    public static final int ITEM_BOT_START = 18;
    public static final int PANE_BOT_START = 27;
    public static final int PROGRESS_START = 36;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_FILTER = 49;
    public static final int SLOT_NEXT = 53;
    private static final int COLS = 9;

    private static final Material PANE_UNFOUND = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material PANE_FOUND = Material.LIME_STAINED_GLASS_PANE;
    private static final Material PROGRESS_FILLED = Material.GREEN_STAINED_GLASS_PANE;
    private static final Material PROGRESS_COMPLETE = Material.ORANGE_STAINED_GLASS_PANE;
    private static final Material PROGRESS_EMPTY = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    private static final Material NAV_FILLER = Material.GRAY_STAINED_GLASS_PANE;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final RareItemCategory[] FILTER_ORDER = {
            null,
            RareItemCategory.ITEM,
            RareItemCategory.MOB,
            RareItemCategory.STRUCTURE,
            RareItemCategory.CUSTOM
    };

    private final RareItemsHunter plugin;
    private final Random rng = new Random();

    public RareItemsGUI(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        RareItemCategory filter = plugin.getGuiFilter(player.getUniqueId());
        List<RareItem> visibleItems = plugin.getRareItemManager().getItems(filter);
        int visibleTotal = visibleItems.size();
        int globalTotal = plugin.getRareItemManager().count();
        int maxPage = Math.max(0, (visibleTotal + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE - 1);
        page = Math.max(0, Math.min(page, maxPage));

        plugin.lockNavigation(player.getUniqueId());
        plugin.setGuiPage(player.getUniqueId(), page);

        String rawTitle = plugin.getConfig().getString("gui-title", "&8» &6&lSeltene Items &8«");
        String filterSuffix = filter == null
                ? ""
                : ChatColor.DARK_GRAY + " • " + color(plugin.getLang().get(filterKey(filter)));
        String title = color(rawTitle) + filterSuffix + ChatColor.GRAY + " [" + (page + 1) + "/" + (maxPage + 1) + "]";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        Set<String> found = plugin.getPlayerDataManager().getFoundItems(player);
        int foundCount = 0;
        for (RareItem item : visibleItems) {
            if (found.contains(item.id())) {
                foundCount++;
            }
        }
        boolean full = visibleTotal > 0 && foundCount >= visibleTotal;

        for (int col = 0; col < COLS; col++) {
            fillPair(inv, visibleItems, visibleTotal, found, player, page * ITEMS_PER_PAGE + col,
                    PANE_TOP_START + col, ITEM_TOP_START + col, globalTotal);
            fillPair(inv, visibleItems, visibleTotal, found, player, page * ITEMS_PER_PAGE + COLS + col,
                    PANE_BOT_START + col, ITEM_BOT_START + col, globalTotal);
        }

        double pct = visibleTotal == 0 ? 0 : (double) foundCount / visibleTotal;
        int filled = (int) Math.round(pct * COLS);
        String pctStr = String.format(Locale.US, "%.1f%%", pct * 100);
        Material barMat = full ? PROGRESS_COMPLETE : PROGRESS_FILLED;

        for (int i = 0; i < COLS; i++) {
            if (i < filled) {
                inv.setItem(PROGRESS_START + i, progressPane(
                        plugin.getLang().get("gui.progress-done", Map.of("pct", pctStr)),
                        plugin.getLang().get("gui.progress-found", Map.of(
                                "found", String.valueOf(foundCount),
                                "total", String.valueOf(visibleTotal))),
                        barMat));
            } else {
                inv.setItem(PROGRESS_START + i, progressPane(
                        plugin.getLang().get("gui.progress-empty", Map.of(
                                "pct", pctStr,
                                "found", String.valueOf(foundCount),
                                "total", String.valueOf(visibleTotal))),
                        null, PROGRESS_EMPTY));
            }
        }

        inv.setItem(SLOT_PREV, page > 0 ? arrow(false, page) : filler());
        for (int i = 46; i < SLOT_NEXT; i++) {
            inv.setItem(i, filler());
        }
        inv.setItem(SLOT_FILTER, filterButton(filter));
        inv.setItem(SLOT_NEXT, page < maxPage ? arrow(true, page) : filler());

        player.openInventory(inv);
    }

    public RareItemCategory nextFilter(RareItemCategory currentFilter) {
        for (int i = 0; i < FILTER_ORDER.length; i++) {
            if (FILTER_ORDER[i] == currentFilter) {
                return FILTER_ORDER[(i + 1) % FILTER_ORDER.length];
            }
        }
        return RareItemCategory.ITEM;
    }

    private void fillPair(Inventory inv, List<RareItem> all, int total, Set<String> found,
                          Player player, int listIdx, int paneSlot, int itemSlot, int globalTotal) {
        if (listIdx < total) {
            RareItem ri = all.get(listIdx);
            boolean fnd = found.contains(ri.id());
            inv.setItem(paneSlot, buildPane(ri, fnd, globalTotal));
            inv.setItem(itemSlot, buildItem(ri, fnd, player));
        } else {
            inv.setItem(paneSlot, filler());
            inv.setItem(itemSlot, filler());
        }
    }

    public void grantReward(Player player, RareItem item, int total) {
        int foundCount = plugin.getPlayerDataManager().countFound(player);
        boolean allFound = foundCount >= total;
        String timestamp = LocalDateTime.now().format(TIME_FMT);

        player.sendMessage(color(plugin.getLang().get("messages.marked-found", Map.of(
                "prefix", plugin.getLang().prefix(),
                "item", item.displayName(),
                "rank", String.valueOf(item.rarity()),
                "total", String.valueOf(total),
                "time", timestamp))));

        if (allFound) {
            String announce = plugin.getLang().get("messages.announce-all", Map.of(
                    "player", player.getName(),
                    "total", String.valueOf(total),
                    "time", timestamp));
            plugin.getSchedulerBridge().runGlobal(() -> Bukkit.broadcastMessage(color(announce)));

            player.sendTitle(
                    color(plugin.getLang().get("messages.complete-title")),
                    color(plugin.getLang().get("messages.complete-subtitle", Map.of("total", String.valueOf(total)))),
                    10, 100, 20
            );

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
            spawnMultiFirework(player);
        } else {
            player.sendTitle(
                    color(item.displayName()),
                    color(plugin.getLang().get("gui.rank", Map.of(
                            "rank", String.valueOf(item.rarity()),
                            "total", String.valueOf(total)))),
                    8, 60, 16
            );

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            spawnFirework(player);
        }

        runConfiguredRewards(player, item, total, allFound, timestamp);
    }

    private void runConfiguredRewards(Player player, RareItem item, int total, boolean allFound, String timestamp) {
        if (!plugin.getConfig().getBoolean("rewards.enabled", false)) {
            return;
        }

        List<String> foundCommands = plugin.getConfig().getStringList("rewards.commands.on-found");
        List<String> completeCommands = plugin.getConfig().getStringList("rewards.commands.on-complete");
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        for (String raw : foundCommands) {
            executeConfiguredCommand(console, raw, player, item, total, timestamp);
        }
        if (allFound) {
            for (String raw : completeCommands) {
                executeConfiguredCommand(console, raw, player, item, total, timestamp);
            }
        }
    }

    private void executeConfiguredCommand(ConsoleCommandSender console, String rawCommand,
                                          Player player, RareItem item, int total, String timestamp) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return;
        }

        String command = rawCommand
                .replace("{player}", player.getName())
                .replace("{item}", ChatColor.stripColor(color(item.displayName())))
                .replace("{id}", item.id())
                .replace("{rank}", String.valueOf(item.rarity()))
                .replace("{total}", String.valueOf(total))
                .replace("{time}", timestamp);

        plugin.getSchedulerBridge().runGlobal(() ->
                Bukkit.dispatchCommand(console, command.startsWith("/") ? command.substring(1) : command)
        );
    }

    private ItemStack buildPane(RareItem ri, boolean found, int total) {
        ItemStack pane = new ItemStack(found ? PANE_FOUND : PANE_UNFOUND);
        ItemMeta meta = pane.getItemMeta();
        String name = ChatColor.stripColor(color(ri.displayName()));

        meta.setDisplayName(plugin.getLang().get(found ? "gui.pane-found" : "gui.pane-unfound", Map.of("name", name)));
        meta.setLore(List.of(
                plugin.getLang().get("gui.rank", Map.of(
                        "rank", String.valueOf(ri.rarity()),
                        "total", String.valueOf(total))),
                plugin.getLang().get(found
                        ? (ri.occurrenceOnly() ? "gui.pane-occurred-label" : "gui.pane-found-label")
                        : "gui.pane-click-hint")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack buildItem(RareItem ri, boolean found, Player player) {
        ItemStack stack = new ItemStack(ri.material());
        ItemMeta meta = stack.getItemMeta();
        String plainName = ChatColor.stripColor(color(ri.displayName()));

        List<String> lore = new ArrayList<>();
        lore.add(color("&6&l" + ri.chance()));
        lore.add(color("&7&o" + ri.description()));
        lore.add(plugin.getLang().get("gui.category-label", Map.of(
                "category", plugin.getLang().get(filterKey(ri.category())))));

        if (found) {
            String ts = plugin.getPlayerDataManager().getFoundTimestamp(player, ri.id());
            if (!ts.isEmpty()) {
                lore.add(plugin.getLang().get("gui.found-at", Map.of("time", ts)));
            }
            meta.setDisplayName(color("&a&l" + plainName));
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        } else {
            meta.setDisplayName(color("&7" + plainName));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack filterButton(RareItemCategory activeFilter) {
        ItemStack stack = new ItemStack(Material.HOPPER);
        ItemMeta meta = stack.getItemMeta();
        RareItemCategory nextFilter = nextFilter(activeFilter);
        String currentLabel = activeFilter == null
                ? plugin.getLang().get("gui.filter-all")
                : plugin.getLang().get(filterKey(activeFilter));
        String nextLabel = nextFilter == null
                ? plugin.getLang().get("gui.filter-all")
                : plugin.getLang().get(filterKey(nextFilter));

        meta.setDisplayName(plugin.getLang().get("gui.filter-title"));
        meta.setLore(List.of(
                plugin.getLang().get("gui.filter-current", Map.of("filter", currentLabel)),
                plugin.getLang().get("gui.filter-cycle", Map.of("next", nextLabel))
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isPaneRow(int slot) {
        return (slot >= PANE_TOP_START && slot < PANE_TOP_START + COLS)
                || (slot >= PANE_BOT_START && slot < PANE_BOT_START + COLS);
    }

    public boolean isItemRow(int slot) {
        return (slot >= ITEM_TOP_START && slot < ITEM_TOP_START + COLS)
                || (slot >= ITEM_BOT_START && slot < ITEM_BOT_START + COLS);
    }

    public int paneSlotToListIndex(int slot, int page) {
        if (slot >= PANE_TOP_START && slot < PANE_TOP_START + COLS) {
            return page * ITEMS_PER_PAGE + (slot - PANE_TOP_START);
        }
        if (slot >= PANE_BOT_START && slot < PANE_BOT_START + COLS) {
            return page * ITEMS_PER_PAGE + COLS + (slot - PANE_BOT_START);
        }
        return -1;
    }

    private ItemStack progressPane(String name, String lore, Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(List.of(lore));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack arrow(boolean next, int currentPage) {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.getLang().get(next ? "gui.nav-next" : "gui.nav-prev",
                Map.of("page", String.valueOf(next ? currentPage + 2 : currentPage))));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack filler() {
        ItemStack stack = new ItemStack(NAV_FILLER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(" ");
        stack.setItemMeta(meta);
        return stack;
    }

    private void spawnFirework(Player player) {
        Firework fw = player.getWorld().spawn(player.getLocation().add(0, 1, 0), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.setPower(Math.max(0, plugin.getConfig().getInt("firework-power", 1)));
        meta.addEffect(FireworkEffect.builder()
                .with(random(FireworkEffect.Type.values()))
                .withColor(randomColor(), randomColor())
                .withFade(randomColor())
                .trail(true).flicker(true).build());
        fw.setFireworkMeta(meta);
    }

    private void spawnMultiFirework(Player player) {
        for (int i = 0; i < 4; i++) {
            Firework fw = player.getWorld().spawn(player.getLocation().add(rng.nextDouble() - .5, 1, rng.nextDouble() - .5), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.setPower(Math.max(1, plugin.getConfig().getInt("firework-power", 1)));
            meta.addEffect(FireworkEffect.builder()
                    .with(random(FireworkEffect.Type.values()))
                    .withColor(randomColor(), randomColor(), randomColor())
                    .withFade(randomColor())
                    .trail(true).flicker(true).build());
            fw.setFireworkMeta(meta);
        }
    }

    private <T> T random(T[] values) {
        return values[rng.nextInt(values.length)];
    }

    private Color randomColor() {
        return Color.fromRGB(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private String filterKey(RareItemCategory category) {
        return switch (category) {
            case ITEM -> "gui.filter-item";
            case MOB -> "gui.filter-mob";
            case STRUCTURE -> "gui.filter-structure";
            case CUSTOM -> "gui.filter-custom";
        };
    }
}
