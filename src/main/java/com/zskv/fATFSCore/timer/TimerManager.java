package com.zskv.fATFSCore.timer;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TimerManager {
    private final FATFSCore plugin;
    private static final String TIMER_BAR_ID = "timer_bar";
    private int timeRemaining;
    private int initialTime;
    private boolean paused;
    private String label;
    private BukkitTask timerTask;
    private Runnable onComplete;

    public TimerManager(FATFSCore plugin) {
        this.plugin = plugin;
        this.paused = false;
        this.timeRemaining = 0;
    }

    public void startTimer(String label, int seconds, Runnable onComplete) {
        stopTimer();
        this.label = label;
        this.timeRemaining = seconds;
        this.initialTime = seconds;
        this.paused = false;
        this.onComplete = onComplete;

        plugin.getBossBarManager().createBossBar(TIMER_BAR_ID, formatBossBarTitle(), BarColor.YELLOW, BarStyle.SOLID);
        plugin.getBossBarManager().showToAll(TIMER_BAR_ID);

        runTimer();
    }

    private void runTimer() {
        if (timerTask != null) timerTask.cancel();
        
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (paused) return;
                
                updateDisplay();

                if (timeRemaining <= 0) {
                    stopTimer();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    return;
                }

                timeRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void pause() {
        if (timeRemaining > 0) {
            paused = true;
            updateDisplay();
        }
    }

    public void unpause() {
        if (timeRemaining > 0) {
            paused = false;
            updateDisplay();
        }
    }

    public void skip() {
        if (timeRemaining > 0) {
            timeRemaining = 0;
            updateDisplay();
            stopTimer();
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        plugin.getBossBarManager().hideFromAll(TIMER_BAR_ID);
        timeRemaining = 0;
        plugin.getScoreboardManager().updateAll();
    }

    private void updateDisplay() {
        BossBar bar = plugin.getBossBarManager().getBossBar(TIMER_BAR_ID);
        if (bar != null) {
            bar.setTitle(formatBossBarTitle());
            double progress = Math.max(0.0, Math.min(1.0, (double) timeRemaining / initialTime));
            bar.setProgress(progress);
        }
        plugin.getScoreboardManager().updateAll();
    }

    private String formatBossBarTitle() {
        String timeStr = formatTime(timeRemaining);
        String status = paused ? ChatColor.RED + " (PAUSED)" : "";
        return ChatColor.GOLD + label + ": " + ChatColor.YELLOW + timeStr + status;
    }

    public String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isActive() {
        return timeRemaining > 0;
    }
}
