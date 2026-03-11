package dev.zyvera.rareitemshunter.listener;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import dev.zyvera.rareitemshunter.gui.RareItemsGUI;
import dev.zyvera.rareitemshunter.model.RareItem;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import java.util.List;
import java.util.Map;

public class InventoryClickListener implements Listener {

    private final RareItemsHunter plugin;

    public InventoryClickListener(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.hasGuiOpen(player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot >= 0 && slot < 54) handleClick(player, slot);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCreativeClick(InventoryCreativeEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.hasGuiOpen(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.hasGuiOpen(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {

        if (!(event.getPlayer() instanceof Player player)) return;
        if (!plugin.isNavigationLocked(player.getUniqueId()))
            plugin.clearGuiPage(player.getUniqueId());
    }

    private void handleClick(Player player, int slot) {
        int page = plugin.getGuiPage(player.getUniqueId());
        int total = plugin.getRareItemManager().count();
        List<RareItem> all = plugin.getRareItemManager().getItems();

        if (slot == RareItemsGUI.SLOT_PREV && page > 0) {
            plugin.getGui().open(player, page - 1);
            return;
        }

        if (slot == RareItemsGUI.SLOT_NEXT) {
            int maxPage = Math.max(0, (int) Math.ceil((double) total / RareItemsGUI.ITEMS_PER_PAGE) - 1);
            if (page < maxPage) { plugin.getGui().open(player, page + 1); return; }
        }

        if (slot >= RareItemsGUI.PROGRESS_START) return;
        if (plugin.getGui().isItemRow(slot)) return;
        if (!plugin.getGui().isPaneRow(slot)) return;

        int listIdx = plugin.getGui().paneSlotToListIndex(slot, page);
        if (listIdx < 0 || listIdx >= total) return;

        RareItem ri = all.get(listIdx);

        if (plugin.getPlayerDataManager().hasFound(player, ri.id())) {
            player.sendMessage(c(plugin.getLang().get("messages.already-found", Map.of(
                    "prefix", plugin.getLang().prefix(),
                    "item", ri.displayName()))));
            return;
        }

        plugin.getPlayerDataManager().markFound(player, ri.id());
        plugin.getGui().grantReward(player, ri, total);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.hasGuiOpen(player))
                plugin.getGui().open(player, page);
        }, 5L);
    }

    private net.kyori.adventure.text.Component c(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
