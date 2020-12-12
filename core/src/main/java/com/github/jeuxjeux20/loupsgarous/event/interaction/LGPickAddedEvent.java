package com.github.jeuxjeux20.loupsgarous.event.interaction;

import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.interaction.PickData;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class LGPickAddedEvent extends LGPickEvent {
    private static final HandlerList handlerList = new HandlerList();

    public LGPickAddedEvent(LGGameOrchestrator orchestrator, PickData<?> pickData) {
        super(orchestrator, pickData);
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}