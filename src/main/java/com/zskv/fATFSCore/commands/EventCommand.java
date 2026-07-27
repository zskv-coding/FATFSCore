package com.zskv.fATFSCore.commands;

import com.zskv.fATFSCore.FATFSCore;
import com.zskv.fATFSCore.teams.Team;
import com.zskv.fATFSCore.teams.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final FATFSCore plugin;

    public EventCommand(FATFSCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        TeamManager teamManager = plugin.getTeamManager();

        switch (args[0].toLowerCase()) {
            case "team" -> handleTeamCommand(player, args, teamManager);
            case "score" -> handleScoreCommand(player, args, teamManager);
            case "game" -> handleGameCommand(player, args);
            case "start" -> handleStartCommand(player, args);
            case "timer" -> handleTimerCommand(player, args);
            case "reload" -> {
                if (!player.hasPermission("event.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                    return true;
                }
                plugin.reloadPlugin();
                player.sendMessage(ChatColor.GREEN + "Event configuration reloaded!");
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleTeamCommand(Player player, String[] args, TeamManager teamManager) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /event team <join|leave|list|set|remove|add|delete>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (!player.hasPermission("event.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                    return;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /event team add <Color> <Team Name>");
                    return;
                }
                ChatColor color;
                try {
                    color = ChatColor.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid color: " + args[2]);
                    return;
                }
                String teamName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                plugin.addTeam(args[2].toLowerCase(), teamName, color);
                player.sendMessage(ChatColor.GREEN + "Team " + color + teamName + ChatColor.GREEN + " created!");
            }
            case "delete" -> {
                if (!player.hasPermission("event.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /event team delete <color_id>");
                    return;
                }
                plugin.deleteTeam(args[2]);
                player.sendMessage(ChatColor.GREEN + "Team " + args[2] + " deleted.");
            }
            case "join" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /event team join <team_id> [player]");
                    return;
                }
                Team team = teamManager.getTeam(args[2]);
                if (team == null) {
                    player.sendMessage(ChatColor.RED + "Team not found: " + args[2]);
                    return;
                }

                Player target = player;
                if (args.length >= 4) {
                    if (!player.hasPermission("event.admin")) {
                        player.sendMessage(ChatColor.RED + "You don't have permission to add others to teams.");
                        return;
                    }
                    target = Bukkit.getPlayer(args[3]);
                    if (target == null) {
                        player.sendMessage(ChatColor.RED + "Player not found: " + args[3]);
                        return;
                    }
                }

                teamManager.addPlayerToTeam(target.getUniqueId(), team);
                if (target.equals(player)) {
                    player.sendMessage(ChatColor.GREEN + "You joined " + team.getColor() + team.getName() + ChatColor.GREEN + "!");
                } else {
                    player.sendMessage(ChatColor.GREEN + target.getName() + " was added to " + team.getColor() + team.getName() + ChatColor.GREEN + "!");
                    target.sendMessage(ChatColor.GREEN + "You were added to " + team.getColor() + team.getName() + ChatColor.GREEN + "!");
                }
                updatePlayerVisuals(target, team);
            }
            case "leave" -> {
                teamManager.removePlayerFromTeam(player.getUniqueId());
                player.sendMessage(ChatColor.YELLOW + "You left your team.");
                resetPlayerVisuals(player);
            }
            case "set" -> {
                if (!player.hasPermission("event.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                    return;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /event team set <player> <team_id>");
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                Team team = teamManager.getTeam(args[3]);
                if (team == null) {
                    player.sendMessage(ChatColor.RED + "Team not found: " + args[3]);
                    return;
                }
                teamManager.addPlayerToTeam(target.getUniqueId(), team);
                player.sendMessage(ChatColor.GREEN + (target.getName() != null ? target.getName() : args[2]) + " was added to " + team.getColor() + team.getName() + ChatColor.GREEN + "!");
                if (target.isOnline() && target.getPlayer() != null) {
                    updatePlayerVisuals(target.getPlayer(), team);
                }
            }
            case "remove" -> {
                if (!player.hasPermission("event.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /event team remove <player>");
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                teamManager.removePlayerFromTeam(target.getUniqueId());
                player.sendMessage(ChatColor.YELLOW + (target.getName() != null ? target.getName() : args[2]) + " was removed from their team.");
                if (target.isOnline() && target.getPlayer() != null) {
                    resetPlayerVisuals(target.getPlayer());
                }
            }
            case "list" -> {
                player.sendMessage(ChatColor.GOLD + "Teams:");
                for (Team team : teamManager.getTeams()) {
                    player.sendMessage("- " + team.getColor() + team.getName() + ChatColor.GRAY + " (" + team.getId() + ")");
                }
            }
            default -> player.sendMessage(ChatColor.RED + "Unknown team subcommand. Use join, leave, or list.");
        }
    }

    private void handleScoreCommand(Player player, String[] args, TeamManager teamManager) {
        if (!player.hasPermission("event.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
            return;
        }

        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /event score <team_id> <add|remove|set> <amount>");
            return;
        }

        Team team = teamManager.getTeam(args[1]);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Team not found: " + args[1]);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
            return;
        }

        switch (args[2].toLowerCase()) {
            case "add" -> plugin.getScoreManager().addScore(team, amount);
            case "remove" -> plugin.getScoreManager().addScore(team, -amount);
            case "set" -> plugin.getScoreManager().setScore(team, amount);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown score subcommand. Use add, remove, or set.");
                return;
            }
        }

        player.sendMessage(ChatColor.GREEN + "Updated score for " + team.getColor() + team.getName() + ChatColor.GREEN + " to " + ChatColor.YELLOW + team.getScore() + ChatColor.GREEN + ".");
    }

    private void handleGameCommand(Player player, String[] args) {
        if (!player.hasPermission("event.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /event game <start|stop> <game_id>");
            return;
        }

        String action = args[1].toLowerCase();
        String gameId = args[2].toLowerCase();

        if (gameId.equalsIgnoreCase("squabble")) {
            if (action.equals("start")) {
                plugin.getSquabbleManager().startSequence();
                player.sendMessage(ChatColor.GREEN + "Starting Squabble...");
            } else if (action.equals("stop")) {
                if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
                    player.sendMessage(ChatColor.YELLOW + "Are you sure? Use " + ChatColor.GOLD + "/event game stop squabble confirm" + ChatColor.YELLOW + " to stop the game.");
                    return;
                }
                plugin.getSquabbleManager().stopGame(true);
                player.sendMessage(ChatColor.RED + "Stopping Squabble...");
            } else {
                player.sendMessage(ChatColor.RED + "Unknown action. Use start or stop.");
            }
        } else if (gameId.equalsIgnoreCase("hideseek")) {
            if (action.equals("start")) {
                plugin.getHideSeekManager().startSequence(player);
                player.sendMessage(ChatColor.GREEN + "Starting Hide and Seek...");
            } else if (action.equals("stop")) {
                if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
                    player.sendMessage(ChatColor.YELLOW + "Are you sure? Use " + ChatColor.GOLD + "/event game stop hideseek confirm" + ChatColor.YELLOW + " to stop the game.");
                    return;
                }
                plugin.getHideSeekManager().stopGame(true);
                player.sendMessage(ChatColor.RED + "Stopping Hide and Seek...");
            } else {
                player.sendMessage(ChatColor.RED + "Unknown action. Use start or stop.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Unknown game: " + gameId);
        }
    }

    private void handleStartCommand(Player player, String[] args) {
        if (!player.hasPermission("event.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
            return;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage(ChatColor.YELLOW + "Are you sure? Use " + ChatColor.GOLD + "/event start confirm" + ChatColor.YELLOW + " to start the event.");
            return;
        }

        Bukkit.broadcastMessage(ChatColor.GREEN + "The event is starting!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(ChatColor.GOLD + "Fast and the Friendslop", ChatColor.YELLOW + "Starting soon!", 10, 70, 20);
        }

        plugin.getTimerManager().startTimer("Starting soon", 60, () -> {
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "THE EVENT HAS BEGUN!");
        });
    }

    private void handleTimerCommand(Player player, String[] args) {
        if (!player.hasPermission("event.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /event timer <pause|unpause|skip>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "pause" -> {
                plugin.getTimerManager().pause();
                player.sendMessage(ChatColor.YELLOW + "Timer paused.");
            }
            case "unpause" -> {
                plugin.getTimerManager().unpause();
                player.sendMessage(ChatColor.GREEN + "Timer unpaused.");
            }
            case "skip" -> {
                plugin.getTimerManager().skip();
                player.sendMessage(ChatColor.GREEN + "Timer skipped.");
            }
            default -> player.sendMessage(ChatColor.RED + "Unknown timer subcommand. Use pause, unpause, or skip.");
        }
    }

    private void updatePlayerVisuals(Player player, Team team) {
        String coloredName = team.getColor() + player.getName() + ChatColor.RESET;
        
        player.setDisplayName(coloredName);
        player.setPlayerListName(coloredName);
    }

    private void resetPlayerVisuals(Player player) {
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "/event team join <team_id> " + ChatColor.GRAY + "- Join a team");
        player.sendMessage(ChatColor.YELLOW + "/event team leave " + ChatColor.GRAY + "- Leave your team");
        player.sendMessage(ChatColor.YELLOW + "/event team list " + ChatColor.GRAY + "- List all teams");
        if (player.hasPermission("event.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/event start confirm " + ChatColor.GRAY + "- Start the event");
            player.sendMessage(ChatColor.YELLOW + "/event timer <pause|unpause|skip> " + ChatColor.GRAY + "- Manage timer");
            player.sendMessage(ChatColor.YELLOW + "/event team add <Color> <Name> " + ChatColor.GRAY + "- Create a team");
            player.sendMessage(ChatColor.YELLOW + "/event team delete <color_id> " + ChatColor.GRAY + "- Delete a team");
            player.sendMessage(ChatColor.YELLOW + "/event team set <player> <team_id> " + ChatColor.GRAY + "- Set player team");
            player.sendMessage(ChatColor.YELLOW + "/event team remove <player> " + ChatColor.GRAY + "- Remove player from team");
            player.sendMessage(ChatColor.YELLOW + "/event score <team_id> <add|remove|set> <amount> " + ChatColor.GRAY + "- Manage scores");
            player.sendMessage(ChatColor.YELLOW + "/event reload " + ChatColor.GRAY + "- Reload configuration");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("team");
            if (sender.hasPermission("event.admin")) {
                completions.add("score");
                completions.add("game");
                completions.add("start");
                completions.add("timer");
                completions.add("reload");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("game") && sender.hasPermission("event.admin")) {
                completions.add("start");
                completions.add("stop");
            } else if (args[0].equalsIgnoreCase("start") && sender.hasPermission("event.admin")) {
                completions.add("confirm");
            } else if (args[0].equalsIgnoreCase("timer") && sender.hasPermission("event.admin")) {
                completions.add("pause");
                completions.add("unpause");
                completions.add("skip");
            } else if (args[0].equalsIgnoreCase("team")) {
                completions.add("join");
                completions.add("leave");
                completions.add("list");
                if (sender.hasPermission("event.admin")) {
                    completions.add("set");
                    completions.add("remove");
                    completions.add("add");
                    completions.add("delete");
                }
            } else if (args[0].equalsIgnoreCase("score") && sender.hasPermission("event.admin")) {
                completions.addAll(plugin.getTeamManager().getTeams().stream()
                        .map(Team::getId)
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("game") && sender.hasPermission("event.admin")) {
                completions.add("squabble");
                completions.add("hideseek");
                completions.add("duels");
            } else if (args[0].equalsIgnoreCase("team")) {
                if (args[1].equalsIgnoreCase("join") || args[1].equalsIgnoreCase("delete")) {
                    completions.addAll(plugin.getTeamManager().getTeams().stream()
                            .map(Team::getId)
                            .collect(Collectors.toList()));
                } else if (args[1].equalsIgnoreCase("add") && sender.hasPermission("event.admin")) {
                    completions.addAll(Arrays.stream(ChatColor.values())
                            .filter(ChatColor::isColor)
                            .map(ChatColor::name)
                            .collect(Collectors.toList()));
                } else if ((args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("remove")) && sender.hasPermission("event.admin")) {
                    List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).filter(Objects::nonNull).forEach(name -> {
                        if (!players.contains(name)) players.add(name);
                    });
                    completions.addAll(players);
                }
            } else if (args[0].equalsIgnoreCase("score") && sender.hasPermission("event.admin")) {
                completions.add("add");
                completions.add("remove");
                completions.add("set");
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("team")) {
                if (args[1].equalsIgnoreCase("set") && sender.hasPermission("event.admin")) {
                    completions.addAll(plugin.getTeamManager().getTeams().stream()
                            .map(Team::getId)
                            .collect(Collectors.toList()));
                } else if (args[1].equalsIgnoreCase("join") && sender.hasPermission("event.admin")) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()));
                }
            } else if (args[0].equalsIgnoreCase("game") && args[1].equalsIgnoreCase("stop") && sender.hasPermission("event.admin")) {
                completions.add("confirm");
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}
