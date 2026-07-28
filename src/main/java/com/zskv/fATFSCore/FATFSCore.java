package com.zskv.fATFSCore;

import com.zskv.fATFSCore.commands.EventCommand;
import com.zskv.fATFSCore.commands.ReadyCheckCommand;
import com.zskv.fATFSCore.commands.AdminCommand;
import com.zskv.fATFSCore.commands.DevCommand;
import com.zskv.fATFSCore.listeners.ChatListener;
import com.zskv.fATFSCore.listeners.PlayerListener;
import com.zskv.fATFSCore.minigames.HideSeek.HideSeekListener;
import com.zskv.fATFSCore.minigames.HideSeek.HideSeekManager;
import com.zskv.fATFSCore.readycheck.ReadyCheckManager;
import com.zskv.fATFSCore.admin.AdminManager;
import com.zskv.fATFSCore.scoreboard.ScoreboardManager;
import com.zskv.fATFSCore.scores.PlayerScoreManager;
import com.zskv.fATFSCore.scores.ScoreManager;
import com.zskv.fATFSCore.teams.Team;
import com.zskv.fATFSCore.teams.TeamManager;
import com.zskv.fATFSCore.bossbar.BossBarManager;
import com.zskv.fATFSCore.minigames.Squabble.SquabbleListener;
import com.zskv.fATFSCore.minigames.Squabble.SquabbleManager;
import com.zskv.fATFSCore.minigames.Squabble.SquabbleMap;
import com.zskv.fATFSCore.timer.TimerManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class FATFSCore extends JavaPlugin {

    private TeamManager teamManager;
    private ScoreboardManager scoreboardManager;
    private ScoreManager scoreManager;
    private PlayerScoreManager playerScoreManager;
    private ReadyCheckManager readyCheckManager;
    private AdminManager adminManager;
    private TimerManager timerManager;
    private BossBarManager bossBarManager;
    private SquabbleManager squabbleManager;
    private HideSeekManager hideSeekManager;

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createDataConfig();
        SquabbleMap.load(this);

        this.teamManager = new TeamManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.scoreManager = new ScoreManager(this);
        this.playerScoreManager = new PlayerScoreManager(this);
        this.readyCheckManager = new ReadyCheckManager(this);
        this.adminManager = new AdminManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.timerManager = new TimerManager(this);
        this.squabbleManager = new SquabbleManager(this);
        this.hideSeekManager = new HideSeekManager(this);

        loadTeams();
        loadData();

        EventCommand eventCommand = new EventCommand(this);
        getCommand("event").setExecutor(eventCommand);
        getCommand("event").setTabCompleter(eventCommand);

        ReadyCheckCommand readyCheckCommand = new ReadyCheckCommand(this);
        getCommand("readycheck").setExecutor(readyCheckCommand);
        getCommand("readycheck").setTabCompleter(readyCheckCommand);

        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("admin").setExecutor(adminCommand);
        getCommand("admin").setTabCompleter(adminCommand);

        DevCommand devCommand = new DevCommand(this);
        getCommand("dev").setExecutor(devCommand);
        getCommand("dev").setTabCompleter(devCommand);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new SquabbleListener(this, squabbleManager), this);
        getServer().getPluginManager().registerEvents(new HideSeekListener(hideSeekManager), this);

        // Plugin startup logic
    }

    private void loadTeams() {
        ConfigurationSection teamsSection = getConfig().getConfigurationSection("teams");
        if (teamsSection == null) return;

        for (String key : teamsSection.getKeys(false)) {
            String name = teamsSection.getString(key + ".name");
            String colorStr = teamsSection.getString(key + ".color");
            ChatColor color;
            try {
                color = ChatColor.valueOf(colorStr);
            } catch (IllegalArgumentException e) {
                color = ChatColor.WHITE;
                getLogger().warning("Invalid color for team " + key + ": " + colorStr);
            }

            Team team = new Team(key, name, color);
            teamManager.registerTeam(team);
        }
        getLogger().info("Loaded " + teamManager.getTeams().size() + " teams.");
    }

    public void addTeam(String id, String name, ChatColor color) {
        getConfig().set("teams." + id.toLowerCase() + ".name", name);
        getConfig().set("teams." + id.toLowerCase() + ".color", color.name());
        saveConfig();

        Team team = new Team(id.toLowerCase(), name, color);
        teamManager.registerTeam(team);
    }

    public void deleteTeam(String id) {
        getConfig().set("teams." + id.toLowerCase(), null);
        saveConfig();

        teamManager.unregisterTeam(id);
        if (scoreboardManager != null) {
            scoreboardManager.updateAll();
        }
    }

    @Override
    public void onDisable() {
        saveData();
        if (bossBarManager != null) {
            bossBarManager.clearAll();
        }
        if (squabbleManager != null) {
            squabbleManager.stopGame(true);
        }

        if (teamManager != null) {
            teamManager.shutdown();
        }
        // Plugin shutdown logic
    }

    private void createDataConfig() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    public void saveDataConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        if (teamManager != null) teamManager.loadData();
        if (playerScoreManager != null) playerScoreManager.loadData();
        if (adminManager != null) adminManager.loadData();
    }

    private void saveData() {
        if (teamManager != null) teamManager.saveData();
        if (playerScoreManager != null) playerScoreManager.saveData();
        if (adminManager != null) adminManager.saveData();
        saveDataConfig();
    }

    public void reloadPlugin() {
        reloadConfig();
        SquabbleMap.load(this);
        if (teamManager != null) {
            teamManager.shutdown();
        }
        this.teamManager = new TeamManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.scoreManager = new ScoreManager(this);
        this.readyCheckManager = new ReadyCheckManager(this);
        this.adminManager = new AdminManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.timerManager = new TimerManager(this);
        loadTeams();

        scoreboardManager.updateAll();
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public PlayerScoreManager getPlayerScoreManager() {
        return playerScoreManager;
    }

    public ReadyCheckManager getReadyCheckManager() {
        return readyCheckManager;
    }

    public AdminManager getAdminManager() {
        return adminManager;
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public SquabbleManager getSquabbleManager() {
        return squabbleManager;
    }

    public HideSeekManager getHideSeekManager() {
        return hideSeekManager;
    }
}