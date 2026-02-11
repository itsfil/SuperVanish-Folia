package de.myzelyam.supervanish.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PaperScheduler implements TaskScheduler {
    private final Plugin plugin;
    private final ConcurrentHashMap<Integer, BukkitTask> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);

    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ScheduledTask runTask(Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
        return wrapTask(bukkitTask);
    }

    @Override
    public ScheduledTask runTaskLater(Runnable task, long delay) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        return wrapTask(bukkitTask);
    }

    @Override
    public ScheduledTask runTaskTimer(Runnable task, long delay, long period) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        return wrapTask(bukkitTask);
    }

    @Override
    public ScheduledTask runTaskAsynchronously(Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        return wrapTask(bukkitTask);
    }

    @Override
    public ScheduledTask runTaskLaterAsynchronously(Runnable task, long delay) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        return wrapTask(bukkitTask);
    }

    @Override
    public ScheduledTask runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        return wrapTask(bukkitTask);
    }

    @Override
    public ScheduledTask runTaskAtLocation(Location location, Runnable task) {
        return runTask(task);
    }

    @Override
    public ScheduledTask runTaskAtEntity(Entity entity, Runnable task) {
        return runTask(task);
    }

    @Override
    public void cancelAllTasks() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    private ScheduledTask wrapTask(BukkitTask bukkitTask) {
        int taskId = taskIdCounter.incrementAndGet();
        tasks.put(taskId, bukkitTask);
        return new ScheduledTask() {
            private volatile boolean cancelled = false;

            @Override
            public void cancel() {
                if (!cancelled) {
                    cancelled = true;
                    tasks.remove(taskId);
                    bukkitTask.cancel();
                }
            }

            @Override
            public boolean isCancelled() {
                return cancelled || !Bukkit.getScheduler().isQueued(bukkitTask.getTaskId());
            }
        };
    }
}
