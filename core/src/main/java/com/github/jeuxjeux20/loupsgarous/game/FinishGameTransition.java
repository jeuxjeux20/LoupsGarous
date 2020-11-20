package com.github.jeuxjeux20.loupsgarous.game;

import com.github.jeuxjeux20.loupsgarous.endings.LGEnding;
import com.google.common.base.MoreObjects;

public final class FinishGameTransition extends StateTransition {
    private final LGEnding ending;

    public FinishGameTransition(LGEnding ending) {
        this.ending = ending;
    }

    public LGEnding getEnding() {
        return ending;
    }

    @Override
    protected void addToString(MoreObjects.ToStringHelper helper) {
        helper.add("ending", ending);
    }
}
