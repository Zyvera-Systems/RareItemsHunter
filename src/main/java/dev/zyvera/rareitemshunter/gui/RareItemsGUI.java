package dev.zyvera.rareitemshunter.gui;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.model.RareItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RareItemsGUI {

    public static final int ITEMS_PER_PAGE = 18;
    public static final int PANE_TOP_START = 0;
    public static final int ITEM_TOP_START = 9;
    public static final int ITEM_BOT_START = 18;
    public static final int PANE_BOT_START = 27;
    public static final int PROGRESS_START = 36;
    public static final int NAV_START      = 45;
    public static final int SLOT_PREV      = 45;
    public static final int SLOT_NEXT      = 53;
    private static final int COLS          = 9;

    private static final Material PANE_UNFOUND      = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material PANE_FOUND        = Material.LIME_STAINED_GLASS_PANE;
    private static final Material PROGRESS_FILLED   = Material.GREEN_STAINED_GLASS_PANE;
    private static final Material PROGRESS_COMPLETE = Material.ORANGE_STAINED_GLASS_PANE;
    private static final Material PROGRESS_EMPTY    = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    private static final Material NAV_FILLER        = Material.GRAY_STAINED_GLASS_PANE;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final RareItemsHunter plugin;
    private final Random          rng = new Random();

    public RareItemsGUI(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        List<RareItem> all  = plugin.getRareItemManager().getItems();
        int total           = all.size();
        int maxPage         = Math.max(0, (int) Math.ceil((double) total / ITEMS_PER_PAGE) - 1);
        page = Math.max(0, Math.min(page, maxPage));

        plugin.lockNavigation(player.getUniqueId());
        plugin.setGuiPage(player.getUniqueId(), page);

        String rawTitle = plugin.getConfig().getString("gui-title", "&8\u00bb &6&lSeltene Items &8\u00ab");
        String title    = color(rawTitle) + ChatColor.GRAY + " [" + (page + 1) + "/" + (maxPage + 1) + "]";
        Inventory inv   = Bukkit.createInventory(null, 54, c(title));

        Set<String> found = plugin.getPlayerDataManager().getFoundItems(player);
        boolean full      = total > 0 && found.size() >= total;

        for (int col = 0; col < COLS; col++) {
            fillPair(inv, all, total, found, player, page * ITEMS_PER_PAGE + col,
                    PANE_TOP_START + col, ITEM_TOP_START + col);
            fillPair(inv, all, total, found, player, page * ITEMS_PER_PAGE + COLS + col,
                    PANE_BOT_START + col, ITEM_BOT_START + col);
        }

        int    foundCount = found.size();
        double pct        = total == 0 ? 0 : (double) foundCount / total;
        int    filled     = (int) Math.round(pct * COLS);
        String pctStr     = String.format("%.1f%%", pct * 100);
        Material barMat   = full ? PROGRESS_COMPLETE : PROGRESS_FILLED;

        for (int i = 0; i < COLS; i++) {
            if (i < filled) {
                inv.setItem(PROGRESS_START + i, progressPane(
                        plugin.getLang().get("gui.progress-done", Map.of("pct", pctStr)),
                        plugin.getLang().get("gui.progress-found", Map.of(
                                "found", String.valueOf(foundCount), "total", String.valueOf(total))),
                        barMat));
            } else {
                inv.setItem(PROGRESS_START + i, progressPane(
                        plugin.getLang().get("gui.progress-empty", Map.of(
                                "pct", pctStr, "found", String.valueOf(foundCount), "total", String.valueOf(total))),
                        null, PROGRESS_EMPTY));
            }
        }

        inv.setItem(SLOT_PREV, page > 0       ? arrow(false, page) : filler());
        for (int i = NAV_START + 1; i < SLOT_NEXT; i++) inv.setItem(i, filler());
        inv.setItem(SLOT_NEXT, page < maxPage ? arrow(true, page)  : filler());

        player.openInventory(inv);
    }

    private void fillPair(Inventory inv, List<RareItem> all, int total, Set<String> found,
                           Player player, int listIdx, int paneSlot, int itemSlot) {
        if (listIdx < total) {
            RareItem ri  = all.get(listIdx);
            boolean  fnd = found.contains(ri.id());
            inv.setItem(paneSlot, buildPane(ri, fnd, total));
            inv.setItem(itemSlot, buildItem(ri, fnd, player));
        } else {
            inv.setItem(paneSlot, filler());
            inv.setItem(itemSlot, filler());
        }
    }

    public void grantReward(Player player, RareItem item, int total) {
        int     foundCount = plugin.getPlayerDataManager().countFound(player);
        boolean allFound   = foundCount >= total;
        String  timestamp  = LocalDateTime.now().format(TIME_FMT);

        player.sendMessage(c(plugin.getLang().get("messages.marked-found", Map.of(
                "prefix", plugin.getLang().prefix(),
                "item",   item.displayName(),
                "rank",   String.valueOf(item.rarity()),
                "total",  String.valueOf(total),
                "time",   timestamp))));

        if (allFound) {
            String announce = plugin.getLang().get("messages.announce-all", Map.of(
                    "player", player.getName(),
                    "total",  String.valueOf(total),
                    "time",   timestamp));
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(c(announce)));

            player.showTitle(net.kyori.adventure.title.Title.title(
                    c(plugin.getLang().get("messages.complete-title")),
                    c(plugin.getLang().get("messages.complete-subtitle", Map.of("total", String.valueOf(total)))),
                    net.kyori.adventure.title.Title.Times.times(
                            Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(1000))));

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
            spawnMultiFirework(player);
        } else {
            player.showTitle(net.kyori.adventure.title.Title.title(
                    c(item.displayName()),
                    c(plugin.getLang().get("gui.rank", Map.of(
                            "rank",  String.valueOf(item.rarity()),
                            "total", String.valueOf(total)))),
                    net.kyori.adventure.title.Title.Times.times(
                            Duration.ofMillis(400), Duration.ofMillis(3000), Duration.ofMillis(800))));

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            spawnFirework(player);
        }
    }

    private ItemStack buildPane(RareItem ri, boolean found, int total) {
        ItemStack pane = new ItemStack(found ? PANE_FOUND : PANE_UNFOUND);
        ItemMeta  meta = pane.getItemMeta();
        String    name = ChatColor.stripColor(color(ri.displayName()));

        meta.displayName(c(plugin.getLang().get(found ? "gui.pane-found" : "gui.pane-unfound",
                Map.of("name", name))));
        meta.lore(List.of(
                c(plugin.getLang().get("gui.rank", Map.of(
                        "rank", String.valueOf(ri.rarity()), "total", String.valueOf(total)))),
                c(plugin.getLang().get(found
                        ? (ri.occurrenceOnly() ? "gui.pane-occurred-label" : "gui.pane-found-label")
                        : "gui.pane-click-hint"))
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack buildItem(RareItem ri, boolean found, Player player) {
        ItemStack stack = new ItemStack(ri.material());
        ItemMeta  meta  = stack.getItemMeta();
        String    name  = ChatColor.stripColor(color(ri.displayName()));

        if (found) {
            String ts = plugin.getPlayerDataManager().getFoundTimestamp(player, ri.id());
            List<Component> lore = new ArrayList<>();
            lore.add(c("&6&l" + ri.chance()));
            lore.add(c("&7&o" + ri.description()));
            if (!ts.isEmpty()) lore.add(c("&8Gefunden: &7" + ts));
            meta.displayName(c("&a&l" + name));
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        } else {
            meta.displayName(c("&7" + name));
            meta.lore(List.of(c("&6&l" + ri.chance()), c("&7&o" + ri.description())));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
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
        if (slot >= PANE_TOP_START && slot < PANE_TOP_START + COLS)
            return page * ITEMS_PER_PAGE + (slot - PANE_TOP_START);
        if (slot >= PANE_BOT_START && slot < PANE_BOT_START + COLS)
            return page * ITEMS_PER_PAGE + COLS + (slot - PANE_BOT_START);
        return -1;
    }

    private ItemStack filler() {
        ItemStack s = new ItemStack(NAV_FILLER);
        ItemMeta  m = s.getItemMeta();
        m.displayName(c(" "));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        s.setItemMeta(m);
        return s;
    }

    private ItemStack progressPane(String title, String subtitle, Material mat) {
        ItemStack s = new ItemStack(mat);
        ItemMeta  m = s.getItemMeta();
        m.displayName(c(title));
        if (subtitle != null) m.lore(List.of(c(subtitle)));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        s.setItemMeta(m);
        return s;
    }

    private ItemStack arrow(boolean next, int page) {
        ItemStack a = new ItemStack(Material.ARROW);
        ItemMeta  m = a.getItemMeta();
        m.displayName(c(plugin.getLang().get(next ? "gui.nav-next" : "gui.nav-prev",
                Map.of("page", String.valueOf(next ? page + 2 : page)))));
        a.setItemMeta(m);
        return a;
    }

    private void spawnFirework(Player player) {
        Color[] palette = {Color.YELLOW, Color.ORANGE, Color.RED, Color.AQUA,
                           Color.LIME, Color.FUCHSIA, Color.WHITE, Color.PURPLE};
        FireworkEffect fx = FireworkEffect.builder()
                .withColor(palette[rng.nextInt(palette.length)], palette[rng.nextInt(palette.length)])
                .withFade(Color.WHITE).with(FireworkEffect.Type.BALL_LARGE).trail(true).flicker(true).build();
        spawnFw(player.getLocation(), plugin.getConfig().getInt("firework-power", 1), fx);
    }

    private void spawnMultiFirework(Player player) {
        int power = Math.max(2, plugin.getConfig().getInt("firework-power", 1) + 1);
        FireworkEffect.Type[] types  = {FireworkEffect.Type.BALL_LARGE, FireworkEffect.Type.STAR,
                                        FireworkEffect.Type.BURST, FireworkEffect.Type.CREEPER, FireworkEffect.Type.BALL};
        Color[][] combos = {{Color.ORANGE, Color.YELLOW}, {Color.FUCHSIA, Color.WHITE},
                            {Color.AQUA, Color.LIME},   {Color.RED, Color.ORANGE}, {Color.WHITE, Color.PURPLE}};
        var loc = player.getLocation();
        for (int i = 0; i < 5; i++) {
            final int fi = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                double angle = fi * (2 * Math.PI / 5);
                FireworkEffect fx = FireworkEffect.builder()
                        .withColor(combos[fi][0], combos[fi][1]).withFade(Color.WHITE)
                        .with(types[fi]).trail(true).flicker(fi % 2 == 0).build();
                spawnFw(loc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5), power, fx);
            }, i * 4L);
        }
    }

    private void spawnFw(org.bukkit.Location loc, int power, FireworkEffect fx) {
        loc.getWorld().spawn(loc, Firework.class, fw -> {
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(fx);
            meta.setPower(power);
            fw.setFireworkMeta(meta);
        });
    }

    private static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private static Component c(String s)  { return LegacyComponentSerializer.legacySection().deserialize(color(s)); }
}
