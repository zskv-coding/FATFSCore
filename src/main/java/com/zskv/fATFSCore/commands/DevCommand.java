package com.zskv.fATFSCore.commands;

import com.zskv.fATFSCore.FATFSCore;
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

public class DevCommand implements CommandExecutor, TabCompleter {
    private final FATFSCore plugin;

    public DevCommand(FATFSCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("event.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /dev <add|remove> <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        switch (args[0].toLowerCase()) {
            case "add" -> {
                plugin.getAdminManager().addDev(target);
                sender.sendMessage(ChatColor.GREEN + (target.getName() != null ? target.getName() : args[1]) + " added to devs.");
            }
            case "remove" -> {
                plugin.getAdminManager().removeDev(target);
                sender.sendMessage(ChatColor.GREEN + (target.getName() != null ? target.getName() : args[1]) + " removed from devs.");
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use add or remove.");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .forEach(name -> {
                        if (!players.contains(name)) players.add(name);
                    });

            return players.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
