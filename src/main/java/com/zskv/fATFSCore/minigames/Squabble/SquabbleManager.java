package com.zskv.fATFSCore.minigames.Squabble;

import com.zskv.fATFSCore.FATFSCore;
import com.zskv.fATFSCore.teams.Team;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.util.*;

public class SquabbleManager {
    private final FATFSCore plugin;
    private boolean active;
    private boolean gameStarted;
    private int currentRound = 0;
    private final Map<UUID, Team> alivePlayers = new HashMap<>();
    private final Map<UUID, Double> roundDamage = new HashMap<>();
    private final Map<UUID, Integer> roundKills = new HashMap<>();
    private final Map<UUID, Integer> totalKillsAcrossRounds = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, BukkitTask> logoutTasks = new HashMap<>();
    private final Map<UUID, Location> preLogoutLocations = new HashMap<>();
    private final Map<Location, BlockState> changedBlocks = new HashMap<>();
    private int deathsThisRound = 0;
    private BukkitTask fallEliminationTask;
    private BukkitTask actionBarTask;
    private BukkitTask borderShrinkTask;
    private BukkitTask overtimeTask;
    private BukkitTask skyBorderParticleTask;
    private double currentBorderSize = 100;
    private double currentSkyHeight = 146;
    private BossBar nextRoundBossBar;

    private static final Map<Material, Material> MINEABLE_BLOCKS = new EnumMap<>(Material.class);
    static {
        MINEABLE_BLOCKS.put(Material.COAL_ORE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.IRON_ORE, Material.STONE_PICKAXE);
        MINEABLE_BLOCKS.put(Material.GOLD_ORE, Material.IRON_PICKAXE);
        MINEABLE_BLOCKS.put(Material.DIAMOND_ORE, Material.IRON_PICKAXE);
        MINEABLE_BLOCKS.put(Material.EMERALD_ORE, Material.IRON_PICKAXE);
        MINEABLE_BLOCKS.put(Material.LAPIS_ORE, Material.STONE_PICKAXE);
        MINEABLE_BLOCKS.put(Material.REDSTONE_ORE, Material.IRON_PICKAXE);
        MINEABLE_BLOCKS.put(Material.ANCIENT_DEBRIS, Material.DIAMOND_PICKAXE);
        MINEABLE_BLOCKS.put(Material.NETHER_QUARTZ_ORE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.STONE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.COBBLESTONE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.GRANITE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.DIORITE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.ANDESITE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.DEEPSLATE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.TUFF, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.CALCITE, Material.WOODEN_PICKAXE);
        MINEABLE_BLOCKS.put(Material.DRIPSTONE_BLOCK, Material.WOODEN_PICKAXE);
    }

    public SquabbleManager(FATFSCore plugin) {
        this.plugin = plugin;
        this.active = false;
        this.gameStarted = false;
    }

    public void startSequence() {
        if (active) return;
        active = true;
        gameStarted = false;
        currentRound = 1;
        totalKillsAcrossRounds.clear();
        alivePlayers.clear();
        roundDamage.clear();
        roundKills.clear();
        logoutTasks.values().forEach(BukkitTask::cancel);
        logoutTasks.clear();
        preLogoutLocations.clear();
        lastAttacker.clear();
        changedBlocks.clear();
        deathsThisRound = 0;
        currentBorderSize = SquabbleMap.STARTING_BORDER_SIZE;
        currentSkyHeight = SquabbleMap.STARTING_SKY_HEIGHT;

        World world = Bukkit.getWorld(SquabbleMap.WORLD_NAME);
        if (world == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Squabble world not found!");
            active = false;
            return;
        }

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(currentBorderSize);
        border.setDamageBuffer(0);
        border.setDamageAmount(0.5);
        border.setWarningDistance(10);
        border.setWarningTime(15);

        teleportAndKitPlayers(world);
        teleportStaff(world);
        SquabbleLoot.restock(world, plugin);

        for (UUID uuid : alivePlayers.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendTitle(ChatColor.AQUA + "Squabble", ChatColor.WHITE + "Round " + currentRound + "/" + SquabbleMap.TOTAL_ROUNDS, 10, 70, 20);
            }
        }

