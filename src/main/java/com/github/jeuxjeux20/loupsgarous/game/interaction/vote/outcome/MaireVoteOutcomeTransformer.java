package com.github.jeuxjeux20.loupsgarous.game.interaction.vote.outcome;

import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;
import com.github.jeuxjeux20.loupsgarous.game.stages.VillageVoteStage;
import com.github.jeuxjeux20.loupsgarous.game.tags.LGTags;

public class MaireVoteOutcomeTransformer implements VoteOutcomeTransformer<LGPlayer> {
    @Override
    public VoteOutcome<LGPlayer> transform(VoteOutcomeContext<LGPlayer> context, VoteOutcome<LGPlayer> outcome) {
        if (context.getVoteClass() != VillageVoteStage.VillageVote.class) {
            return outcome;
        }

        return outcome.accept(new TransformingVoteOutcomeVisitor<LGPlayer>() {
            @Override
            public VoteOutcome<LGPlayer> visit(IndecisiveVoteOutcome<LGPlayer> indecisiveVoteOutcome) {
                for (LGPlayer conflictingCandidate : indecisiveVoteOutcome.getConflictingCandidates()) {
                    if (conflictingCandidate.tags().has(LGTags.MAIRE)) {
                        return new RelativeMajorityVoteOutcome<>(conflictingCandidate);
                    }
                }

                return indecisiveVoteOutcome;
            }
        });
    }
}
