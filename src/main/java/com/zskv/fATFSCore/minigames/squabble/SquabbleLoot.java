package com.zskv.fATFSCore.minigames.squabble;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public class SquabbleLoot {
// to-do: change to .yml file
    public static void fillShulkerBox(Block block, String type) {
        if (!(block.getState() instanceof ShulkerBox shulker)) return;
        Inventory inv = shulker.getInventory();
        inv.clear();

        switch (type) {
            case "lime_1" -> {
                inv.setItem(13, new ItemStack(Material.STONE_SWORD));
                inv.setItem(12, new ItemStack(Material.ARROW, 3));
                inv.setItem(14, new ItemStack(Material.ARROW, 3));
            }
            case "lime_2" -> {
                inv.setItem(13, new ItemStack(Material.APPLE, 2));
                inv.setItem(4, new ItemStack(Material.GOLD_INGOT, 5));
                inv.setItem(12, new ItemStack(Material.GOLD_INGOT, 5));
                inv.setItem(14, new ItemStack(Material.GOLD_INGOT, 5));
                inv.setItem(22, new ItemStack(Material.GOLD_INGOT, 5));
            }
            case "yellow_1" -> {
                inv.setItem(13, new ItemStack(Material.IRON_BOOTS));
                inv.setItem(11, new ItemStack(Material.IRON_BOOTS));
                inv.setItem(12, new ItemStack(Material.IRON_BOOTS));
                inv.setItem(14, new ItemStack(Material.IRON_BOOTS));
                inv.setItem(15, new ItemStack(Material.IRON_BOOTS));
            }
            case "yellow_2" -> {
                inv.setItem(13, new ItemStack(Material.ARROW, 3));
                inv.setItem(12, new ItemStack(Material.WOODEN_AXE));
                inv.setItem(14, new ItemStack(Material.WOODEN_AXE));
                inv.setItem(4, new ItemStack(Material.STONE_SWORD));
                inv.setItem(22, new ItemStack(Material.STONE_SWORD));
                inv.setItem(3, new ItemStack(Material.ARROW, 2));
                inv.setItem(5, new ItemStack(Material.ARROW, 2));
                inv.setItem(21, new ItemStack(Material.ARROW, 2));
                inv.setItem(23, new ItemStack(Material.ARROW, 2));
            }
            case "light_blue_team" -> {
                inv.setItem(13, new ItemStack(Material.GOLDEN_APPLE, 2));
            }
            case "light_blue_middle" -> {
                ItemStack trident = new ItemStack(Material.TRIDENT);
                trident.addEnchantment(Enchantment.LOYALTY, 3);
                inv.setItem(13, trident);
                inv.setItem(4, new ItemStack(Material.DIAMOND_HELMET));
                inv.setItem(22, new ItemStack(Material.DIAMOND_BOOTS));
                inv.setItem(12, new ItemStack(Material.IRON_SWORD));
                inv.setItem(14, new ItemStack(Material.IRON_SWORD));
                inv.setItem(3, new ItemStack(Material.GOLDEN_APPLE));
                inv.setItem(5, new ItemStack(Material.GOLDEN_APPLE));
                inv.setItem(21, new ItemStack(Material.GOLDEN_APPLE));
                inv.setItem(23, new ItemStack(Material.GOLDEN_APPLE));
            }
            case "red_middle" -> {
                inv.setItem(13, new ItemStack(Material.IRON_CHESTPLATE));
                inv.setItem(12, new ItemStack(Material.TNT, 2));
                
                ItemStack potion = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) potion.getItemMeta();
                if (meta != null) {
                    meta.setBasePotionType(PotionType.STRONG_HEALING);
                    potion.setItemMeta(meta);
                }
                inv.setItem(14, potion);
            }
        }
    }
}
