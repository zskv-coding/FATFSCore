package com.zskv.fATFSCore.minigames.squabble;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SquabbleMap {
    public static final String WORLD_NAME = "Squabble";
    public static final Vector SCHEMATIC_PASTE_LOCATION = new Vector(0.5, 91, 0.5);

    public static class TeamData {
        public final String id;
        public final Vector spawnMin;
        public final Vector spawnMax;
        public final Vector barrierMin;
        public final Vector barrierMax;
        public final Material spawnBlockType;

        public TeamData(String id, Vector spawnMin, Vector spawnMax, Vector barrierMin, Vector barrierMax, Material spawnBlockType) {
            this.id = id;
            this.spawnMin = spawnMin;
            this.spawnMax = spawnMax;
            this.barrierMin = barrierMin;
            this.barrierMax = barrierMax;
            this.spawnBlockType = spawnBlockType;
        }
    }

    public static final Map<String, TeamData> TEAMS = new HashMap<>();

    static {
        // Red
        TEAMS.put("red", new TeamData("red",
                new Vector(31, 77, 10), new Vector(33, 77, 12),
                new Vector(30, 78, 9), new Vector(34, 80, 13),
                Material.RED_WOOL));
        // Orange
        TEAMS.put("orange", new TeamData("orange",
                new Vector(31, 77, -12), new Vector(33, 77, -10),
                new Vector(30, 78, -13), new Vector(34, 80, -9),
                Material.ORANGE_WOOL));
        // Yellow
        TEAMS.put("yellow", new TeamData("yellow",
                new Vector(10, 77, -33), new Vector(12, 77, -31),
                new Vector(9, 78, -34), new Vector(13, 80, -30),
                Material.YELLOW_WOOL));
        // Green (Lime)
        TEAMS.put("green", new TeamData("green",
                new Vector(-12, 77, -33), new Vector(-10, 77, -31),
                new Vector(-13, 78, -34), new Vector(-9, 80, -30),
                Material.LIME_WOOL));
        // Aqua (Light Blue)
        TEAMS.put("aqua", new TeamData("aqua",
                new Vector(-33, 77, -12), new Vector(-31, 77, -10),
                new Vector(-34, 78, -13), new Vector(-30, 80, -9),
                Material.LIGHT_BLUE_WOOL));
        // Blue
        TEAMS.put("blue", new TeamData("blue",
                new Vector(-33, 77, 10), new Vector(-31, 77, 12),
                new Vector(-34, 78, 9), new Vector(-30, 80, 13),
                Material.BLUE_WOOL));
        // Purple
        TEAMS.put("purple", new TeamData("purple",
                new Vector(-12, 77, 31), new Vector(-10, 77, 33),
                new Vector(-13, 78, 30), new Vector(-9, 80, 34),
                Material.PURPLE_WOOL));
        // Pink
        TEAMS.put("pink", new TeamData("pink",
                new Vector(10, 77, 31), new Vector(12, 77, 33),
                new Vector(9, 78, 30), new Vector(13, 80, 34),
                Material.PINK_WOOL));
    }

    public static class LootLocation {
        public final Vector position;
        public final String type; // "lime_1", "lime_2", "light_blue_team", "yellow_1", "yellow_2", "red_middle", "light_blue_middle"

        public LootLocation(Vector position, String type) {
            this.position = position;
            this.type = type;
        }
    }

    public static final List<LootLocation> LOOT_LOCATIONS = new ArrayList<>();

    static {
        // Team Island Loot
        LOOT_LOCATIONS.add(new LootLocation(new Vector(30, 78, 11), "lime_1")); // Red
        LOOT_LOCATIONS.add(new LootLocation(new Vector(32, 78, 13), "lime_2")); // Red
        LOOT_LOCATIONS.add(new LootLocation(new Vector(32, 88, 11), "light_blue_team")); // Red

        LOOT_LOCATIONS.add(new LootLocation(new Vector(30, 78, -11), "lime_1")); // Orange
        LOOT_LOCATIONS.add(new LootLocation(new Vector(32, 78, -13), "lime_2")); // Orange
        LOOT_LOCATIONS.add(new LootLocation(new Vector(32, 88, -11), "light_blue_team")); // Orange

        LOOT_LOCATIONS.add(new LootLocation(new Vector(11, 78, -30), "lime_1")); // Yellow
        LOOT_LOCATIONS.add(new LootLocation(new Vector(13, 78, -32), "lime_2")); // Yellow
        LOOT_LOCATIONS.add(new LootLocation(new Vector(11, 88, -32), "light_blue_team")); // Yellow

        LOOT_LOCATIONS.add(new LootLocation(new Vector(-11, 78, -30), "lime_1")); // Green
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-13, 78, -32), "lime_2")); // Green
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-11, 88, -32), "light_blue_team")); // Green

        LOOT_LOCATIONS.add(new LootLocation(new Vector(-30, 78, -11), "lime_1")); // Aqua
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-32, 78, -13), "lime_2")); // Aqua
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-32, 88, -11), "light_blue_team")); // Aqua

        LOOT_LOCATIONS.add(new LootLocation(new Vector(-30, 78, 11), "lime_1")); // Blue
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-32, 78, 13), "lime_2")); // Blue
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-32, 88, 11), "light_blue_team")); // Blue

        LOOT_LOCATIONS.add(new LootLocation(new Vector(-11, 78, 30), "lime_1")); // Purple
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-13, 78, 32), "lime_2")); // Purple
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-11, 88, 32), "light_blue_team")); // Purple

        LOOT_LOCATIONS.add(new LootLocation(new Vector(11, 78, 30), "lime_1")); // Pink
        LOOT_LOCATIONS.add(new LootLocation(new Vector(13, 78, 32), "lime_2")); // Pink
        LOOT_LOCATIONS.add(new LootLocation(new Vector(11, 88, 32), "light_blue_team")); // Pink

        // Side Island
        LOOT_LOCATIONS.add(new LootLocation(new Vector(29, 77, -2), "yellow_1"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(29, 77, 2), "yellow_2"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-2, 77, -29), "yellow_1"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(2, 77, -29), "yellow_2"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-29, 77, 2), "yellow_1"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-29, 77, -2), "yellow_2"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(2, 77, 29), "yellow_1"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-2, 77, 29), "yellow_2"));

        // Middle Island
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-9, 77, 9), "red_middle"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(9, 77, 9), "red_middle"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(9, 77, -9), "red_middle"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(-9, 77, -9), "red_middle"));
        LOOT_LOCATIONS.add(new LootLocation(new Vector(0, 82, 0), "light_blue_middle"));
    }
}
