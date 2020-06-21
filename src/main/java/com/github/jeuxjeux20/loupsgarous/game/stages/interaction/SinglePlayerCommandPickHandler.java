package com.github.jeuxjeux20.loupsgarous.game.stages.interaction;

import com.github.jeuxjeux20.loupsgarous.LGMessages;
import com.github.jeuxjeux20.loupsgarous.game.LGGameOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.LGPlayer;
import com.github.jeuxjeux20.loupsgarous.util.Check;
import me.lucko.helper.command.context.CommandContext;
import me.lucko.helper.command.functional.FunctionalCommandBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SinglePlayerCommandPickHandler implements CommandPickHandler<PlayerPickable> {
    @Override
    public void configure(FunctionalCommandBuilder<Player> builder) {
        builder.assertUsage("<player>", "C'est pas comme ça que ça marche ! {usage}");
    }

    @Override
    public void pick(CommandContext<Player> context, LGPlayer player, PlayerPickable pickable, LGGameOrchestrator orchestrator) {
        String targetName = context.arg(0).value().orElseThrow(AssertionError::new);

        Optional<LGPlayer> maybeTarget = orchestrator.game().findByName(targetName);

        maybeTarget.ifPresent(target -> {
            Check check = pickable.canPick(player, target);

            if (check.isSuccess()) {
                pickable.pick(player, target);
            }
            else {
                context.reply(ChatColor.RED + check.getErrorMessage());
            }
        });

        if (!maybeTarget.isPresent())
            context.reply(LGMessages.cannotFindPlayer(targetName));
    }
}
