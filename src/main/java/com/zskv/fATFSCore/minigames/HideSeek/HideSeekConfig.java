package com.zskv.fATFSCore.minigames.HideSeek;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HideSeekConfig {

    private final FATFSCore plugin;
    private final File file;
    private int doorGap;

    private String worldName;
    private double spawnX, spawnY, spawnZ;
    private float spawnYaw, spawnPitch;
    private String spawnSchem;
    private final List<String> pool = new ArrayList<>();
    private int borderMargin;
    private int borderThickness;
    private int borderHeightPadding;

    public HideSeekConfig(FATFSCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "hideseek.yml");
    }


    public void load() {
        if (!file.exists()) {
            plugin.saveResource("hideseek.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        worldName = cfg.getString("world", "hideseek");

        spawnX = cfg.getDouble("spawn.x");
        spawnY = cfg.getDouble("spawn.y");
        spawnZ = cfg.getDouble("spawn.z");
        spawnYaw = (float) cfg.getDouble("spawn.yaw");
        spawnPitch = (float) cfg.getDouble("spawn.pitch");
        borderMargin = cfg.getInt("border.margin", 3);
        borderThickness = cfg.getInt("border.thickness", 1);
        borderHeightPadding = cfg.getInt("border.height-padding", 5);
        doorGap = cfg.getInt("door-gap", 2);
        spawnSchem = cfg.getString("schematics.spawn", "spawn_room.schem");

        pool.clear();
        pool.addAll(cfg.getStringList("schematics.pool"));

        if (pool.size() < 4) {
            plugin.getLogger().warning("hideseek.yml only has " + pool.size() + " room schematics in the pool, need at least 4.");
        }
    }

    public Location getSpawnLocation() {
        World world = plugin.getServer().getWorld(worldName);
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public String getWorldName() {
        return worldName;
    }


    public String getSpawnSchematic() {
        return spawnSchem;
    }

    public int getDoorGap() {
        return doorGap;
    }

    public int getBorderMargin() {
        return borderMargin;
    }

    public int getBorderThickness() {
        return borderThickness;
    }

    public int getBorderHeightPadding() {
        return borderHeightPadding;
    }

    public List<String> getPool() {
        return pool;
    }
}