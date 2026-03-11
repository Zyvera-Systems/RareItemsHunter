package dev.zyvera.rareitemshunter.listener;

import dev.zyvera.rareitemshunter.RareItemsHunter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final RareItemsHunter plugin;

    public PlayerJoinQuitListener(RareItemsHunter plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().save(event.getPlayer());
        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());
    }
}
