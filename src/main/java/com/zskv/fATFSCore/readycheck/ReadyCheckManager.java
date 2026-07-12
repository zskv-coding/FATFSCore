package com.zskv.fATFSCore.readycheck;

import com.zskv.fATFSCore.FATFSCore;
import com.zskv.fATFSCore.teams.Team;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ReadyCheckManager {
    private final FATFSCore plugin;
    private boolean active = false;
    private final Set<String> respondedTeams = new HashSet<>();
    private final Map<String, Boolean> results = new HashMap<>();
    private BukkitTask timeoutTask;

    public ReadyCheckManager(FATFSCore plugin) {
        this.plugin = plugin;
    }

    public void startReadyCheck() {
        if (active) return;
        
        active = true;
        respondedTeams.clear();
        results.clear();

        TextComponent yes = new TextComponent(ChatColor.GREEN + "[Yes]");
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/readycheck answer yes"));
        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to mark team as READY").create()));

        TextComponent no = new TextComponent(ChatColor.RED + " [No]");
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/readycheck answer no"));
        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to mark team as NOT READY").create()));

        TextComponent buttons = new TextComponent("");
        buttons.addExtra(yes);
        buttons.addExtra(no);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Is your team ready?");
            player.sendMessage("");
            player.spigot().sendMessage(buttons);
            player.sendMessage("");
        }

        timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active) {
                endReadyCheck(true);
            }
        }, 20 * 20L);
    }

    public void handleAnswer(Player player, boolean ready) {
        if (!active) return;

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You must be in a team to answer the ready check.");
            return;
        }

        if (respondedTeams.contains(team.getId())) {
            player.sendMessage(ChatColor.RED + "Your team has already responded.");
            return;
        }

        respondedTeams.add(team.getId());
        results.put(team.getId(), ready);

        if (ready) {
            Bukkit.broadcastMessage(team.getColor() + "" + ChatColor.BOLD + team.getName() + ChatColor.GREEN + " are ready!");
        } else {
            Bukkit.broadcastMessage(team.getColor() + "" + ChatColor.BOLD + team.getName() + ChatColor.RED + " are not ready!");
        }

        checkCompletion();
    }

    private void checkCompletion() {
        // Check if all registered teams have responded
        if (respondedTeams.size() >= plugin.getTeamManager().getTeams().size()) {
            endReadyCheck(false);
        }
    }

    private void endReadyCheck(boolean timedOut) {
        if (!active) return;
        active = false;

        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }

        if (timedOut) {
            Bukkit.broadcastMessage(ChatColor.RED + "");
            // Mark non-responded teams as NOT READY
            List<Team> sortedTeams = new ArrayList<>(plugin.getTeamManager().getTeams());
            sortedTeams.sort(Comparator.comparingInt(this::getRainbowOrder));

            for (Team team : sortedTeams) {
                if (!respondedTeams.contains(team.getId())) {
                    results.put(team.getId(), false);
                    respondedTeams.add(team.getId());
                    Bukkit.broadcastMessage(team.getColor() + "" + ChatColor.BOLD + team.getName() + ChatColor.RED + " are not ready!");
                }
            }
        }

        Bukkit.broadcastMessage(ChatColor.RED + "Chat unmuted.");
    }

    public boolean isActive() {
        return active;
    }

    private int getRainbowOrder(Team team) {
        return switch (team.getColor()) {
            case RED -> 1;
            case GOLD -> 2;
            case YELLOW -> 3;
            case GREEN -> 4;
            case AQUA -> 5;
            case BLUE -> 6;
            case DARK_PURPLE -> 7;
            case LIGHT_PURPLE -> 8;
            default -> 99;
        };
    }
}
