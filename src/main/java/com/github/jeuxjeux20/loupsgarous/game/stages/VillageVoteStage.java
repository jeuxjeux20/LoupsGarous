package com.github.jeuxjeux20.loupsgarous.game.stages;

import com.github.jeuxjeux20.loupsgarous.game.Countdown;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGGameTurnTime;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;
import com.github.jeuxjeux20.loupsgarous.game.atmosphere.VoteStructure;
import com.github.jeuxjeux20.loupsgarous.game.interaction.InteractableEntry;
import com.github.jeuxjeux20.loupsgarous.game.interaction.LGInteractableKeys;
import com.github.jeuxjeux20.loupsgarous.game.interaction.condition.PickConditions;
import com.github.jeuxjeux20.loupsgarous.game.interaction.vote.AbstractPlayerVotable;
import com.github.jeuxjeux20.loupsgarous.game.interaction.vote.Votable;
import com.github.jeuxjeux20.loupsgarous.game.kill.reasons.VillageVoteKillReason;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.bukkit.ChatColor;

import java.util.Optional;

import static com.github.jeuxjeux20.loupsgarous.LGChatStuff.info;

@MajorityVoteShortensCountdown(LGInteractableKeys.Names.PLAYER_VOTE)
public class VillageVoteStage extends CountdownLGStage {
    private final VillageVotable votable;
    private final VoteStructure voteStructure;

    @Inject
    VillageVoteStage(@Assisted LGGameOrchestrator orchestrator,
                     VoteStructure.Factory voteStructureFactory,
                     AbstractPlayerVotable.PlayerVoteDependencies voteDependencies) {
        super(orchestrator);

        votable = new VillageVotable(voteDependencies);
        voteStructure =
                voteStructureFactory.create(orchestrator, orchestrator.world().getSpawnLocation(), votable);

        orchestrator.interactables().put(bind(votable));

        bind(voteStructure);
        bindModule(voteStructure.createInteractionModule());
    }

    @Override
    protected Countdown createCountdown() {
        if (orchestrator.game().getAlivePlayers().count() <= 2) {
            // Only two players? They'll vote each other and that's it.
            return Countdown.of(30);
        } else {
            return Countdown.of(90);
        }
    }

    @Override
    public boolean shouldRun() {
        return orchestrator.turn().getTime() == LGGameTurnTime.DAY &&
               votable.canSomeonePick();
    }

    @Override
    protected void start() {
        voteStructure.build();
    }

    @Override
    protected void finish() {
        votable.closeAndReportException();
        computeVoteOutcome();
    }

    @Override
    public String getName() {
        return "Vote du village";
    }

    @Override
    public String getTitle() {
        return "Le village va voter";
    }

    private void computeVoteOutcome() {
        Optional<LGPlayer> maybeMajority = votable.getOutcome().getElected();

        if (maybeMajority.isPresent()) {
            orchestrator.kills().instantly(maybeMajority.get(), VillageVoteKillReason::new);
        } else {
            orchestrator.chat().sendToEveryone(info("Le village n'a pas pu se décider !"));
        }
    }

    public VillageVotable votes() {
        return votable;
    }

    public final class VillageVotable extends AbstractPlayerVotable {
        private VillageVotable(PlayerVoteDependencies dependencies) {
            super(VillageVoteStage.this.orchestrator, dependencies);
        }

        @Override
        public InteractableEntry<Votable<LGPlayer>> getEntry() {
            return new InteractableEntry<>(LGInteractableKeys.PLAYER_VOTE, this);
        }

        @Override
        protected PickConditions<LGPlayer> additionalVoteConditions() {
            return PickConditions.empty();
        }

        @Override
        public String getPointingText() {
            return "vote pour tuer";
        }

        @Override
        public ChatColor getHighlightColor() {
            return ChatColor.RED;
        }
    }
}
