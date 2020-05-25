package com.github.jeuxjeux20.loupsgarous.game;

import com.github.jeuxjeux20.loupsgarous.game.cards.composition.Composition;
import com.github.jeuxjeux20.loupsgarous.game.cards.composition.SnapshotComposition;
import com.google.common.collect.ImmutableSet;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A set of data to create a new Loups-Garous game instance.
 */
public final class LGGameLobbyInfo {
    private final MultiverseWorld world;
    private final CommandSender initiator;
    private final Player owner;
    private final UUID id;
    private final ImmutableSet<Player> players;
    private final Composition composition;

    public LGGameLobbyInfo(Set<Player> players, Composition composition,
                           MultiverseWorld world, CommandSender initiator, UUID id) {
        if (players.isEmpty()) {
            throw new IllegalArgumentException("There are no players.");
        }
        if (players.size() > composition.getCards().size()) {
            throw new IllegalArgumentException("There are more players than cards.");
        }

        this.players = ImmutableSet.copyOf(players);
        this.composition = new SnapshotComposition(composition);
        this.world = world;
        this.initiator = initiator;
        this.id = id;

        this.owner = determineOwner();
    }

    private Player determineOwner() {
        if (initiator instanceof Player) {
            if (players.contains(initiator)) {
                return (Player) initiator;
            }
        }
        return players.iterator().next();
    }

    public MultiverseWorld getWorld() {
        return world;
    }

    public CommandSender getInitiator() {
        return initiator;
    }

    public UUID getId() {
        return id;
    }

    public ImmutableSet<Player> getPlayers() {
        return players;
    }

    public Composition getComposition() {
        return composition;
    }

    public Player getOwner() {
        return owner;
    }
}
