package com.zskv.fATFSCore.bossbar;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BossBarManager {
    private final FATFSCore plugin;
    private final Map<String, BossBar> bossBars;

    public BossBarManager(FATFSCore plugin) {
        this.plugin = plugin;
        this.bossBars = new HashMap<>();
    }

    public BossBar createBossBar(String id, String title, BarColor color, BarStyle style) {
        if (bossBars.containsKey(id)) {
            removeBossBar(id);
        }
        BossBar bar = Bukkit.createBossBar(title, color, style);
        bossBars.put(id, bar);
        return bar;
    }

    public BossBar getBossBar(String id) {
        return bossBars.get(id);
    }

    public void removeBossBar(String id) {
        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    public void showToAll(String id) {
        BossBar bar = bossBars.get(id);
        if (bar != null) {
            bar.setVisible(true);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bar.addPlayer(player);
            }
        }
    }

    public void hideFromAll(String id) {
        BossBar bar = bossBars.get(id);
        if (bar != null) {
            bar.setVisible(false);
            bar.removeAll();
        }
    }

    public void handleJoin(Player player) {
        for (BossBar bar : bossBars.values()) {
            if (bar.isVisible()) {
                bar.addPlayer(player);
            }
        }
    }

    public void clearAll() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        bossBars.clear();
    }
}
