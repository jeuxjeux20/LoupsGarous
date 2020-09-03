package com.github.jeuxjeux20.loupsgarous.phases;

import com.github.jeuxjeux20.loupsgarous.game.AbstractOrchestratorComponent;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGGameState;
import com.github.jeuxjeux20.loupsgarous.phases.descriptor.LGPhaseDescriptor;
import com.github.jeuxjeux20.loupsgarous.phases.overrides.PhaseOverride;
import com.github.jeuxjeux20.loupsgarous.util.FutureExceptionUtils;
import com.google.inject.Inject;
import me.lucko.helper.terminable.Terminable;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.github.jeuxjeux20.loupsgarous.extensibility.LGExtensionPoints.PHASES;
import static com.github.jeuxjeux20.loupsgarous.extensibility.LGExtensionPoints.PHASE_OVERRIDES;

public class LGPhasesOrchestrator extends AbstractOrchestratorComponent {
    private LinkedList<RunnableLGPhase.Factory<?>> phases;
    private final LGPhaseDescriptor.Registry descriptorRegistry;
    private ListIterator<RunnableLGPhase.Factory<?>> phaseIterator;
    private @Nullable RunnableLGPhase currentPhase;
    private final Set<PhaseOverride> phaseOverrides;
    private final Logger logger;

    @Inject
    LGPhasesOrchestrator(LGGameOrchestrator orchestrator,
                         LGPhaseDescriptor.Registry descriptorRegistry) {
        super(orchestrator);
        this.phases = getPhaseFactories();
        this.descriptorRegistry = descriptorRegistry;
        this.phaseIterator = this.phases.listIterator();
        this.phaseOverrides = orchestrator.getGameBundle().contents(PHASE_OVERRIDES);
        this.logger = orchestrator.logger();

        bind(new CurrentPhaseTerminable());
    }

    private LinkedList<RunnableLGPhase.Factory<?>> getPhaseFactories() {
        return orchestrator.getGameBundle().contents(PHASES).stream()
                .map(c -> (RunnableLGPhase.Factory<?>) o -> o.resolve(c))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Insert the given phase factory to the current game that will be created and run as soon as
     * possible (LIFO).
     *
     * @param phaseFactory the phase factory to insert
     */
    public void insert(RunnableLGPhase.Factory<?> phaseFactory) {
        phaseIterator.add(phaseFactory);
        phaseIterator.previous();
    }

    /**
     * Cancels the current phase, if any, and runs the next one.
     * <p>
     * Note that some {@link PhaseOverride}s might prevent the execution of the next phase, for
     * example, if the game is in a {@linkplain LGGameState#LOBBY lobby state},
     * this method will ensure that the current phase is an instance of {@link LobbyPhase}.
     */
    public void next() {
        if (callPhaseOverride()) return;

        if (phases.size() == 0)
            throw new IllegalStateException("No phases have been found.");

        if (!phaseIterator.hasNext())
            phaseIterator = phases.listIterator(); // Reset the iterator

        RunnableLGPhase.Factory<?> factory = phaseIterator.next();
        RunnableLGPhase phase = factory.create(orchestrator);
        LGPhaseDescriptor descriptor = descriptorRegistry.get(phase.getClass());

        if (descriptor.isTemporary())
            phaseIterator.remove();

        if (phase.shouldRun()) {
            runPhase(phase).thenRun(this::next).exceptionally(this::handlePostPhaseException);
        } else {
            // Close the phase because we didn't run it.
            //
            // NOTE: That's one of the problems of having close() and shouldRun() together,
            // you might forget to close it when it shouldn't run, and also create
            // unnecessary objects in the constructor.
            // I can't really think of an alternative that is nearly as pleasant to use
            // as the shouldRun() method.
            // While something such as PhaseOverride somewhat fixes the whole issue,
            // requiring an extra class just for a condition seems really annoying.
            // For now, this works. Let's not complain :D
            phase.closeAndReportException();
            next();
        }
    }

    /**
     * Gets the current phase, or an instance of {@link LGPhase.Null} if there isn't any phase
     * running right now.
     *
     * @return the current phase, or {@link LGPhase.Null}
     */
    public LGPhase current() {
        return currentPhase == null ? new LGPhase.Null(orchestrator) : currentPhase;
    }

    /**
     * Returns the descriptor registry.
     *
     * @return the descriptor registry
     */
    public LGPhaseDescriptor.Registry descriptors() {
        return descriptorRegistry;
    }

    private boolean callPhaseOverride() {
        Optional<PhaseOverride> activePhaseOverride = phaseOverrides.stream()
                .filter(x -> x.shouldOverride(orchestrator))
                .findFirst();

        activePhaseOverride.ifPresent(phaseOverride -> {
            if (phaseOverride.getPhaseClass().isInstance(currentPhase)) return;

            RunnableLGPhase phase = phaseOverride.getPhaseFactory().create(orchestrator);

            runPhase(phase).exceptionally(this::handlePostPhaseException);
        });

        return activePhaseOverride.isPresent();
    }

    private CompletableFuture<Void> runPhase(RunnableLGPhase phase) {
        updatePhase(phase);

        return phase.run().exceptionally(this::handlePhaseException);
    }

    private void updatePhase(RunnableLGPhase phase) {
        if (currentPhase != null && !currentPhase.isClosed())
            currentPhase.closeAndReportException();

        currentPhase = phase;
    }

    private Void handlePhaseException(Throwable ex) {
        if (FutureExceptionUtils.isCancellation(ex)) {
            // That's cancelled, rethrow to avoid executing further actions.

            throw FutureExceptionUtils.asCompletionException(ex);
        } else {
            // If something wrong happens, let's just log and continue.
            // We still want the game to continue!

            logger.log(Level.SEVERE, "Unhandled exception while running phase: " + currentPhase, ex);
            return null;
        }
    }

    private Void handlePostPhaseException(Throwable ex) {
        if (!FutureExceptionUtils.isCancellation(ex)) {
            logger.log(Level.SEVERE, "Unhandled exception while the game was running the next phase.", ex);
        }
        return null;
    }

    @Override
    public LGGameOrchestrator gameOrchestrator() {
        return orchestrator;
    }

    private final class CurrentPhaseTerminable implements Terminable {
        @Override
        public void close() {
            if (currentPhase != null && !currentPhase.isClosed())
                currentPhase.closeAndReportException();
        }
    }
}
