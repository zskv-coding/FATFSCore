package com.zskv.fATFSCore.scores;

import com.zskv.fATFSCore.FATFSCore;
import com.zskv.fATFSCore.teams.Team;
import org.bukkit.Bukkit;

public class ScoreManager {
    private final FATFSCore plugin;

    public ScoreManager(FATFSCore plugin) {
        this.plugin = plugin;
    }

    public int getScore(Team team) {
        return team.getScore();
    }

    public void setScore(Team team, int score) {
        team.setScore(score);
        updateScoreboard();
    }

    public void addScore(Team team, int amount) {
        team.addScore(amount);
        updateScoreboard();
    }

    public void resetScores() {
        for (Team team : plugin.getTeamManager().getTeams()) {
            team.setScore(0);
        }
        updateScoreboard();
    }

    private void updateScoreboard() {
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateAll();
        }
    }
}
