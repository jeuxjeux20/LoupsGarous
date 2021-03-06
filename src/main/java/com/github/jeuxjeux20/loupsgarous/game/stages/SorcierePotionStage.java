package com.github.jeuxjeux20.loupsgarous.game.stages;

import com.github.jeuxjeux20.loupsgarous.ComponentStyles;
import com.github.jeuxjeux20.loupsgarous.ComponentTemplates;
import com.github.jeuxjeux20.loupsgarous.LGSoundStuff;
import com.github.jeuxjeux20.loupsgarous.game.*;
import com.github.jeuxjeux20.loupsgarous.game.interaction.AbstractPlayerPick;
import com.github.jeuxjeux20.loupsgarous.game.interaction.InteractableRegisterer;
import com.github.jeuxjeux20.loupsgarous.game.interaction.LGInteractableKeys;
import com.github.jeuxjeux20.loupsgarous.game.interaction.PickableConditions;
import com.github.jeuxjeux20.loupsgarous.game.interaction.condition.FunctionalPickConditions;
import com.github.jeuxjeux20.loupsgarous.game.interaction.condition.PickConditions;
import com.github.jeuxjeux20.loupsgarous.game.kill.LGKill;
import com.github.jeuxjeux20.loupsgarous.game.kill.causes.NightKillCause;
import com.github.jeuxjeux20.loupsgarous.game.powers.SorcierePower;
import com.github.jeuxjeux20.loupsgarous.util.Check;
import com.google.inject.Inject;
import me.lucko.helper.text.Text;
import me.lucko.helper.text.TextComponent;
import me.lucko.helper.text.event.ClickEvent;
import me.lucko.helper.text.event.HoverEvent;
import me.lucko.helper.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import static com.github.jeuxjeux20.loupsgarous.LGChatStuff.*;
import static me.lucko.helper.text.format.TextColor.*;
import static me.lucko.helper.text.format.TextDecoration.BOLD;

@StageInfo(
        name = "Sorcière",
        title = "La sorcière va utiliser ses potions..."
)
public final class SorcierePotionStage extends CountdownLGStage {
    private final SorciereHeal heal;
    private final SorciereKill kill;
    private final BaseConditions baseConditions;

    @Inject
    SorcierePotionStage(LGGameOrchestrator orchestrator, BaseConditions baseConditions,
                        InteractableRegisterer<SorciereHeal> heal,
                        InteractableRegisterer<SorciereKill> kill) {
        super(orchestrator);
        this.baseConditions = baseConditions;

        this.heal = heal.as(LGInteractableKeys.HEAL).boundWith(this);
        this.kill = kill.as(LGInteractableKeys.KILL).boundWith(this);
    }

    @Override
    protected Countdown createCountdown() {
        return Countdown.of(30);
    }

    @Override
    public boolean shouldRun() {
        return orchestrator.game().getPlayers().stream().anyMatch(Check.predicate(baseConditions::canAct)) &&
               orchestrator.game().getTurn().getTime() == LGGameTurnTime.NIGHT;
    }

    @Override
    protected void start() {
        orchestrator.game().getPlayers().stream()
                .filter(Check.predicate(baseConditions::canAct))
                .forEach(this::sendNotification);
    }

    public SorciereHeal heals() {
        return heal;
    }

    public SorciereKill kills() {
        return kill;
    }

    private void sendNotification(LGPlayer player) {
        player.getMinecraftPlayer().ifPresent(minecraftPlayer -> sendNotification(player, minecraftPlayer));
    }