        if (currentRound == 1) {
            plugin.getTimerManager().startTimer("How to Play", SquabbleMap.HOW_TO_PLAY_SECONDS, () -> {
                startRound();
            });
        } else {
            startRound();
        }
    }

    private void startRound() {
        gameStarted = false;
        restoreMap();
        roundDamage.clear();
        roundKills.clear();
        deathsThisRound = 0;
        plugin.getPlayerScoreManager().resetRoundCoins();

        // Reset player states for the round

        World world = Bukkit.getWorld(SquabbleMap.WORLD_NAME);
        if (world != null && currentRound > 1) {
            alivePlayers.clear();
            preLogoutLocations.clear();
            teleportAndKitPlayers(world);
            teleportStaff(world);
            SquabbleLoot.restock(world, plugin);
        }

        plugin.getTimerManager().startTimer("Starting In", SquabbleMap.STARTING_IN_SECONDS, this::startGame);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (active && !gameStarted) {
                    startCountdown();
                }
            }
        }.runTaskLater(plugin, (SquabbleMap.STARTING_IN_SECONDS - SquabbleMap.FINAL_COUNTDOWN_SECONDS) * 20L);
    }

    private void startCountdown() {
        new BukkitRunnable() {
            int count = SquabbleMap.FINAL_COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (!active || gameStarted) {
                    cancel();
                    return;
                }

                if (count <= 0) {
                    for (UUID uuid : alivePlayers.keySet()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendTitle("", ChatColor.BOLD + "GO", 0, 20, 10);
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                        }
                    }
                    cancel();
                    return;
                }

                ChatColor numColor = ChatColor.WHITE;
                if (count == 3) numColor = ChatColor.GOLD;
                else if (count == 2) numColor = ChatColor.YELLOW;
                else if (count == 1) numColor = ChatColor.RED;

                String title = ChatColor.AQUA + "Starting in";
                String subtitle = ChatColor.GRAY + "» " + numColor.toString() + count + ChatColor.GRAY + " «";

                for (UUID uuid : alivePlayers.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendTitle(title, subtitle, 0, 21, 0);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    }
                }

                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void teleportStaff(World world) {
        Location staffSpawn = new Location(world,
                SquabbleMap.SCHEMATIC_PASTE_LOCATION.getX(),
                SquabbleMap.SCHEMATIC_PASTE_LOCATION.getY(),
                SquabbleMap.SCHEMATIC_PASTE_LOCATION.getZ());

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (plugin.getAdminManager().isAdmin(uuid) || plugin.getAdminManager().isDev(uuid)) {
                player.teleport(staffSpawn);
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    private void teleportAndKitPlayers(World world) {
        for (Team team : plugin.getTeamManager().getTeams()) {
            SquabbleMap.TeamData teamData = SquabbleMap.TEAMS.get(team.getId().toLowerCase());
            if (teamData == null) continue;

            List<UUID> members = new ArrayList<>(team.getMembers());
            List<Location> spawnLocations = getSpawnLocations(world, teamData);

            for (int i = 0; i < members.size(); i++) {
                Player player = Bukkit.getPlayer(members.get(i));
                if (player == null) continue;

                Location spawn;
                if (spawnLocations.isEmpty()) {
                    plugin.getLogger().warning("No valid spawn locations found for team " + team.getId() + " at Y=" + teamData.spawnMin.getY() + ". Using world spawn.");
                    spawn = world.getSpawnLocation();
                } else {
                    spawn = i < spawnLocations.size() ? spawnLocations.get(i) : spawnLocations.get(0);
                }
                player.teleport(spawn);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.getInventory().clear();

                alivePlayers.put(player.getUniqueId(), team);
                giveKit(player, team);
            }
        }
    }

    private List<Location> getSpawnLocations(World world, SquabbleMap.TeamData teamData) {
        List<Location> locations = new ArrayList<>();
        Vector min = teamData.spawnMin;
        Vector max = teamData.spawnMax;
        Material target = teamData.spawnBlockType;

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                for (int y = min.getBlockY() - 1; y <= min.getBlockY() + 1; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == target) {
                        locations.add(block.getLocation().add(0.5, 1.1, 0.5));
                    }
                }
            }
        }

        if (locations.isEmpty()) {
            plugin.getLogger().severe("MAP ERROR: Could not find any " + target + " blocks for team " + teamData.id + " near Y=" + min.getBlockY());
        }

        Collections.shuffle(locations);
        return locations;
    }

    private void giveKit(Player player, Team team) {
        SquabbleMap.TeamData teamData = SquabbleMap.TEAMS.get(team.getId().toLowerCase());
        Color armorColor = teamData != null ? teamData.armorColor : Color.WHITE;

        player.getInventory().setHelmet(createColoredArmor(Material.LEATHER_HELMET, armorColor));
        player.getInventory().setChestplate(createColoredArmor(Material.LEATHER_CHESTPLATE, armorColor));
        player.getInventory().setLeggings(createColoredArmor(Material.LEATHER_LEGGINGS, armorColor));
        player.getInventory().setBoots(createColoredArmor(Material.LEATHER_BOOTS, armorColor));

        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
        player.getInventory().addItem(new ItemStack(Material.BOW));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 3));
        player.getInventory().addItem(new ItemStack(Material.STICK, 8));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 4));
        player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));

        if (teamData != null) {
            player.getInventory().setItemInOffHand(new ItemStack(teamData.spawnBlockType, 64));
        }
    }

    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void startGame() {
        gameStarted = true;
        dropBarriers();
        Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "GAME STARTED!");

        // Start 2-minute round timer
        plugin.getTimerManager().startTimer("Round Ends In", SquabbleMap.ROUND_DURATION_SECONDS, this::startOvertime);

        // Start fall elimination task
        fallEliminationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || !gameStarted) {
                    cancel();
                    return;
                }
                for (UUID uuid : new ArrayList<>(alivePlayers.keySet())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && (player.getLocation().getY() < SquabbleMap.FALL_ELIMINATION_MIN_Y || player.getLocation().getY() > currentSkyHeight)) {
                        handlePlayerDeath(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // Sky Border Particles
        skyBorderParticleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || !gameStarted) {
                    cancel();
                    return;
                }
                World world = Bukkit.getWorld(SquabbleMap.WORLD_NAME);
                if (world == null) return;

                Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.5f);
                for (Player p : world.getPlayers()) {
                    Location loc = p.getLocation();
                    for (double x = -10; x <= 10; x += 2.5) {
                        for (double z = -10; z <= 10; z += 2.5) {
                            world.spawnParticle(Particle.DUST, loc.getX() + x, currentSkyHeight, loc.getZ() + z, 1, 0, 0, 0, 0, dust);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void startOvertime() {
        if (!active || !gameStarted) return;

        Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "OVERTIME: Your health has been drained.");
        // Visual and audio cue for Overtime start

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(ChatColor.RED + "OVERTIME", "", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1f);
        }

        World world = Bukkit.getWorld(SquabbleMap.WORLD_NAME);
        if (world == null) return;

        if (borderShrinkTask != null) {
            borderShrinkTask.cancel();
            borderShrinkTask = null;
        }

        // Start repeating Overtime cycle
        overtimeTask = new BukkitRunnable() {
            int elapsedSeconds = 0;

            @Override
            public void run() {
                if (!active || !gameStarted) {
                    cancel();
                    return;
                }

                if (elapsedSeconds % SquabbleMap.OVERTIME_INTERVAL_SECONDS
                        == (SquabbleMap.OVERTIME_INTERVAL_SECONDS - SquabbleMap.OVERTIME_WARNING_LEAD_SECONDS)) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("", ChatColor.RED + "Heart drain in 5 seconds!", 0, 40, 10);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
                    }
                }

                // Execute collapse effects every 20 seconds
                if (elapsedSeconds > 0 && elapsedSeconds % SquabbleMap.OVERTIME_INTERVAL_SECONDS == 0) {
                    for (UUID uuid : alivePlayers.keySet()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.getHealth() > SquabbleMap.OVERTIME_MIN_HEALTH) {
                            p.setHealth(Math.max(SquabbleMap.OVERTIME_MIN_HEALTH, p.getHealth() - SquabbleMap.OVERTIME_HEALTH_DRAIN));
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1.2f);
                        }
                    }

                    WorldBorder border = world.getWorldBorder();
                    if (currentBorderSize > SquabbleMap.OVERTIME_BORDER_MINIMUM) {
                        currentBorderSize = Math.max(SquabbleMap.OVERTIME_BORDER_MINIMUM, currentBorderSize - SquabbleMap.OVERTIME_BORDER_SHRINK_AMOUNT);
                        border.setSize(currentBorderSize, SquabbleMap.OVERTIME_BORDER_TRANSITION_SECONDS);
                        Bukkit.broadcastMessage(ChatColor.YELLOW + "The border is collapsing to " + (int) currentBorderSize + " blocks!");
                    }

                    if (currentSkyHeight > SquabbleMap.MIN_SKY_HEIGHT) {
                        currentSkyHeight -= SquabbleMap.OVERTIME_SKY_SHRINK_AMOUNT;
                    }
                }

                elapsedSeconds++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void trackBlockChange(Location loc, BlockState oldState) {
        if (!active) return;
        changedBlocks.putIfAbsent(loc, oldState);
    }

    private void restoreMap() {
        World world = Bukkit.getWorld(SquabbleMap.WORLD_NAME);
        if (world != null) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof org.bukkit.entity.Item ||
                        entity instanceof org.bukkit.entity.TNTPrimed ||
                        entity instanceof org.bukkit.entity.Projectile ||
                        entity instanceof org.bukkit.entity.ExperienceOrb) {
                    entity.remove();
                }
            }
        }

        for (Map.Entry<Location, BlockState> entry : changedBlocks.entrySet()) {
            entry.getValue().update(true, false);
        }
        changedBlocks.clear();

        if (world != null) {
            WorldBorder border = world.getWorldBorder();
            currentBorderSize = SquabbleMap.STARTING_BORDER_SIZE;
            currentSkyHeight = SquabbleMap.STARTING_SKY_HEIGHT;
            border.setSize(currentBorderSize);
            border.setCenter(0, 0);
        }
    }

    private void dropBarriers() {
        World world = Bukkit.getWorld(SquabbleMap.WORLD_NAME);
        if (world == null) return;

        for (SquabbleMap.TeamData teamData : SquabbleMap.TEAMS.values()) {
            Vector min = teamData.barrierMin;
            Vector max = teamData.barrierMax;
            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == Material.BARRIER) {
                            trackBlockChange(block.getLocation(), block.getState());
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    public void stopGame(boolean force) {
        if (!active && !force) return;

        restoreMap();

        active = false;
        gameStarted = false;
        currentRound = 0;
        totalKillsAcrossRounds.clear();
        if (plugin.getTimerManager() != null) plugin.getTimerManager().stopTimer();

        if (fallEliminationTask != null) {
            fallEliminationTask.cancel();
            fallEliminationTask = null;
        }
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        if (borderShrinkTask != null) {
            borderShrinkTask.cancel();
            borderShrinkTask = null;
        }
        if (overtimeTask != null) {
            overtimeTask.cancel();
            overtimeTask = null;
        }
        if (skyBorderParticleTask != null) {
            skyBorderParticleTask.cancel();
            skyBorderParticleTask = null;
        }
        if (nextRoundBossBar != null) {
            nextRoundBossBar.removeAll();
            nextRoundBossBar = null;
        }

        World hubWorld = Bukkit.getWorld("hub");
        Location hubSpawn = hubWorld != null ? hubWorld.getSpawnLocation() : null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            boolean isStaff = plugin.getAdminManager().isAdmin(uuid) || plugin.getAdminManager().isDev(uuid);
            Team team = plugin.getTeamManager().getPlayerTeam(uuid);

            if (isStaff || team != null) {
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.getInventory().setItemInOffHand(null);
                player.setGameMode(isStaff ? GameMode.SPECTATOR : GameMode.ADVENTURE);

                if (hubSpawn != null) {
                    player.teleport(hubSpawn);
                }
            }
        }
        alivePlayers.clear();
        logoutTasks.values().forEach(BukkitTask::cancel);
        logoutTasks.clear();
        preLogoutLocations.clear();
        lastAttacker.clear();
    }

    private void endRound() {
        gameStarted = false;
        restoreMap();
        plugin.getTimerManager().stopTimer();

        if (fallEliminationTask != null) {
            fallEliminationTask.cancel();
            fallEliminationTask = null;
        }
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        if (borderShrinkTask != null) {
            borderShrinkTask.cancel();
            borderShrinkTask = null;
        }
        if (overtimeTask != null) {
            overtimeTask.cancel();
            overtimeTask = null;
        }
        if (skyBorderParticleTask != null) {
            skyBorderParticleTask.cancel();
            skyBorderParticleTask = null;
        }
        logoutTasks.values().forEach(BukkitTask::cancel);
        logoutTasks.clear();
        preLogoutLocations.clear();
        lastAttacker.clear();
        // Reset World Border to starting size

        World world = Bukkit.getWorld(SquabbleMap.WORLD_NAME);
        if (world != null) {
            WorldBorder border = world.getWorldBorder();
            currentBorderSize = SquabbleMap.STARTING_BORDER_SIZE;
            border.setSize(currentBorderSize);
            border.setCenter(0, 0);
        }

        // Only set game mode to SPECTATOR for players who were in the game or staff
        for (UUID uuid : alivePlayers.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
        // Also set staff to spectator if they are not already
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if ((plugin.getAdminManager().isAdmin(uuid) || plugin.getAdminManager().isDev(uuid)) && !alivePlayers.containsKey(uuid)) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }

        displayRoundResults();
        // Boss bar countdown for next round
        // Add relevant players to the boss bar

        nextRoundBossBar = Bukkit.createBossBar(ChatColor.GREEN + "Next round starting in " + SquabbleMap.NEXT_ROUND_SECONDS + " seconds", BarColor.GREEN, BarStyle.SEGMENTED_10);

        for (UUID uuid : alivePlayers.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                nextRoundBossBar.addPlayer(player);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if ((plugin.getAdminManager().isAdmin(uuid) || plugin.getAdminManager().isDev(uuid)) && !alivePlayers.containsKey(uuid)) {
                nextRoundBossBar.addPlayer(player);
            }
        }

        new BukkitRunnable() {
            int timeRemaining = SquabbleMap.NEXT_ROUND_SECONDS;

            @Override
            public void run() {
                if (!active) {
                    nextRoundBossBar.removeAll();
                    cancel();
                    return;
                }

                if (timeRemaining <= 0) {
                    nextRoundBossBar.removeAll();
                    currentRound++;
                    if (currentRound <= SquabbleMap.TOTAL_ROUNDS) {
                        startRound();
                    } else {
                        stopGame(false);
                    }
                    cancel();
                    return;
                }

                nextRoundBossBar.setTitle(ChatColor.GREEN + "Next round starting in " + timeRemaining + " seconds");
                nextRoundBossBar.setProgress((double) timeRemaining / SquabbleMap.NEXT_ROUND_SECONDS);
                timeRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void displayRoundResults() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Top 10 players in " + ChatColor.AQUA + "Squabble" + ChatColor.WHITE + "" + ChatColor.BOLD + "!");

        List<Map.Entry<UUID, Integer>> topPlayers = plugin.getPlayerScoreManager().getRoundCoins().entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());

        for (Map.Entry<UUID, Integer> entry : topPlayers) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            Bukkit.broadcastMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + name + ChatColor.WHITE + " (" + entry.getValue() + " coins)");
        }
        Bukkit.broadcastMessage("");

        Map.Entry<UUID, Integer> mostKills = roundKills.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (mostKills != null) {
            String name = Bukkit.getOfflinePlayer(mostKills.getKey()).getName();
            Bukkit.broadcastMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Most kills: " + ChatColor.YELLOW + name + ChatColor.WHITE + " (" + mostKills.getValue() + ")");
            Bukkit.broadcastMessage("");
        }

        // Damage
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) return;
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Most damage done in " + ChatColor.AQUA + "Squabble" + ChatColor.WHITE + "" + ChatColor.BOLD + "!");

                List<Map.Entry<UUID, Double>> topDamage = roundDamage.entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(5)
                        .collect(java.util.stream.Collectors.toList());

                for (Map.Entry<UUID, Double> entry : topDamage) {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    Bukkit.broadcastMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + name + " " + ChatColor.RED + String.format("%.1f", entry.getValue()));
                }
                Bukkit.broadcastMessage("");
                // Standings

                if (currentRound >= SquabbleMap.TOTAL_ROUNDS) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!active) return;
                            Bukkit.broadcastMessage("");
                            Bukkit.broadcastMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Current event standings:");

                            List<Team> standings = plugin.getTeamManager().getTeams().stream()
                                    .sorted((t1, t2) -> Integer.compare(t2.getScore(), t1.getScore()))
                                    .collect(java.util.stream.Collectors.toList());

                            for (int i = 0; i < standings.size(); i++) {
                                Team team = standings.get(i);
                                Bukkit.broadcastMessage(ChatColor.YELLOW.toString() + (i + 1) + ". " + team.getColor() + team.getName() + ChatColor.WHITE + ": " + ChatColor.YELLOW + team.getScore());
                            }
                            Bukkit.broadcastMessage("");
                        }
                    }.runTaskLater(plugin, 3 * 20L);
                }
            }
        }.runTaskLater(plugin, 3 * 20L);
    }

    public void handlePlayerDeath(Player player) {
        if (!active || !gameStarted) return;

        if (alivePlayers.containsKey(player.getUniqueId())) {
            UUID killerId = lastAttacker.remove(player.getUniqueId());
            if (killerId != null) {
                Player killer = Bukkit.getPlayer(killerId);
                if (killer != null && !killer.equals(player)) {
                    handleKill(killer, player);
                }
            }

            alivePlayers.remove(player.getUniqueId());
            deathsThisRound++;
            // 1 Coin Per Survival For the First 10 Deaths

            if (deathsThisRound <= SquabbleMap.MAX_SURVIVAL_REWARD_DEATHS) {
                for (UUID aliveUuid : alivePlayers.keySet()) {
                    plugin.getPlayerScoreManager().addCoins(aliveUuid, 1);
                }
            }
        }

        player.setGameMode(GameMode.SPECTATOR);
        checkWinCondition();
    }

    public void handleKill(Player killer, Player victim) {
        if (!active || !gameStarted) return;

        int killNumber = totalKillsAcrossRounds.getOrDefault(killer.getUniqueId(), 0) + 1;
        totalKillsAcrossRounds.put(killer.getUniqueId(), killNumber);
        int reward = Math.max(1, 21 - killNumber);

        plugin.getPlayerScoreManager().addCoins(killer.getUniqueId(), reward);
        roundKills.put(killer.getUniqueId(), roundKills.getOrDefault(killer.getUniqueId(), 0) + 1);
        killer.sendMessage(ChatColor.GOLD + "Kill! +" + reward + " Coins");
    }

    public void handleDamage(Player attacker, Player victim, double damage) {
        if (!active || !gameStarted) return;
        roundDamage.put(attacker.getUniqueId(), roundDamage.getOrDefault(attacker.getUniqueId(), 0.0) + damage);
        lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
    }

    public void handleLogout(Player player) {
        if (!active || !gameStarted || !alivePlayers.containsKey(player.getUniqueId())) return;

        UUID uuid = player.getUniqueId();

        preLogoutLocations.put(uuid, player.getLocation().clone());

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                handlePlayerDeath(player);
                logoutTasks.remove(uuid);
                preLogoutLocations.remove(uuid);
            }
        }.runTaskLater(plugin, 30 * 20L);

        logoutTasks.put(uuid, task);
    }

    public void handleLogin(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = logoutTasks.remove(uuid);
        if (task != null) task.cancel();

        if (active && alivePlayers.containsKey(uuid)) {
            Location returnLocation = preLogoutLocations.remove(uuid);
            World world = Bukkit.getWorld(SquabbleMap.WORLD_NAME);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                if (!active || !alivePlayers.containsKey(uuid)) return;

                Location destination = returnLocation;
                boolean validSaved = destination != null
                        && world != null
                        && destination.getWorld() != null
                        && destination.getWorld().equals(world)
                        && destination.getY() >= SquabbleMap.FALL_ELIMINATION_MIN_Y
                        && destination.getY() <= currentSkyHeight;

                if (!validSaved) {
                    Team team = alivePlayers.get(uuid);
                    SquabbleMap.TeamData teamData = team != null ? SquabbleMap.TEAMS.get(team.getId().toLowerCase()) : null;
                    if (world != null && teamData != null) {
                        List<Location> spawns = getSpawnLocations(world, teamData);
                        destination = !spawns.isEmpty() ? spawns.get(0) : (world != null ? world.getSpawnLocation() : null);
                    } else if (world != null) {
                        destination = world.getSpawnLocation();
                    }
                }

                if (destination != null) {
                    player.teleport(destination);
                }
                player.setGameMode(GameMode.SURVIVAL);
            }, 1L);
            return;
        }

        preLogoutLocations.remove(uuid);

        if (active && gameStarted && !alivePlayers.containsKey(uuid)) {
            boolean isStaff = plugin.getAdminManager().isAdmin(uuid) || plugin.getAdminManager().isDev(uuid);
            if (!isStaff) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    private void checkWinCondition() {
        Set<Team> remainingTeams = new HashSet<>(alivePlayers.values());
        if (remainingTeams.size() == 1) {
            Team winner = remainingTeams.iterator().next();
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Team " + winner.getColor() + winner.getName() + ChatColor.GOLD + " wins!");

            String title = winner.getColor() + winner.getName();
            String subtitle = ChatColor.WHITE + "has won Squabble Round " + currentRound + "/" + SquabbleMap.TOTAL_ROUNDS;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(title, subtitle, 10, 70, 20);
            }
            // 20 Coins If You Win The Round (for each alive player on the team)

            for (Map.Entry<UUID, Team> entry : alivePlayers.entrySet()) {
                if (entry.getValue().equals(winner)) {
                    plugin.getPlayerScoreManager().addCoins(entry.getKey(), 20);
                }
            }

            distributeDamagePool();
            endRound();
        } else if (remainingTeams.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Game over! No teams remaining.");
            distributeDamagePool();
            endRound();
        }
    }

    private void distributeDamagePool() {
        double totalDamage = 0;
        for (double d : roundDamage.values()) {
            totalDamage += d;
        }

        if (totalDamage <= 0) return;

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Distributing " + SquabbleMap.DAMAGE_POOL_TOTAL + " Coin Damage Pool...");
        for (Map.Entry<UUID, Double> entry : roundDamage.entrySet()) {
            double percentage = entry.getValue() / totalDamage;
            int coins = (int) Math.round(percentage * SquabbleMap.DAMAGE_POOL_TOTAL);
            if (coins > 0) {
                plugin.getPlayerScoreManager().addCoins(entry.getKey(), coins);
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) {
                    p.sendMessage(ChatColor.YELLOW + "You dealt " + String.format("%.1f", percentage * 100) + "% of total damage and received " + coins + " coins!");
                }
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public Team getPlayerTeam(UUID uuid) {
        return alivePlayers.get(uuid);
    }
}