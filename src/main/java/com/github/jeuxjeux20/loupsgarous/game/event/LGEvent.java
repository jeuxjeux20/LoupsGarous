package com.github.jeuxjeux20.loupsgarous.game.event;

import com.github.jeuxjeux20.loupsgarous.game.LGGame;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import org.bukkit.event.Event;

public abstract class LGEvent extends Event {
    private final LGGameOrchestrator orchestrator;

    public LGEvent(LGGameOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public LGGameOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public LGGame getGame() {
        return orchestrator.game();
    }
}
