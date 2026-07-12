package com.zskv.fATFSCore.commands;

import com.zskv.fATFSCore.FATFSCore;
import org.bukkit.ChatColor;
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
import java.util.stream.Collectors;

public class ReadyCheckCommand implements CommandExecutor, TabCompleter {
    private final FATFSCore plugin;

    public ReadyCheckCommand(FATFSCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /readycheck <start|answer>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (!player.hasPermission("event.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to start a ready check.");
                    return true;
                }
                plugin.getReadyCheckManager().startReadyCheck();
            }
            case "answer" -> {
                if (args.length < 2) return true;
                boolean ready = args[1].equalsIgnoreCase("yes");
                plugin.getReadyCheckManager().handleAnswer(player, ready);
            }
            default -> player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("event.admin")) {
                options.add("start");
            }
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
