package com.github.jeuxjeux20.loupsgarous.lobby;

import com.github.jeuxjeux20.loupsgarous.cards.LGCard;
import com.github.jeuxjeux20.loupsgarous.cards.VillageoisCard;
import com.github.jeuxjeux20.loupsgarous.cards.composition.Composition;
import com.github.jeuxjeux20.loupsgarous.cards.composition.ImmutableComposition;
import com.github.jeuxjeux20.loupsgarous.cards.composition.validation.CompositionValidator;
import com.github.jeuxjeux20.loupsgarous.event.lobby.LGLobbyCompositionUpdateEvent;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import me.lucko.helper.Events;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import static com.github.jeuxjeux20.loupsgarous.extensibility.LGExtensionPoints.COMPOSITION_VALIDATORS;

public class MinecraftLGLobbyCompositionManager implements LGLobbyCompositionManager {
    private final LGGameOrchestrator orchestrator;
    private final CompositionValidator compositionValidator;

    private ImmutableComposition composition;
    private @Nullable CompositionValidator.Problem.Type worseCompositionProblemType;

    @Inject
    MinecraftLGLobbyCompositionManager(@Assisted LGGameOrchestrator orchestrator,
                                       @Assisted LGGameBootstrapData bootstrapData) {
        this.orchestrator = orchestrator;
        this.compositionValidator = orchestrator.bundle().handler(COMPOSITION_VALIDATORS);

        this.composition = new ImmutableComposition(bootstrapData.getComposition());

        updateCompositionProblemType();
    }

    @Override
    public ImmutableComposition get() {
        return composition;
    }

    @Override
    public void update(Composition composition) {
        Preconditions.checkArgument(!orchestrator.lobby().isLocked(),
                "Impossible to change the composition while lobby is locked.");

        HashMultiset<LGCard> cards = HashMultiset.create(composition.getContents());

        // Add some cards if there are not enough cards for the players we have.
        while (cards.size() < orchestrator.lobby().getSlotsTaken()) {
            cards.add(VillageoisCard.INSTANCE);
        }

        this.composition = new ImmutableComposition(cards);
        updateCompositionProblemType();
        Events.call(new LGLobbyCompositionUpdateEvent(orchestrator));
    }

    @Override
    public @Nullable CompositionValidator.Problem.Type getWorstProblemType() {
        return worseCompositionProblemType;
    }


    private void updateCompositionProblemType() {
        worseCompositionProblemType = compositionValidator.validate(composition).stream()
                .map(CompositionValidator.Problem::getType)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Override
    public boolean isValid() {
        return getWorstProblemType() != CompositionValidator.Problem.Type.IMPOSSIBLE;
    }
}
