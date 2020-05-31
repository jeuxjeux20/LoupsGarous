package com.github.jeuxjeux20.loupsgarous.game.teams;

import org.bukkit.ChatColor;

public final class LoupsGarousTeam extends LGTeam {
    public static final LoupsGarousTeam INSTANCE = new LoupsGarousTeam();

    private LoupsGarousTeam() {
    }

    @Override
    public String getName() {
        return "Loups-garous";
    }

    @Override
    public ChatColor getColor() {
        return ChatColor.RED;
    }
}
