package com.zskv.fATFSCore.minigames.squabble;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Iterator;

public class SquabbleListener implements Listener {
    private final FATFSCore plugin;
    private final SquabbleManager manager;

    public SquabbleListener(FATFSCore plugin, SquabbleManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isInSquabbleWorld(event.getBlock().getWorld())) return;
        if (!manager.isActive()) return;

        // Prevent breaking during countdown phases
        if (!manager.isGameStarted()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot break blocks during the countdown!");
            return;
        }

        // Track the block change for restoration
        manager.trackBlockChange(event.getBlock().getLocation(), event.getBlock().getState());

        Material type = event.getBlock().getType();
        if (type == Material.IRON_BLOCK || type == Material.GOLD_BLOCK || 
            type == Material.DIAMOND_BLOCK || type == Material.CRAFTING_TABLE) {
            return;
        }
        
        event.setDropItems(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isInSquabbleWorld(event.getBlock().getWorld())) return;
        if (!manager.isActive()) return;

        // Prevent placing during countdown phases
        if (!manager.isGameStarted()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place blocks during the countdown!");
            return;
        }

        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        
        // Track placement for restoration
        manager.trackBlockChange(event.getBlock().getLocation(), event.getBlockReplacedState());

        // Unlimited blocks logic
        if (isTeamBlock(type)) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            
            if (mainHand.getType() == type) mainHand.setAmount(64);
            if (offHand.getType() == type) offHand.setAmount(64);
        }

        if (type == Material.TNT) {
            event.getBlock().setType(Material.AIR);
            event.getBlock().getWorld().spawn(event.getBlock().getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
        }
    }

    private boolean isTeamBlock(Material type) {
        return type.name().endsWith("_WOOL") || type.name().endsWith("_CONCRETE");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!isInSquabbleWorld(event.getLocation().getWorld())) return;
        if (!manager.isActive()) return;

        // Explosions should only modify blocks if the game has started
        if (!manager.isGameStarted()) {
            event.setCancelled(true);
            return;
        }

        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            
            // Track for restoration before change
            manager.trackBlockChange(block.getLocation(), block.getState());

            Material type = block.getType();
            if (!(type == Material.IRON_BLOCK || type == Material.GOLD_BLOCK || 
                  type == Material.DIAMOND_BLOCK || type == Material.CRAFTING_TABLE)) {
                block.setType(Material.AIR);
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!manager.isActive()) return;
        // Damage should only be applied if the game has started
        if (!manager.isGameStarted()) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null || attacker.equals(victim)) return;
        
        // Don't track damage from staff
        if (plugin.getAdminManager().isAdmin(attacker.getUniqueId()) || plugin.getAdminManager().isDev(attacker.getUniqueId())) return;

        double damage = Math.min(event.getFinalDamage(), victim.getHealth());
        manager.handleDamage(attacker, victim, damage);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isInSquabbleWorld(player.getWorld())) return;
        if (!manager.isActive()) return;

        // Player deaths should only be handled if the game has started
        if (!manager.isGameStarted()) {
            player.setHealth(20);
            player.teleport(player.getWorld().getSpawnLocation());
            event.setDeathMessage(null);
            return;
        }

        manager.handlePlayerDeath(player);
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (!manager.isActive()) return;
        // Crafting should only be allowed if the game has started
        if (!manager.isGameStarted()) {
            event.getInventory().setResult(null);
            return;
        }
        
        ItemStack result = event.getInventory().getResult();
        if (result != null && result.getType() == Material.SHIELD) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isInSquabbleWorld(event.getPlayer().getWorld())) return;
        if (!manager.isActive()) return;
        
        if (!manager.isGameStarted()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!isInSquabbleWorld(event.getEntity().getWorld())) return;
        if (!manager.isActive()) return;
        
        if (!manager.isGameStarted()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleLogout(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.handleLogin(event.getPlayer());
    }

    private boolean isInSquabbleWorld(org.bukkit.World world) {
        return world != null && world.getName().equals(SquabbleMap.WORLD_NAME);
    }
}
