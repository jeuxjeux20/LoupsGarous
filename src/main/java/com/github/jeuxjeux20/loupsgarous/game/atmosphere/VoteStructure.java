package com.github.jeuxjeux20.loupsgarous.game.atmosphere;

import com.github.jeuxjeux20.loupsgarous.Plugin;
import com.github.jeuxjeux20.loupsgarous.game.LGGameManager;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayerAndGame;
import com.github.jeuxjeux20.loupsgarous.game.event.interaction.LGPickEvent;
import com.github.jeuxjeux20.loupsgarous.game.event.interaction.LGPickRemovedEvent;
import com.github.jeuxjeux20.loupsgarous.game.stages.interaction.Votable;
import com.github.jeuxjeux20.loupsgarous.util.Check;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.lucko.helper.Events;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.metadata.Metadata;
import me.lucko.helper.metadata.MetadataKey;
import me.lucko.helper.metadata.TransientValue;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VoteStructure implements Structure {
    public static final MetadataKey<LGPlayer> ARMOR_STAND_PLAYER_KEY =
            MetadataKey.create("armor_stand_vote", LGPlayer.class);

    private final LGGameOrchestrator orchestrator;
    private final Location location;
    private final World world;
    private final Votable votable;
    private final LGGameManager gameManager;

    private final int spacing = 3;
    private final Material blockMaterial = Material.OAK_WOOD;

    private BackedStructure backedStructure = BackedStructure.EMPTY;

    @Inject
    VoteStructure(@Assisted LGGameOrchestrator orchestrator, @Assisted Location location, @Assisted Votable votable,
                  LGGameManager gameManager) {
        this.orchestrator = orchestrator;
        this.location = location;
        this.world = location.getWorld();
        this.votable = votable;
        this.gameManager = gameManager;
    }

    public void build() {
        remove();

        BuildingContext buildingContext = createBuildingContext();

        placeBlocks(buildingContext);
        placeArmorStands(buildingContext);

        backedStructure = buildingContext.structureBuilder.build();
        backedStructure.build();
    }

    private BuildingContext createBuildingContext() {
        Votable.VoteState voteState = votable.getCurrentState();

        List<LGPlayer> players = orchestrator.game().getPlayers().stream()
                .filter(Check.predicate(voteState::canPickTarget))
                .collect(Collectors.toList());
        LGPlayer playerWithMostVotes = voteState.getPlayerWithMostVotes();

        return new BuildingContext(players, playerWithMostVotes);
    }

    private void placeBlocks(BuildingContext context) {
        Location blockLocation = location.clone();
        for (int i = 0; i < context.blockCount; i++) {
            context.structureBuilder.transformBlock(blockLocation, block -> block.setType(blockMaterial));

            blockLocation.add(1, 0, 0);
        }
    }

    private void placeArmorStands(BuildingContext context) {
        Location armorStandLocation = location.clone();
        for (LGPlayer player : context.players) {
            Location correctedLocation = armorStandLocation.clone().add(0.5, 1, 0.5);

            context.structureBuilder.spawnEntity(() -> createArmorStand(player, correctedLocation, context));

            armorStandLocation.add(spacing, 0, 0);
        }
    }

    private ArmorStand createArmorStand(LGPlayer player, Location armorStandLocation, BuildingContext context) {
        int voteCount = votable.getCurrentState().getPlayersVoteCount().getOrDefault(player, 0);
        String color = context.playerWithMostVotes == player ? ChatColor.RED.toString() + ChatColor.BOLD : "";

        ArmorStand armorStand = world.spawn(armorStandLocation, ArmorStand.class);

        armorStand.setCustomName(color + player.getName() + "(" + voteCount + ")");
        armorStand.setCustomNameVisible(true);

        EntityEquipment equipment = Objects.requireNonNull(armorStand.getEquipment());
        ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD).transformMeta(meta -> {
            SkullMeta skullMeta = (SkullMeta) meta;

            skullMeta.setOwningPlayer(player.getOfflineMinecraftPlayer());
        }).build();
        equipment.setHelmet(head);

        Metadata.provideForEntity(armorStand).put(ARMOR_STAND_PLAYER_KEY, new TransientValue<LGPlayer>() {
            @Override
            public LGPlayer getOrNull() {
                return player;
            }

            @Override
            public boolean shouldExpire() {
                return armorStand.isDead();
            }
        });

        return armorStand;
    }

    public void remove() {
        backedStructure.remove();
        backedStructure = BackedStructure.EMPTY;
    }

    public TerminableModule createInteractionModule() {
        return new InteractionModule();
    }

    private class InteractionModule implements TerminableModule {
        @Override
        public void setup(@Nonnull TerminableConsumer consumer) {
            Events.merge(LGPickEvent.class, LGPickEvent.class, LGPickRemovedEvent.class)
                    .filter(votable::isMyEvent)
                    .handler(e -> build())
                    .bindWith(consumer);

            Events.subscribe(PlayerInteractAtEntityEvent.class)
                    .handler(this::handleEntityInteraction)
                    .bindWith(consumer);
        }

        private void handleEntityInteraction(PlayerInteractAtEntityEvent e) {
            LGPlayer player = gameManager.getPlayerInGame(e.getPlayer())
                    .filter(x -> x.getOrchestrator() == orchestrator)
                    .map(LGPlayerAndGame::getPlayer)
                    .orElse(null);

            if (player == null) {
                return;
            }

            Entity rightClicked = e.getRightClicked();

            Metadata.provideForEntity(rightClicked).get(ARMOR_STAND_PLAYER_KEY)
                    .ifPresent(target -> {
                        Votable.VoteState voteState = votable.getCurrentState();
                        Check check = voteState.canPick(player, target);

                        if (check.isSuccess()) {
                            voteState.togglePick(player, target);
                        } else {
                            e.getPlayer().sendMessage(ChatColor.RED + check.getErrorMessage());
                        }
                    });
        }
    }

    private final class BuildingContext {
        final List<LGPlayer> players;
        final @Nullable LGPlayer playerWithMostVotes;
        final BackedStructure.Builder structureBuilder = BackedStructure.builder();
        final int blockCount;

        BuildingContext(List<LGPlayer> players, @Nullable LGPlayer playerWithMostVotes) {
            this.players = players;
            this.playerWithMostVotes = playerWithMostVotes;

            blockCount = 1 + spacing * (players.size() - 1);
        }
    }

    public interface Factory {
        VoteStructure create(LGGameOrchestrator orchestrator, Location location, Votable votable);
    }
}