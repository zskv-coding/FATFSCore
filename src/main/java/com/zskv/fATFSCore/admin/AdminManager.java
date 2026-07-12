package com.zskv.fATFSCore.admin;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class AdminManager {
    private final FATFSCore plugin;
    private final Set<UUID> admins = new HashSet<>();
    private final Set<UUID> devs = new HashSet<>();

    public AdminManager(FATFSCore plugin) {
        this.plugin = plugin;
    }

    public void addAdmin(OfflinePlayer player) {
        if (admins.add(player.getUniqueId())) {
            plugin.getTeamManager().removePlayerFromTeam(player.getUniqueId());
            if (player.isOnline() && player.getPlayer() != null) {
                player.getPlayer().setGameMode(GameMode.SPECTATOR);
                player.getPlayer().sendMessage(ChatColor.DARK_RED + "You are now in Admin Mode.");
                updateVisuals(player.getPlayer());
            }
        }
    }

    public void removeAdmin(OfflinePlayer player) {
        if (admins.remove(player.getUniqueId())) {
            if (player.isOnline() && player.getPlayer() != null) {
                player.getPlayer().sendMessage(ChatColor.YELLOW + "You are no longer in Admin Mode.");
                updateVisuals(player.getPlayer());
            }
        }
    }

    public void addDev(OfflinePlayer player) {
        if (devs.add(player.getUniqueId())) {
            plugin.getTeamManager().removePlayerFromTeam(player.getUniqueId());
            if (player.isOnline() && player.getPlayer() != null) {
                player.getPlayer().setGameMode(GameMode.SPECTATOR);
                player.getPlayer().sendMessage(ChatColor.DARK_AQUA + "You are now in Dev Mode.");
                updateVisuals(player.getPlayer());
            }
        }
    }

    public void removeDev(OfflinePlayer player) {
        if (devs.remove(player.getUniqueId())) {
            if (player.isOnline() && player.getPlayer() != null) {
                player.getPlayer().sendMessage(ChatColor.YELLOW + "You are no longer in Dev Mode.");
                updateVisuals(player.getPlayer());
            }
        }
    }

    public void loadData() {
        admins.clear();
        devs.clear();
        
        List<String> adminList = plugin.getDataConfig().getStringList("staff.admins");
        for (String uuidStr : adminList) {
            admins.add(UUID.fromString(uuidStr));
        }

        List<String> devList = plugin.getDataConfig().getStringList("staff.devs");
        for (String uuidStr : devList) {
            devs.add(UUID.fromString(uuidStr));
        }
    }

    public void saveData() {
        List<String> adminList = new ArrayList<>();
        for (UUID uuid : admins) {
            adminList.add(uuid.toString());
        }
        plugin.getDataConfig().set("staff.admins", adminList);

        List<String> devList = new ArrayList<>();
        for (UUID uuid : devs) {
            devList.add(uuid.toString());
        }
        plugin.getDataConfig().set("staff.devs", devList);
    }

    public boolean isAdmin(UUID uuid) {
        return admins.contains(uuid);
    }

    public boolean isDev(UUID uuid) {
        return devs.contains(uuid);
    }

    public Set<UUID> getAdmins() {
        return new HashSet<>(admins);
    }

    public Set<UUID> getDevs() {
        return new HashSet<>(devs);
    }

    private void updateVisuals(Player player) {
        if (isAdmin(player.getUniqueId())) {
            player.setPlayerListName(ChatColor.DARK_RED + "[Admin] " + ChatColor.RESET + player.getName());
        } else if (isDev(player.getUniqueId())) {
            player.setPlayerListName(ChatColor.DARK_AQUA + "[Dev] " + ChatColor.RESET + player.getName());
        } else {
            com.zskv.fATFSCore.teams.Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team != null) {
                player.setPlayerListName(team.getColor() + player.getName());
            } else {
                player.setPlayerListName(player.getName());
            }
        }

        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().syncStaffVisuals(player);
        }
    }
}
