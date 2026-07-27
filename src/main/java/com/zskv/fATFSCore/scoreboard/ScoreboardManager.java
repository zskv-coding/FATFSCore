package com.zskv.fATFSCore.scoreboard;

import com.zskv.fATFSCore.FATFSCore;
import com.zskv.fATFSCore.teams.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScoreboardManager {
    private final FATFSCore plugin;

    public ScoreboardManager(FATFSCore plugin) {
        this.plugin = plugin;
    }

    public void setScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        setupTeams(scoreboard);
        setupStaffTeams(scoreboard);
        
        Objective objective = scoreboard.registerNewObjective("scores", "dummy", ChatColor.GOLD + "Fast and the Friendslop");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        updateScoreboard(player, scoreboard);
        player.setScoreboard(scoreboard);
        
        // Sync all players to this new scoreboard
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            syncStaffVisualsForScoreboard(onlinePlayer, scoreboard);
        }
    }

    private void setupStaffTeams(Scoreboard scoreboard) {
        org.bukkit.scoreboard.Team adminTeam = scoreboard.getTeam("staff_admin");
        if (adminTeam == null) {
            adminTeam = scoreboard.registerNewTeam("staff_admin");
        }
        adminTeam.setPrefix(ChatColor.DARK_RED + "[Admin] " + ChatColor.RESET);
        adminTeam.setColor(ChatColor.DARK_RED);

        org.bukkit.scoreboard.Team devTeam = scoreboard.getTeam("staff_dev");
        if (devTeam == null) {
            devTeam = scoreboard.registerNewTeam("staff_dev");
        }
        devTeam.setPrefix(ChatColor.DARK_AQUA + "[Dev] " + ChatColor.RESET);
        devTeam.setColor(ChatColor.DARK_AQUA);
    }

    private void setupTeams(Scoreboard scoreboard) {
        for (Team team : plugin.getTeamManager().getTeams()) {
            org.bukkit.scoreboard.Team vTeam = scoreboard.getTeam(team.getId());
            if (vTeam == null) {
                vTeam = scoreboard.registerNewTeam(team.getId());
            }
            vTeam.setColor(team.getColor());
            vTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        }
    }

    public void syncStaffVisuals(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            syncStaffVisualsForScoreboard(player, onlinePlayer.getScoreboard());
        }
    }

    private void syncStaffVisualsForScoreboard(Player target, Scoreboard sb) {
        // Remove from all teams first
        for (org.bukkit.scoreboard.Team t : sb.getTeams()) {
            t.removeEntry(target.getName());
        }

        if (plugin.getAdminManager().isAdmin(target.getUniqueId())) {
            org.bukkit.scoreboard.Team adminTeam = sb.getTeam("staff_admin");
            if (adminTeam != null) adminTeam.addEntry(target.getName());
        } else if (plugin.getAdminManager().isDev(target.getUniqueId())) {
            org.bukkit.scoreboard.Team devTeam = sb.getTeam("staff_dev");
            if (devTeam != null) devTeam.addEntry(target.getName());
        } else {
            Team team = plugin.getTeamManager().getPlayerTeam(target.getUniqueId());
            if (team != null) {
                org.bukkit.scoreboard.Team vTeam = sb.getTeam(team.getId());
                if (vTeam != null) vTeam.addEntry(target.getName());
            }
        }
    }

    public void updatePlayerTeam(Player player, Team team) {
        syncStaffVisuals(player);
    }

    public void updateAllTeams() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = player.getScoreboard();
            setupTeams(sb);
            setupStaffTeams(sb);
            // Re-sync all players to be sure
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                syncStaffVisualsForScoreboard(onlinePlayer, sb);
            }
        }
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player, player.getScoreboard());
        }
    }

    public void updateScoreboard(Player player, Scoreboard scoreboard) {
        Objective objective = scoreboard.getObjective("scores");
        if (objective == null) return;

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        List<Team> teams = new ArrayList<>(plugin.getTeamManager().getTeams());

        teams.sort((t1, t2) -> {
            if (t1.getScore() != t2.getScore()) {
                return Integer.compare(t2.getScore(), t1.getScore());
            }
            return Integer.compare(getRainbowOrder(t1), getRainbowOrder(t2));
        });

        int score = teams.size() + 5;
        
        // Timer
        if (plugin.getTimerManager().isActive()) {
            String timerLabel = ChatColor.GOLD + plugin.getTimerManager().getLabel() + ":";
            String timerTime = ChatColor.YELLOW + plugin.getTimerManager().formatTime(plugin.getTimerManager().getTimeRemaining());
            if (plugin.getTimerManager().isPaused()) {
                timerTime += ChatColor.RED + " (P)";
            }
            objective.getScore(timerLabel).setScore(score--);
            objective.getScore(timerTime).setScore(score--);
            objective.getScore("   ").setScore(score--);
        }

        objective.getScore(" ").setScore(score--);

        for (Team team : teams) {
            String entry = team.getColor() + team.getName() + ChatColor.WHITE + ": " + ChatColor.YELLOW + team.getScore();
            objective.getScore(entry).setScore(score--);
        }

        // Blank line
        objective.getScore("  ").setScore(score--);

        // Player Coins
        String coinsLabel = ChatColor.GOLD + "Coins: " + ChatColor.YELLOW + plugin.getPlayerScoreManager().getCoins(player.getUniqueId());
        objective.getScore(coinsLabel).setScore(score--);
    }

    private int getRainbowOrder(Team team) {
        return switch (team.getColor()) {
            case RED -> 1;
            case GOLD -> 2;
            case YELLOW -> 3;
            case GREEN -> 4;
            case DARK_GREEN -> 5;
            case AQUA -> 6;
            case DARK_AQUA -> 7;
            case BLUE -> 8;
            case DARK_PURPLE -> 9;
            case LIGHT_PURPLE -> 10;
            default -> 99;
        };
    }
}
