package de.myzelyam.supervanish.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public interface TaskScheduler {
    ScheduledTask runTask(Runnable task);
    ScheduledTask runTaskLater(Runnable task, long delay);
    ScheduledTask runTaskTimer(Runnable task, long delay, long period);
    ScheduledTask runTaskAsynchronously(Runnable task);
    ScheduledTask runTaskLaterAsynchronously(Runnable task, long delay);
    ScheduledTask runTaskTimerAsynchronously(Runnable task, long delay, long period);
    ScheduledTask runTaskAtLocation(Location location, Runnable task);
    ScheduledTask runTaskAtEntity(Entity entity, Runnable task);
    void cancelAllTasks();
    boolean isFolia();
    Plugin getPlugin();
}
