package com.github.jeuxjeux20.loupsgarous.atmosphere.listeners;

import com.github.jeuxjeux20.loupsgarous.event.LGKillEvent;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;
import com.github.jeuxjeux20.loupsgarous.kill.LGKill;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerDiesOnKillListener implements Listener {
    @EventHandler
    public void onLGKill(LGKillEvent event) {
        World world = event.getOrchestrator().world();
        for (LGKill kill : event.getKills()) {
            LGPlayer victim = kill.getVictim();

            Location location = victim.minecraft()
                    .map(Entity::getLocation)
                    .orElse(world.getSpawnLocation());
            world.strikeLightningEffect(location);

            victim.minecraft(player -> player.setHealth(0));
        }
    }
}