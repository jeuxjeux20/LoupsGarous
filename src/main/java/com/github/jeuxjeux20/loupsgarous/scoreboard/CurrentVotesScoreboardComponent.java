package com.github.jeuxjeux20.loupsgarous.scoreboard;

import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;
import com.github.jeuxjeux20.loupsgarous.event.LGEvent;
import com.github.jeuxjeux20.loupsgarous.event.interaction.LGPickEvent;
import com.github.jeuxjeux20.loupsgarous.event.interaction.LGPickRemovedEvent;
import com.github.jeuxjeux20.loupsgarous.event.phase.LGPhaseStartingEvent;
import com.github.jeuxjeux20.loupsgarous.interaction.LGInteractableKeys;
import com.github.jeuxjeux20.loupsgarous.interaction.vote.Vote;
import com.google.common.collect.ImmutableList;
import org.bukkit.ChatColor;

import java.util.Optional;

public class CurrentVotesScoreboardComponent implements ScoreboardComponent {
    @Override
    public ImmutableList<Line> render(LGPlayer player, LGGameOrchestrator orchestrator) {
        if (!orchestrator.isGameRunning()) return ImmutableList.of();

        ImmutableList.Builder<Line> lines = ImmutableList.builder();

        // TODO: Broader votables
        Optional<Vote<LGPlayer>> maybeVotable =
                orchestrator.interactables().single(LGInteractableKeys.PLAYER_VOTE)
                        .check(x -> x.conditions().checkPicker(player))
                        .getOptional();

        maybeVotable.ifPresent(votable -> {
            lines.add(new Line(ChatColor.LIGHT_PURPLE + "-= Votes =-"));

            LGPlayer elected = votable.getOutcome().getElected().orElse(null);

            votable.getVotes().forEachEntry((votedPlayer, voteCount) -> {
                boolean isElected = elected == votedPlayer;
                String color = isElected ? votable.getHighlightColor().toString() + ChatColor.BOLD : "";

                lines.add(new Line(color + votedPlayer.getName(), voteCount));
            });
        });

        return lines.build();
    }

    @Override
    public ImmutableList<Class<? extends LGEvent>> getUpdateTriggers() {
        return ImmutableList.of(LGPickEvent.class, LGPickRemovedEvent.class, LGPhaseStartingEvent.class);
    }
}
