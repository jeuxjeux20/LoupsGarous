package com.github.jeuxjeux20.loupsgarous.game.listeners;

import com.github.jeuxjeux20.loupsgarous.game.LGGameManager;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGGameTurnTime;
import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PreventNightPlayerMovementListener implements Listener {
    private final LGGameManager gameManager;

    @Inject
    PreventNightPlayerMovementListener(LGGameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        gameManager.getPlayerInGame(player).ifPresent(pg -> {
            LGGameOrchestrator orchestrator = pg.getOrchestrator();

            if (orchestrator.isGameRunning() && orchestrator.getTurn().getTime() == LGGameTurnTime.NIGHT) {
                event.setCancelled(true);
            }
        });
    }
}
