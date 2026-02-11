package de.myzelyam.supervanish.scheduler;

import org.bukkit.plugin.Plugin;

public class SchedulerFactory {
    private static Boolean isFolia = null;

    public static TaskScheduler create(Plugin plugin) {
        if (isFolia == null) {
            isFolia = detectFolia();
        }
        return isFolia ? new FoliaScheduler(plugin) : new PaperScheduler(plugin);
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFoliaServer() {
        if (isFolia == null) {
            isFolia = detectFolia();
        }
        return isFolia;
    }
}
