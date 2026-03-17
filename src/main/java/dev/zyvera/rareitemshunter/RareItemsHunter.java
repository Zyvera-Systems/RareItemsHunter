package dev.zyvera.rareitemshunter;

import dev.zyvera.rareitemshunter.command.RiCommand;
import dev.zyvera.rareitemshunter.gui.RareItemsGUI;
import dev.zyvera.rareitemshunter.lang.LangManager;
import dev.zyvera.rareitemshunter.listener.InventoryClickListener;
import dev.zyvera.rareitemshunter.listener.PlayerJoinQuitListener;
import dev.zyvera.rareitemshunter.manager.PlayerDataManager;
import dev.zyvera.rareitemshunter.manager.RareItemManager;
import dev.zyvera.rareitemshunter.model.RareItemCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class RareItemsHunter extends JavaPlugin {

    private RareItemManager rareItemManager;
    private PlayerDataManager playerDataManager;
    private RareItemsGUI gui;
    private LangManager lang;

    private final Map<UUID, Integer>          openGuiPages   = new HashMap<>();
    private final Map<UUID, RareItemCategory> openGuiFilters = new HashMap<>();
    private final Set<UUID>                   navLock        = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        lang = new LangManager(this);
        lang.load();

        rareItemManager = new RareItemManager(this);
        rareItemManager.reload();

        playerDataManager = new PlayerDataManager(this);
        gui = new RareItemsGUI(this);

        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);

        RiCommand riCmd = new RiCommand(this);
        var cmd = getCommand("ri");
        if (cmd != null) {
            cmd.setExecutor(riCmd);
            cmd.setTabCompleter(riCmd);
        }

        getServer().getOnlinePlayers().forEach(playerDataManager::load);

        getLogger().info("RareItemsHunter v" + getDescription().getVersion()
                + " by Zyvera-Systems & Thomas U. | " + rareItemManager.count() + " items loaded.");
    }

    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(p -> {
            playerDataManager.save(p);
            playerDataManager.unload(p.getUniqueId());
        });
        openGuiPages.clear();
        openGuiFilters.clear();
        navLock.clear();
    }

    public void setGuiPage(UUID uuid, int page) {
        openGuiPages.put(uuid, page);
    }

    public int getGuiPage(UUID uuid) {
        return openGuiPages.getOrDefault(uuid, -1);
    }

    public void setGuiFilter(UUID uuid, RareItemCategory filter) {
        if (filter == null) {
            openGuiFilters.remove(uuid);
            return;
        }
        openGuiFilters.put(uuid, filter);
    }

    public RareItemCategory getGuiFilter(UUID uuid) {
        return openGuiFilters.get(uuid);
    }

    public void clearGuiPage(UUID uuid) {
        openGuiPages.remove(uuid);
        openGuiFilters.remove(uuid);
    }

    public boolean hasGuiOpen(Player player) {
        return openGuiPages.containsKey(player.getUniqueId());
    }

    public void lockNavigation(UUID uuid) {
        navLock.add(uuid);
        getServer().getScheduler().runTaskLater(this, () -> navLock.remove(uuid), 1L);
    }

    public boolean isNavigationLocked(UUID uuid) {
        return navLock.contains(uuid);
    }

    public RareItemManager getRareItemManager() {
        return rareItemManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public RareItemsGUI getGui() {
        return gui;
    }

    public LangManager getLang() {
        return lang;
    }
}
