package dev.zyvera.rareitemshunter.listener;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.gui.RareItemsGUI;
import dev.zyvera.rareitemshunter.model.RareItem;
import dev.zyvera.rareitemshunter.model.RareItemCategory;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InventoryClickListener implements Listener {

    private final RareItemsHunter plugin;

    public InventoryClickListener(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!plugin.hasGuiOpen(player)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < 54) {
            handleClick(player, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCreativeClick(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (plugin.hasGuiOpen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (plugin.hasGuiOpen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!plugin.isNavigationLocked(player.getUniqueId())) {
            plugin.clearGuiPage(player.getUniqueId());
        }
    }

    private void handleClick(Player player, int slot) {
        int page = plugin.getGuiPage(player.getUniqueId());
        RareItemCategory currentFilter = plugin.getGuiFilter(player.getUniqueId());
        List<RareItem> visibleItems = plugin.getRareItemManager().getItems(currentFilter);
        int visibleTotal = visibleItems.size();
        int totalAll = plugin.getRareItemManager().count();

        if (slot == RareItemsGUI.SLOT_PREV && page > 0) {
            plugin.getGui().open(player, page - 1);
            return;
        }
        if (slot == RareItemsGUI.SLOT_NEXT) {
            int maxPage = Math.max(0, (visibleTotal + RareItemsGUI.ITEMS_PER_PAGE - 1) / RareItemsGUI.ITEMS_PER_PAGE - 1);
            if (page < maxPage) {
                plugin.getGui().open(player, page + 1);
                return;
            }
        }

        if (slot == RareItemsGUI.SLOT_FILTER) {
            plugin.setGuiFilter(player.getUniqueId(), plugin.getGui().nextFilter(currentFilter));
            plugin.getGui().open(player, 0);
            return;
        }

        if (slot >= RareItemsGUI.PROGRESS_START || plugin.getGui().isItemRow(slot) || !plugin.getGui().isPaneRow(slot)) {
            return;
        }

        int listIdx = plugin.getGui().paneSlotToListIndex(slot, page);
        if (listIdx < 0 || listIdx >= visibleTotal) {
            return;
        }

        RareItem ri = visibleItems.get(listIdx);

        if (plugin.getPlayerDataManager().hasFound(player, ri.id())) {
            player.sendMessage(color(plugin.getLang().get("messages.already-found", Map.of(
                    "prefix", plugin.getLang().prefix(),
                    "item", ri.displayName()))));
            return;
        }

        if (!ri.occurrenceOnly() && !hasItemInInventory(player, ri)) {
            player.sendMessage(color(plugin.getLang().get("messages.item-not-found", Map.of(
                    "prefix", plugin.getLang().prefix(),
                    "item", ri.displayName()))));
            return;
        }

        plugin.getPlayerDataManager().markFound(player, ri.id());
        player.closeInventory();
        plugin.getGui().grantReward(player, ri, totalAll);
    }

    private boolean hasItemInInventory(Player player, RareItem ri) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(stack -> stack != null)
                .anyMatch(stack -> stack.getType() == ri.material());
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
