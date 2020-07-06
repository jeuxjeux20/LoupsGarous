package com.github.jeuxjeux20.loupsgarous.game.chat;

import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGGameTurnTime;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;

public class DayChatChannel implements LGChatChannel {
    @Override
    public String getName() {
        return "Jour";
    }

    @Override
    public boolean isNameDisplayed() {
        return false;
    }

    @Override
    public boolean canBeUsedByPlayer(LGGameOrchestrator orchestrator) {
        return orchestrator.isGameRunning() &&
               orchestrator.game().getTurn().getTime() == LGGameTurnTime.DAY;
    }

    @Override
    public boolean areMessagesVisibleTo(LGPlayer recipient, LGGameOrchestrator orchestrator) {
        return true;
    }

    @Override
    public boolean canTalk(LGPlayer sender, LGGameOrchestrator orchestrator) {
        return sender.isAlive();
    }
}
