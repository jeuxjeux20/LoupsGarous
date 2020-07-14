package com.github.jeuxjeux20.loupsgarous.game.interaction.vote;

import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;
import com.github.jeuxjeux20.loupsgarous.game.event.interaction.LGPickEvent;
import com.github.jeuxjeux20.loupsgarous.game.event.interaction.LGPickRemovedEvent;
import com.github.jeuxjeux20.loupsgarous.game.interaction.*;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import me.lucko.helper.Events;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractVote<T>
        extends AbstractStatefulPick<T>
        implements Vote<T>, SelfAwareInteractable {
    public AbstractVote(LGGameOrchestrator orchestrator) {
        super(orchestrator);
    }

    @Override
    public VoteOutcome<T> getOutcome() {
        if (getPicks().size() == 0) {
            return new NoVotesVoteOutcome<>();
        }

        List<Multiset.Entry<T>> sameVotesCandidates = getHighestSameVotesCandidates(getVotes());

        if (sameVotesCandidates.size() == 1) {
            return new RelativeMajorityVoteOutcome<>(sameVotesCandidates.get(0).getElement());
        } else {
            ImmutableList<T> conflictingCandidates = sameVotesCandidates.stream()
                    .map(Multiset.Entry::getElement)
                    .collect(ImmutableList.toImmutableList());

            return new IndecisiveVoteOutcome<>(conflictingCandidates);
        }
    }

    @Override
    public Multiset<T> getVotes() {
        return HashMultiset.create(getPicks().values());
    }

    @Override
    public final boolean conclude() {
        throwIfClosed();
        closeAndReportException();

        VoteOutcome<T> outcome = getOutcome();
        return conclude(outcome);
    }

    protected abstract boolean conclude(VoteOutcome<T> outcome);

    @Override
    protected final void safePick(LGPlayer picker, T target) {
        super.safePick(picker, target);

        if (canCallEvent()) {
            Events.call(new LGPickEvent(orchestrator, createPick(picker, target)));
        }
    }

    @Override
    protected final @Nullable T safeRemovePick(LGPlayer picker, boolean isInvalidate) {
        T removedTarget = super.safeRemovePick(picker, isInvalidate);

        if (removedTarget != null && canCallEvent()) {
            Events.call(new LGPickRemovedEvent(orchestrator, createPick(picker, removedTarget), isInvalidate));
        }

        return removedTarget;
    }

    private PickData<T, ?> createPick(LGPlayer picker, T target) {
        return new PickData<>(getEntry(), picker, target);
    }

    private boolean canCallEvent() {
        return orchestrator.interactables().has(getEntry());
    }

    @Override
    public abstract InteractableEntry<? extends Pick<T>> getEntry();

    @NotNull
    private List<Multiset.Entry<T>> getHighestSameVotesCandidates(Multiset<T> votes) {
        List<Multiset.Entry<T>> votesDescending = votes.entrySet().stream()
                .sorted(Comparator.<Multiset.Entry<T>, Integer>comparing(Multiset.Entry::getCount).reversed())
                .collect(Collectors.toList());

        List<Multiset.Entry<T>> results = new ArrayList<>();
        int lastCount = -1;

        for (Multiset.Entry<T> entry : votesDescending) {
            int count = entry.getCount();
            if (lastCount != -1 && lastCount != count) {
                break;
            }

            results.add(entry);
            lastCount = count;
        }

        return results;
    }

}