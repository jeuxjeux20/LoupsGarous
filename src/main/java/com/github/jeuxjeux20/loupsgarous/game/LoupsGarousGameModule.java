package com.github.jeuxjeux20.loupsgarous.game;

import com.github.jeuxjeux20.loupsgarous.game.cards.LGCardOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.cards.LGCardsModule;
import com.github.jeuxjeux20.loupsgarous.game.cards.MinecraftLGCardOrchestrator;
import com.github.jeuxjeux20.loupsgarous.game.chat.LGChatModule;
import com.github.jeuxjeux20.loupsgarous.game.commands.LGCommandsModule;
import com.github.jeuxjeux20.loupsgarous.game.listeners.LGListenersModule;
import com.github.jeuxjeux20.loupsgarous.game.stages.LGStagesModule;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public final class LoupsGarousGameModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new LGCardsModule());
        install(new LGListenersModule());
        install(new LGStagesModule());
        install(new LGCommandsModule());
        install(new LGChatModule());

        bind(LGGameManager.class).to(MinecraftLGGameManager.class);

        install(new FactoryModuleBuilder()
                .implement(LGGameOrchestrator.class, MinecraftLGGameOrchestrator.class)
                .build(LGGameOrchestrator.Factory.class));

        install(new FactoryModuleBuilder()
                .implement(LGCardOrchestrator.class, MinecraftLGCardOrchestrator.class)
                .build(LGCardOrchestrator.Factory.class));

        bind(LGScoreboardManager.class).to(MinecraftLGScoreboardManager.class);
        bind(LGActionBarManager.class).to(MinecraftLGActionBarManager.class);
    }
}
