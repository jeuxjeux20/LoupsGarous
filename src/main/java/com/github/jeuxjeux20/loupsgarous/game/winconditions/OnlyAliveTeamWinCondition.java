package com.github.jeuxjeux20.loupsgarous.game.winconditions;

import com.github.jeuxjeux20.loupsgarous.game.LGGame;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;
import com.github.jeuxjeux20.loupsgarous.game.endings.LGEnding;
import com.github.jeuxjeux20.loupsgarous.game.teams.LGTeam;

import java.util.Optional;
import java.util.function.Supplier;

public class OnlyAliveTeamWinCondition implements WinCondition {
    private final LGTeam team;
    private final Supplier<LGEnding> endingSupplier;

    public OnlyAliveTeamWinCondition(LGTeam team, Supplier<LGEnding> endingSupplier) {
        this.team = team;
        this.endingSupplier = endingSupplier;
    }

    @Override
    public Optional<LGEnding> check(LGGame game) {
        if (game.getAlivePlayers().allMatch(this::allSameTeam)) {
            LGEnding ending = endingSupplier.get();
            return Optional.of(ending);
        }
        return Optional.empty();
    }

    private boolean allSameTeam(LGPlayer x) {
        return x.teams().get().stream().allMatch(n -> n.equals(team));
    }
}
