package com.github.jeuxjeux20.loupsgarous.game.cards.revealers;

import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;

public interface CardRevealer {
    boolean willReveal(LGPlayer playerToReveal, LGPlayer target, LGGameOrchestrator orchestrator);
}
