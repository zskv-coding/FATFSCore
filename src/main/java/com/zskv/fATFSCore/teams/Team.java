package com.zskv.fATFSCore.teams;

import org.bukkit.ChatColor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Team {
    private final String id;
    private final String name;
    private final ChatColor color;
    private final Set<UUID> members;
    private int score;

    public Team(String id, String name, ChatColor color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.members = new HashSet<>();
        this.score = 0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int amount) {
        this.score += amount;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }
}
