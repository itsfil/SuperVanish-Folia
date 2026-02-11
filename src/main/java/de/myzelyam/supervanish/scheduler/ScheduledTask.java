package de.myzelyam.supervanish.scheduler;

public interface ScheduledTask {
    void cancel();
    boolean isCancelled();
}
