package com.github.jeuxjeux20.loupsgarous.game.stages;

import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestratorComponent;
import com.github.jeuxjeux20.loupsgarous.game.LGGameState;
import com.github.jeuxjeux20.loupsgarous.game.stages.overrides.StageOverride;
import org.jetbrains.annotations.NotNull;

/**
 * This is a component of {@link LGGameOrchestrator} managing the {@linkplain LGStage stages}
 * of a game.
 */
public interface LGStagesOrchestrator extends LGGameOrchestratorComponent {
    /**
     * Adds a stage to the current game.
     * If the game has started, the stage added will be executed as soon as possible (LIFO),
     * else, it will be added at the end.
     *
     * @param stage the stage to add
     */
    void add(RunnableLGStage.Factory<?> stage);

    /**
     * Cancels the current stages, if any, and runs the next one.
     * <p>
     * Note that some {@link StageOverride}s might prevent the execution of the next stage,
     * for example, if the game is {@linkplain LGGameState#WAITING_FOR_PLAYERS waiting for players},
     * this method will ensure that the current stage is an instance of {@link GameStartStage}.
     */
    void next();

    /**
     * Gets the current stage, or an instance of {@link LGStage.Null} if there isn't any stage running
     * right now.
     *
     * @return the current stage, or {@link LGStage.Null}
     */
    @NotNull LGStage current();

    interface Factory {
        LGStagesOrchestrator create(LGGameOrchestrator gameOrchestrator);
    }
}