    // This is really long.
    private void sendNotification(LGPlayer player, Player minecraftPlayer) {
        SorcierePower power = player.powers().getOrThrow(SorcierePower.class);

        TextComponent.Builder builder = TextComponent.builder("")
                .append(TextComponent.of("==== Vous êtes la sorcière !")
                        .color(TextColor.LIGHT_PURPLE)
                        .decoration(BOLD, true));

        if (power.hasHealPotion()) {
            Set<LGKill> pendingKills = orchestrator.kills().pending().getAll();

            builder.append(TextComponent.of("\n" + HEAL_SYMBOL + " ").color(GREEN));

            if (pendingKills.isEmpty()) {
                builder.append(TextComponent.of("Personne ne va mourir !").color(WHITE).decoration(BOLD, true));
            } else {
                int i = 0;
                int endSeparator = pendingKills.size() - 2;

                for (Iterator<LGKill> iterator = pendingKills.iterator(); iterator.hasNext(); i++) {
                    LGKill kill = iterator.next();
                    String victimName = kill.getVictim().getName();

                    builder.append(TextComponent.of(victimName + " ", RED, Collections.singleton(BOLD)));

                    String command = "/lgheal " + victimName;

                    TextComponent hoverHeal = TextComponent.of("Cliquez ici pour soigner " + victimName + " !");
                    TextComponent healButton = TextComponent.of("[Soigner]")
                            .mergeStyle(ComponentStyles.CLICKABLE)
                            .color(GREEN)
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverHeal))
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

                    builder.append(healButton);

                    if (iterator.hasNext()) {
                        builder.append(TextComponent.of(i == endSeparator ? " et " : ", ", WHITE));
                    }
                }

                String finalText = pendingKills.size() > 1 ? " vont mourir cette nuit !" : " va mourir cette nuit !";
                builder.append(TextComponent.of(finalText, WHITE));
            }
        }

        if (power.hasKillPotion()) {
            TextComponent poison = TextComponent.of("\n" + SKULL_SYMBOL + " Vous avez votre potion de poison !")
                    .color(RED);

            TextComponent.Builder tipBuilder = TextComponent.builder("\n").mergeStyle(ComponentStyles.TIP);

            tipBuilder.append(TextComponent.of("Faites "))
                    .append(ComponentTemplates.command("/lgkill", "<joueur>"))
                    .append(TextComponent.of(" pour l'utiliser et tuer quelqu'un !"));

            builder.append(poison).append(tipBuilder.build());
        }

        if (!power.hasKillPotion() && !power.hasHealPotion()) {
            builder.append(TextComponent.of("\nVous n'avez plus de potions.").color(YELLOW));
        }

        builder.append(TextComponent.of("\n====").color(LIGHT_PURPLE).decoration(BOLD, true));

        TextComponent message = builder.build();
        Text.sendMessage(minecraftPlayer, message);

        LGSoundStuff.ding(minecraftPlayer);
    }

    // PickData stuff

    @OrchestratorScoped
    private static final class BaseConditions {
        public Check canAct(LGPlayer player) {
            return Check.ensure(player.isAlive(), "Vous êtes mort !")
                    .and(player.powers().has(SorcierePower.class), "Vous n'êtes pas une sorcière !");
        }
    }

    private static abstract class SorcierePick extends AbstractPlayerPick {
        private final BaseConditions baseConditions;

        protected SorcierePick(LGGameOrchestrator orchestrator, BaseConditions baseConditions) {
            super(orchestrator);
            this.baseConditions = baseConditions;
        }

        @Override
        protected final PickConditions<LGPlayer> pickConditions() {
            return FunctionalPickConditions.<LGPlayer>builder()
                    .ensurePicker(baseConditions::canAct)
                    .use(powerConditions())
                    .build();
        }

        protected abstract PickConditions<LGPlayer> powerConditions();
    }

    @OrchestratorScoped
    public static class SorciereHeal extends SorcierePick {
        @Inject
        SorciereHeal(LGGameOrchestrator orchestrator, BaseConditions baseConditions) {
            super(orchestrator, baseConditions);
        }

        @Override
        protected PickConditions<LGPlayer> powerConditions() {
            return FunctionalPickConditions.<LGPlayer>builder()
                    .ensurePicker(this::hasHealPotion, "Vous avez déjà utilisé votre potion de soin !")
                    .ensureTarget(LGPlayer::willDie, "Ce joueur ne va pas mourir ce tour ci.")
                    .build();
        }

        @Override
        protected void safePick(LGPlayer healer, LGPlayer target) {
            SorcierePower power = healer.powers().getOrThrow(SorcierePower.class);

            power.useHealPotion();
            target.cancelFutureDeath();

            healer.getMinecraftPlayer().ifPresent(player ->
                    player.sendMessage(
                            ChatColor.GREEN + " Glou glou, la potion guérit " + player(target.getName()) +
                            ChatColor.GREEN + " qui restera en vie cette nuit."
                    )
            );
        }

        private boolean hasHealPotion(LGPlayer player) {
            return player.powers().getOrThrow(SorcierePower.class).hasHealPotion();
        }
    }

    @OrchestratorScoped
    public static class SorciereKill extends SorcierePick {
        @Inject
        SorciereKill(LGGameOrchestrator orchestrator, BaseConditions baseConditions) {
            super(orchestrator, baseConditions);
        }

        @Override
        protected PickConditions<LGPlayer> powerConditions() {
            return FunctionalPickConditions.<LGPlayer>builder()
                    .apply(PickableConditions::ensureKillTargetAlive)
                    .ensurePicker(this::hasKillPotion, "Vous avez déjà utilisé votre potion de mort !")
                    .ensureTarget(LGPlayer::willNotDie, "Ce joueur va déjà mourir !")
                    .build();
        }

        @Override
        protected void safePick(LGPlayer killer, LGPlayer target) {
            SorcierePower power = killer.powers().getOrThrow(SorcierePower.class);

            power.useKillPotion();
            target.dieLater(NightKillCause.INSTANCE);

            killer.getMinecraftPlayer().ifPresent(player ->
                    player.sendMessage(
                            ChatColor.RED + " Glou glou, la potion empoisonne " + player(target.getName()) +
                            ChatColor.RED + " qui va mourir cette nuit."
                    )
            );
        }

        private boolean hasKillPotion(LGPlayer player) {
            return player.powers().getOrThrow(SorcierePower.class).hasKillPotion();
        }
    }
}
