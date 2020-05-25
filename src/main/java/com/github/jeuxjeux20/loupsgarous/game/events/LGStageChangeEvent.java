package com.github.jeuxjeux20.loupsgarous.game.events;

import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.stages.LGGameStage;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class LGStageChangeEvent extends LGEvent {
    private static final HandlerList handlerList = new HandlerList();
    private final LGGameStage stage;

    public LGStageChangeEvent(LGGameOrchestrator orchestrator, LGGameStage stage) {
        super(orchestrator);
        this.stage = stage;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public LGGameStage getStage() {
        return stage;
    }
}