package com.zskv.fATFSCore.listeners;

import com.zskv.fATFSCore.FATFSCore;
import com.zskv.fATFSCore.teams.Team;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final FATFSCore plugin;

    public ChatListener(FATFSCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getReadyCheckManager().isActive()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Chat is muted during the ready check.");
            return;
        }

        Player player = event.getPlayer();
        if (plugin.getAdminManager().isAdmin(player.getUniqueId())) {
            event.setFormat(ChatColor.DARK_RED + "[Admin] " + ChatColor.RESET + "%1$s: %2$s");
            return;
        } else if (plugin.getAdminManager().isDev(player.getUniqueId())) {
            event.setFormat(ChatColor.DARK_AQUA + "[Dev] " + ChatColor.RESET + "%1$s: %2$s");
            return;
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (team != null) {
            event.setFormat(team.getColor() + "%1$s" + ChatColor.RESET + ": %2$s");
        } else {
            event.setFormat(ChatColor.WHITE + "%1$s" + ChatColor.RESET + ": %2$s");
        }
    }
}
