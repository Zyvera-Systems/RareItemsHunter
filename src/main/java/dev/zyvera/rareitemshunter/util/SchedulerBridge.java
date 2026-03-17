package dev.zyvera.rareitemshunter.util;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class SchedulerBridge {

    private final Plugin plugin;
    private final Method globalSchedulerAccessor;
    private final Method globalRunMethod;
    private final Method globalRunDelayedMethod;
    private final Method entitySchedulerAccessor;
    private final Method entityRunMethod;
    private boolean warnedGlobalFallback;
    private boolean warnedEntityFallback;

    public SchedulerBridge(Plugin plugin) {
        this.plugin = plugin;

        Method globalAccessor = null;
        Method globalRun = null;
        Method globalRunDelayed = null;
        try {
            globalAccessor = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
            Class<?> schedulerClass = globalAccessor.getReturnType();
            globalRun = schedulerClass.getMethod("run", Plugin.class, Consumer.class);
            globalRunDelayed = schedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
        } catch (ReflectiveOperationException ignored) {
            globalAccessor = null;
            globalRun = null;
            globalRunDelayed = null;
        }
        this.globalSchedulerAccessor = globalAccessor;
        this.globalRunMethod = globalRun;
        this.globalRunDelayedMethod = globalRunDelayed;

        Method entityAccessor = null;
        Method entityRun = null;
        try {
            entityAccessor = Entity.class.getMethod("getScheduler");
            Class<?> schedulerClass = entityAccessor.getReturnType();
            entityRun = schedulerClass.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
        } catch (ReflectiveOperationException ignored) {
            entityAccessor = null;
            entityRun = null;
        }
        this.entitySchedulerAccessor = entityAccessor;
        this.entityRunMethod = entityRun;
    }

    public boolean supportsGlobalRegionScheduler() {
        return globalSchedulerAccessor != null && globalRunMethod != null && globalRunDelayedMethod != null;
    }

    public boolean supportsEntityScheduler() {
        return entitySchedulerAccessor != null && entityRunMethod != null;
    }

    public void runGlobal(Runnable task) {
        if (supportsGlobalRegionScheduler()) {
            try {
                Object scheduler = globalSchedulerAccessor.invoke(plugin.getServer());
                Consumer<Object> consumer = scheduledTask -> task.run();
                globalRunMethod.invoke(scheduler, plugin, consumer);
                return;
            } catch (ReflectiveOperationException ex) {
                warnGlobalFallback(ex);
            }
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    public void runGlobalLater(Runnable task, long delayTicks) {
        long safeDelay = Math.max(1L, delayTicks);
        if (supportsGlobalRegionScheduler()) {
            try {
                Object scheduler = globalSchedulerAccessor.invoke(plugin.getServer());
                Consumer<Object> consumer = scheduledTask -> task.run();
                globalRunDelayedMethod.invoke(scheduler, plugin, consumer, safeDelay);
                return;
            } catch (ReflectiveOperationException ex) {
                warnGlobalFallback(ex);
            }
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, task, safeDelay);
    }

    public void runForEntity(Entity entity, Runnable task) {
        if (supportsEntityScheduler()) {
            try {
                Object scheduler = entitySchedulerAccessor.invoke(entity);
                Consumer<Object> consumer = scheduledTask -> task.run();
                entityRunMethod.invoke(scheduler, plugin, consumer, null);
                return;
            } catch (ReflectiveOperationException ex) {
                warnEntityFallback(ex);
            }
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    private void warnGlobalFallback(Exception ex) {
        if (!warnedGlobalFallback) {
            warnedGlobalFallback = true;
            plugin.getLogger().warning("GlobalRegionScheduler could not be used, falling back to Bukkit scheduler: " + ex.getClass().getSimpleName());
        }
    }

    private void warnEntityFallback(Exception ex) {
        if (!warnedEntityFallback) {
            warnedEntityFallback = true;
            plugin.getLogger().warning("EntityScheduler could not be used, falling back to Bukkit scheduler: " + ex.getClass().getSimpleName());
        }
    }
}
