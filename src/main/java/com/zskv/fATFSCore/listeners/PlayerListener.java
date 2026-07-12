package com.zskv.fATFSCore.listeners;

import com.zskv.fATFSCore.FATFSCore;
import com.zskv.fATFSCore.teams.Team;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerListener implements Listener {
    private final FATFSCore plugin;

    public PlayerListener(FATFSCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getScoreboardManager().setScoreboard(player);
        plugin.getBossBarManager().handleJoin(player);
        
        // Ensure staff list names are correct on join
        if (plugin.getAdminManager().isAdmin(player.getUniqueId())) {
            player.setPlayerListName(ChatColor.DARK_RED + "[Admin] " + ChatColor.RESET + player.getName());
        } else if (plugin.getAdminManager().isDev(player.getUniqueId())) {
            player.setPlayerListName(ChatColor.DARK_AQUA + "[Dev] " + ChatColor.RESET + player.getName());
        } else {
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team != null) {
                player.setPlayerListName(team.getColor() + player.getName());
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.getScoreboardManager().setScoreboard(event.getPlayer());
    }
}
