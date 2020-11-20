package com.github.jeuxjeux20.loupsgarous.kill;

import com.github.jeuxjeux20.loupsgarous.event.LGKillEvent;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.OrchestratedLGPlayer;
import com.google.common.base.Preconditions;
import me.lucko.helper.Events;

import java.util.Collection;

/**
 * Internal class to kill players. This sounds ASTONISHINGLY fine.
 */
final class PlayerKiller {
    private final LGGameOrchestrator orchestrator;

    PlayerKiller(LGGameOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    void applyKills(Collection<LGKill> kills) {
        for (LGKill kill : kills) {
            OrchestratedLGPlayer victim = (OrchestratedLGPlayer) orchestrator.ensurePresent(kill.getVictim());

            Preconditions.checkArgument(victim.isAlive(),
                    "Cannot kill player " + victim.getName() + " because they are dead.");

            victim.dieSilently();
        }

        Events.call(new LGKillEvent(orchestrator, kills));
    }
}
