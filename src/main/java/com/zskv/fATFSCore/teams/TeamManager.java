package com.zskv.fATFSCore.teams;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class TeamManager {
    private final FATFSCore plugin;
    private final Map<String, Team> teams;
    private final Map<UUID, Team> playerTeams;
    private final Scoreboard scoreboard;

    public TeamManager(FATFSCore plugin) {
        this.plugin = plugin;
        this.teams = new HashMap<>();
        this.playerTeams = new HashMap<>();
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void registerTeam(Team team) {
        teams.put(team.getId().toLowerCase(), team);

        org.bukkit.scoreboard.Team vTeam = scoreboard.getTeam(team.getId());
        if (vTeam == null) {
            vTeam = scoreboard.registerNewTeam(team.getId());
        }
        vTeam.setDisplayName(team.getName());
        vTeam.setColor(team.getColor());
        vTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);

        // Update all online player scoreboards
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateAllTeams();
            plugin.getScoreboardManager().updateAll();
        }
    }

    public void unregisterTeam(String id) {
        Team team = teams.remove(id.toLowerCase());
        if (team != null) {
            // Remove players from the team first
            List<UUID> members = new ArrayList<>(team.getMembers());
            for (UUID uuid : members) {
                removePlayerFromTeam(uuid);
            }

            org.bukkit.scoreboard.Team vTeam = scoreboard.getTeam(id);
            if (vTeam != null) {
                vTeam.unregister();
            }

            // Update all online player scoreboards
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updateAllTeams();
                plugin.getScoreboardManager().updateAll();
            }
        }
    }

    public Team getTeam(String id) {
        return teams.get(id.toLowerCase());
    }

    public Team getPlayerTeam(UUID uuid) {
        return playerTeams.get(uuid);
    }

    public void addPlayerToTeam(UUID uuid, Team team) {
        Team currentTeam = playerTeams.get(uuid);
        if (currentTeam != null) {
            currentTeam.removeMember(uuid);
            org.bukkit.scoreboard.Team vOldTeam = scoreboard.getTeam(currentTeam.getId());
            if (vOldTeam != null) {
                vOldTeam.removeEntry(Bukkit.getOfflinePlayer(uuid).getName());
            }
        }
        
        team.addMember(uuid);
        playerTeams.put(uuid, team);
        
        org.bukkit.scoreboard.Team vNewTeam = scoreboard.getTeam(team.getId());
        if (vNewTeam != null) {
            vNewTeam.addEntry(Bukkit.getOfflinePlayer(uuid).getName());
        }

        // Sync to individual scoreboards
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updatePlayerTeam(player, team);
        }
    }

    public void removePlayerFromTeam(UUID uuid) {
        Team team = playerTeams.remove(uuid);
        if (team != null) {
            team.removeMember(uuid);
            org.bukkit.scoreboard.Team vTeam = scoreboard.getTeam(team.getId());
            if (vTeam != null) {
                vTeam.removeEntry(Bukkit.getOfflinePlayer(uuid).getName());
            }

            // Sync to individual scoreboards
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updatePlayerTeam(player, null);
            }
        }
    }

    public Collection<Team> getTeams() {
        return teams.values();
    }

    public void shutdown() {
        for (String teamId : teams.keySet()) {
            org.bukkit.scoreboard.Team vTeam = scoreboard.getTeam(teamId);
            if (vTeam != null) {
                vTeam.unregister();
            }
        }
        teams.clear();
        playerTeams.clear();
    }

    public void loadData() {
        playerTeams.clear();
        for (Team team : teams.values()) {
            team.getMembers().clear();
            
            // Load scores
            int score = plugin.getDataConfig().getInt("scores." + team.getId(), 0);
            team.setScore(score);

            // Load members
            List<String> memberList = plugin.getDataConfig().getStringList("members." + team.getId());
            for (String uuidStr : memberList) {
                UUID uuid = UUID.fromString(uuidStr);
                team.addMember(uuid);
                playerTeams.put(uuid, team);
            }
        }
    }

    public void saveData() {
        for (Team team : teams.values()) {
            // Save scores
            plugin.getDataConfig().set("scores." + team.getId(), team.getScore());

            // Save members
            List<String> memberList = new ArrayList<>();
            for (UUID uuid : team.getMembers()) {
                memberList.add(uuid.toString());
            }
            plugin.getDataConfig().set("members." + team.getId(), memberList);
        }
    }
}
