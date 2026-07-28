package com.zskv.fATFSCore.minigames.Squabble;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SquabbleMap {

    public static String WORLD_NAME = "Squabble";
    public static Vector SCHEMATIC_PASTE_LOCATION = new Vector(0.5, 91, 0.5);

    public static int MAX_SURVIVAL_REWARD_DEATHS = 10;
    public static int DAMAGE_POOL_TOTAL = 550;
    public static int TOTAL_ROUNDS = 4;

    public static double STARTING_BORDER_SIZE = 100;
    public static double STARTING_SKY_HEIGHT = 146;
    public static double MIN_SKY_HEIGHT = 85;
    public static double FALL_ELIMINATION_MIN_Y = 60;

    public static int ROUND_DURATION_SECONDS = 120;
    public static int HOW_TO_PLAY_SECONDS = 20;
    public static int STARTING_IN_SECONDS = 30;
    public static int FINAL_COUNTDOWN_SECONDS = 10;
    public static int NEXT_ROUND_SECONDS = 10;

    public static int OVERTIME_INTERVAL_SECONDS = 20;
    public static int OVERTIME_WARNING_LEAD_SECONDS = 5;
    public static double OVERTIME_HEALTH_DRAIN = 2.0;
    public static double OVERTIME_MIN_HEALTH = 6.0;
    public static double OVERTIME_BORDER_SHRINK_AMOUNT = 15;
    public static int OVERTIME_BORDER_TRANSITION_SECONDS = 15;
    public static double OVERTIME_BORDER_MINIMUM = 2;
    public static double OVERTIME_SKY_SHRINK_AMOUNT = 10;

    public static double LOOT_SCAN_CENTER_X = 0;
    public static double LOOT_SCAN_CENTER_Z = 0;
    public static double LOOT_SCAN_RADIUS = 40;
    public static double LOOT_SCAN_MIN_Y = 76;
    public static double LOOT_SCAN_MAX_Y = 92;

    public static class TeamData {
        public final String id;
        public final Vector spawnMin;
        public final Vector spawnMax;
        public final Vector barrierMin;
        public final Vector barrierMax;
        public final Material spawnBlockType;
        public final Color armorColor;

        public TeamData(String id, Vector spawnMin, Vector spawnMax, Vector barrierMin, Vector barrierMax,
                        Material spawnBlockType, Color armorColor) {
            this.id = id;
            this.spawnMin = spawnMin;
            this.spawnMax = spawnMax;
            this.barrierMin = barrierMin;
            this.barrierMax = barrierMax;
            this.spawnBlockType = spawnBlockType;
            this.armorColor = armorColor;
        }
    }

    public static final Map<String, TeamData> TEAMS = new HashMap<>();

    public static void load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "squabble.yml");
        if (!file.exists()) {
            plugin.saveResource("squabble.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        WORLD_NAME = config.getString("world-name", WORLD_NAME);
        SCHEMATIC_PASTE_LOCATION = readVector(
                config.getConfigurationSection("schematic-paste-location"),
                SCHEMATIC_PASTE_LOCATION);

        loadSettings(config, plugin);
        loadLootScan(config, plugin);
        loadTeams(config, plugin);
    }

    private static void loadSettings(FileConfiguration config, Plugin plugin) {
        ConfigurationSection s = config.getConfigurationSection("settings");
        if (s == null) {
            plugin.getLogger().warning("squabble.yml is missing a 'settings' section, using built-in defaults.");
            return;
        }

        MAX_SURVIVAL_REWARD_DEATHS = s.getInt("max-survival-reward-deaths", MAX_SURVIVAL_REWARD_DEATHS);
        DAMAGE_POOL_TOTAL = s.getInt("damage-pool-total", DAMAGE_POOL_TOTAL);
        TOTAL_ROUNDS = s.getInt("total-rounds", TOTAL_ROUNDS);

        STARTING_BORDER_SIZE = s.getDouble("starting-border-size", STARTING_BORDER_SIZE);
        STARTING_SKY_HEIGHT = s.getDouble("starting-sky-height", STARTING_SKY_HEIGHT);
        MIN_SKY_HEIGHT = s.getDouble("min-sky-height", MIN_SKY_HEIGHT);
        FALL_ELIMINATION_MIN_Y = s.getDouble("fall-elimination-min-y", FALL_ELIMINATION_MIN_Y);

        ROUND_DURATION_SECONDS = s.getInt("round-duration-seconds", ROUND_DURATION_SECONDS);
        HOW_TO_PLAY_SECONDS = s.getInt("how-to-play-seconds", HOW_TO_PLAY_SECONDS);
        STARTING_IN_SECONDS = s.getInt("starting-in-seconds", STARTING_IN_SECONDS);
        FINAL_COUNTDOWN_SECONDS = s.getInt("final-countdown-seconds", FINAL_COUNTDOWN_SECONDS);
        NEXT_ROUND_SECONDS = s.getInt("next-round-seconds", NEXT_ROUND_SECONDS);

        OVERTIME_INTERVAL_SECONDS = s.getInt("overtime-interval-seconds", OVERTIME_INTERVAL_SECONDS);
        OVERTIME_WARNING_LEAD_SECONDS = s.getInt("overtime-warning-lead-seconds", OVERTIME_WARNING_LEAD_SECONDS);
        OVERTIME_HEALTH_DRAIN = s.getDouble("overtime-health-drain", OVERTIME_HEALTH_DRAIN);
        OVERTIME_MIN_HEALTH = s.getDouble("overtime-min-health", OVERTIME_MIN_HEALTH);
        OVERTIME_BORDER_SHRINK_AMOUNT = s.getDouble("overtime-border-shrink-amount", OVERTIME_BORDER_SHRINK_AMOUNT);
        OVERTIME_BORDER_TRANSITION_SECONDS = s.getInt("overtime-border-transition-seconds", OVERTIME_BORDER_TRANSITION_SECONDS);
        OVERTIME_BORDER_MINIMUM = s.getDouble("overtime-border-minimum", OVERTIME_BORDER_MINIMUM);
        OVERTIME_SKY_SHRINK_AMOUNT = s.getDouble("overtime-sky-shrink-amount", OVERTIME_SKY_SHRINK_AMOUNT);
    }

    private static void loadLootScan(FileConfiguration config, Plugin plugin) {
        ConfigurationSection s = config.getConfigurationSection("loot-scan");
        if (s == null) {
            plugin.getLogger().warning("squabble.yml is missing a 'loot-scan' section, using built-in defaults.");
            return;
        }

        LOOT_SCAN_CENTER_X = s.getDouble("center-x", LOOT_SCAN_CENTER_X);
        LOOT_SCAN_CENTER_Z = s.getDouble("center-z", LOOT_SCAN_CENTER_Z);
        LOOT_SCAN_RADIUS = s.getDouble("radius", LOOT_SCAN_RADIUS);
        LOOT_SCAN_MIN_Y = s.getDouble("min-y", LOOT_SCAN_MIN_Y);
        LOOT_SCAN_MAX_Y = s.getDouble("max-y", LOOT_SCAN_MAX_Y);
    }

    private static void loadTeams(FileConfiguration config, Plugin plugin) {
        TEAMS.clear();
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection == null) {
            plugin.getLogger().severe("squabble.yml is missing a 'teams' section, no teams will be loaded.");
            return;
        }

        for (String id : teamsSection.getKeys(false)) {
            ConfigurationSection t = teamsSection.getConfigurationSection(id);
            if (t == null) continue;

            Vector spawnMin = readVector(t.getConfigurationSection("spawn-min"), null);
            Vector spawnMax = readVector(t.getConfigurationSection("spawn-max"), null);
            Vector barrierMin = readVector(t.getConfigurationSection("barrier-min"), null);
            Vector barrierMax = readVector(t.getConfigurationSection("barrier-max"), null);

            if (spawnMin == null || spawnMax == null || barrierMin == null || barrierMax == null) {
                plugin.getLogger().severe("squabble.yml: team '" + id + "' is missing spawn/barrier coordinates, skipping.");
                continue;
            }

            String blockName = t.getString("spawn-block", "WHITE_WOOL");
            Material spawnBlockType;
            try {
                spawnBlockType = Material.valueOf(blockName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("squabble.yml: team '" + id + "' has invalid spawn-block '" + blockName + "', defaulting to WHITE_WOOL.");
                spawnBlockType = Material.WHITE_WOOL;
            }

            Color armorColor = readColor(t.getString("armor-color"), plugin, id);

            String key = id.toLowerCase();
            TEAMS.put(key, new TeamData(key, spawnMin, spawnMax, barrierMin, barrierMax, spawnBlockType, armorColor));
        }
    }

    private static Color readColor(String hex, Plugin plugin, String teamId) {
        if (hex == null) {
            plugin.getLogger().warning("squabble.yml: team '" + teamId + "' is missing 'armor-color', defaulting to white.");
            return Color.WHITE;
        }
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            int rgb = Integer.parseInt(cleaned, 16);
            return Color.fromRGB(rgb);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("squabble.yml: team '" + teamId + "' has invalid armor-color '" + hex + "', defaulting to white.");
            return Color.WHITE;
        }
    }

    private static Vector readVector(ConfigurationSection section, Vector fallback) {
        if (section == null) return fallback;
        double defX = fallback != null ? fallback.getX() : 0;
        double defY = fallback != null ? fallback.getY() : 0;
        double defZ = fallback != null ? fallback.getZ() : 0;
        return new Vector(
                section.getDouble("x", defX),
                section.getDouble("y", defY),
                section.getDouble("z", defZ));
    }
}