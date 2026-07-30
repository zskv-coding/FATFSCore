package com.zskv.fATFSCore.minigames.Squabble;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SquabbleLoot {

    private static final Map<Location, ItemStack[]> LOOT_TEMPLATE = new HashMap<>();
    private static boolean templateCaptured = false;

    public static void restock(World world, Plugin plugin) {
        if (!templateCaptured) {
            if (!loadTemplate(plugin)) {
                captureTemplate(world, plugin);
                saveTemplate(plugin);
            }
        }

        int restocked = 0;
        for (Map.Entry<Location, ItemStack[]> entry : LOOT_TEMPLATE.entrySet()) {
            Location loc = entry.getKey();
            Block block = loc.getBlock();
            if (!(block.getState() instanceof ShulkerBox shulker)) {
                plugin.getLogger().warning("Squabble loot: expected a cyan shulker box at " + formatLoc(loc) + " but found " + block.getType() + ". Skipping.");
                continue;
            }
            shulker.getInventory().clear();
            shulker.getInventory().setContents(cloneContents(entry.getValue()));
            restocked++;
        }

        plugin.getLogger().info("Squabble loot: restocked " + restocked + " shulker box(es) from template.");
    }

    public static void captureTemplate(World world, Plugin plugin) {
        LOOT_TEMPLATE.clear();

        double centerX = SquabbleMap.LOOT_SCAN_CENTER_X;
        double centerZ = SquabbleMap.LOOT_SCAN_CENTER_Z;
        double radius = SquabbleMap.LOOT_SCAN_RADIUS;
        int minX = (int) Math.floor(centerX - radius);
        int maxX = (int) Math.ceil(centerX + radius);
        int minZ = (int) Math.floor(centerZ - radius);
        int maxZ = (int) Math.ceil(centerZ + radius);
        int minY = (int) SquabbleMap.LOOT_SCAN_MIN_Y;
        int maxY = (int) SquabbleMap.LOOT_SCAN_MAX_Y;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.CYAN_SHULKER_BOX) continue;
                    if (!(block.getState() instanceof ShulkerBox shulker)) continue;

                    LOOT_TEMPLATE.put(block.getLocation(), cloneContents(shulker.getInventory().getContents()));
                }
            }
        }

        templateCaptured = true;
        plugin.getLogger().info("Squabble loot: captured template from " + LOOT_TEMPLATE.size() + " cyan shulker box(es).");
    }

    public static void resetTemplate() {
        LOOT_TEMPLATE.clear();
        templateCaptured = false;
    }

    public static void forceRecapture(World world, Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "squabble-loot-template.yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Squabble loot: failed to delete old saved loot template at " + file.getPath());
        }

        LOOT_TEMPLATE.clear();
        templateCaptured = false;

        captureTemplate(world, plugin);
        saveTemplate(plugin);

        plugin.getLogger().info("Squabble loot: discarded previous saved template and captured a fresh loot template with "
                + LOOT_TEMPLATE.size() + " shulker box(es) for this game.");
    }

    private static void saveTemplate(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "squabble-loot-template.yml");
        YamlConfiguration config = new YamlConfiguration();

        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map.Entry<Location, ItemStack[]> e : LOOT_TEMPLATE.entrySet()) {
            Location loc = e.getKey();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("x", loc.getBlockX());
            entry.put("y", loc.getBlockY());
            entry.put("z", loc.getBlockZ());
            entry.put("world", loc.getWorld() != null ? loc.getWorld().getName() : SquabbleMap.WORLD_NAME);
            entry.put("items", Arrays.asList(e.getValue()));
            entries.add(entry);
        }
        config.set("boxes", entries);

        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Squabble loot: failed to save loot template: " + ex.getMessage());
        }
    }

    private static boolean loadTemplate(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "squabble-loot-template.yml");
        if (!file.exists()) return false;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> boxes = config.getList("boxes");
        if (boxes == null || boxes.isEmpty()) return false;

        LOOT_TEMPLATE.clear();
        for (Object obj : boxes) {
            if (!(obj instanceof Map<?, ?> map)) continue;

            Object xObj = map.get("x");
            Object yObj = map.get("y");
            Object zObj = map.get("z");
            if (!(xObj instanceof Number) || !(yObj instanceof Number) || !(zObj instanceof Number)) continue;

            int x = ((Number) xObj).intValue();
            int y = ((Number) yObj).intValue();
            int z = ((Number) zObj).intValue();

            String worldName = map.get("world") instanceof String ? (String) map.get("world") : SquabbleMap.WORLD_NAME;
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Squabble loot: template references world '" + worldName + "' which is not loaded. Skipping entry.");
                continue;
            }

            Object itemsObj = map.get("items");
            if (!(itemsObj instanceof List<?> itemsList)) continue;

            ItemStack[] items = new ItemStack[itemsList.size()];
            for (int i = 0; i < itemsList.size(); i++) {
                Object item = itemsList.get(i);
                items[i] = item instanceof ItemStack ? (ItemStack) item : null;
            }

            LOOT_TEMPLATE.put(new Location(world, x, y, z), items);
        }

        templateCaptured = !LOOT_TEMPLATE.isEmpty();
        if (templateCaptured) {
            plugin.getLogger().info("Squabble loot: loaded template from disk with " + LOOT_TEMPLATE.size() + " shulker box(es).");
        }
        return templateCaptured;
    }

    private static ItemStack[] cloneContents(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }

    private static String formatLoc(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}