package com.zskv.fATFSCore.scores;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerScoreManager {
    private final FATFSCore plugin;
    private final Map<UUID, Integer> playerCoins = new HashMap<>();
    private final Map<UUID, Integer> roundCoins = new HashMap<>();

    public PlayerScoreManager(FATFSCore plugin) {
        this.plugin = plugin;
    }

    public void resetRoundCoins() {
        roundCoins.clear();
    }

    public Map<UUID, Integer> getRoundCoins() {
        return roundCoins;
    }

    public int getCoins(UUID uuid) {
        return playerCoins.getOrDefault(uuid, 0);
    }

    public void addCoins(UUID uuid, int amount) {
        playerCoins.put(uuid, getCoins(uuid) + amount);
        roundCoins.put(uuid, roundCoins.getOrDefault(uuid, 0) + amount);
        
        // Add to team score (1:2 ratio)
        com.zskv.fATFSCore.teams.Team team = plugin.getTeamManager().getPlayerTeam(uuid);
        if (team != null && amount > 0) {
            plugin.getScoreManager().addScore(team, amount / 2);
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && amount > 0) {
            player.sendMessage(ChatColor.GOLD + "+" + amount + " Coins!");
        }
        updateScoreboard();
    }

    public void setCoins(UUID uuid, int amount) {
        playerCoins.put(uuid, amount);
        updateScoreboard();
    }

    public void loadData() {
        playerCoins.clear();
        if (plugin.getDataConfig().contains("player_coins")) {
            for (String key : plugin.getDataConfig().getConfigurationSection("player_coins").getKeys(false)) {
                playerCoins.put(UUID.fromString(key), plugin.getDataConfig().getInt("player_coins." + key));
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Integer> entry : playerCoins.entrySet()) {
            plugin.getDataConfig().set("player_coins." + entry.getKey().toString(), entry.getValue());
        }
    }

    private void updateScoreboard() {
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateAll();
        }
    }
}
