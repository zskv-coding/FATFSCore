package com.zskv.fATFSCore.minigames.HideSeek;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.entity.Player;

public class HideSeekManager {

    private final FATFSCore plugin;
    private final HideSeekConfig config;
    private final HideSeekMapGenerator mapGenerator;
    private boolean running = false;

    public HideSeekManager(FATFSCore plugin) {
        this.plugin = plugin;
        this.config = new HideSeekConfig(plugin);
        this.config.load();
        this.mapGenerator = new HideSeekMapGenerator(plugin, config);
    }

    public void startSequence(Player initiator) {
        if (running) return;
        running = true;

        mapGenerator.generate();
        initiator.teleport(config.getSpawnLocation());
    }

    public void stopGame(boolean force) {
        if (!running && !force) return;
        running = false;

        mapGenerator.clear();
        // send players to hub
    }

    public boolean isRunning() {
        return running;
    }
}