package com.github.jeuxjeux20.loupsgarous.game.event.stage;

import com.github.jeuxjeux20.loupsgarous.game.stages.LGStage;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class LGStageStartedEvent extends LGStageEvent {
    private static final HandlerList handlerList = new HandlerList();

    public LGStageStartedEvent(LGStage stage) {
        super(stage);
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
