package com.github.jeuxjeux20.loupsgarous.extensibility;

import com.github.jeuxjeux20.loupsgarous.cards.LGCard;
import com.github.jeuxjeux20.loupsgarous.cards.composition.validation.CompositionValidator;
import com.github.jeuxjeux20.loupsgarous.cards.composition.validation.CompositionValidatorHandler;
import com.github.jeuxjeux20.loupsgarous.cards.revealers.CardRevealer;
import com.github.jeuxjeux20.loupsgarous.cards.revealers.CardRevealerHandler;
import com.github.jeuxjeux20.loupsgarous.chat.ChatChannelViewTransformer;
import com.github.jeuxjeux20.loupsgarous.chat.ChatChannel;
import com.github.jeuxjeux20.loupsgarous.descriptor.Descriptor;
import com.github.jeuxjeux20.loupsgarous.descriptor.DescriptorProcessor;
import com.github.jeuxjeux20.loupsgarous.inventory.InventoryItem;
import com.github.jeuxjeux20.loupsgarous.phases.RunnableLGPhase;
import com.github.jeuxjeux20.loupsgarous.phases.dusk.DuskAction;
import com.github.jeuxjeux20.loupsgarous.phases.overrides.PhaseOverride;
import com.github.jeuxjeux20.loupsgarous.scoreboard.ScoreboardComponent;
import com.github.jeuxjeux20.loupsgarous.tags.revealers.TagRevealer;
import com.github.jeuxjeux20.loupsgarous.tags.revealers.TagRevealerHandler;
import com.github.jeuxjeux20.loupsgarous.teams.revealers.TeamRevealer;
import com.github.jeuxjeux20.loupsgarous.teams.revealers.TeamRevealerHandler;
import com.github.jeuxjeux20.loupsgarous.winconditions.WinCondition;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

public final class LGExtensionPoints {
    public static final ExtensionPoint<Class<? extends RunnableLGPhase>> PHASES =
            new ExtensionPoint<>("phases", new TypeToken<Class<? extends RunnableLGPhase>>() {});

    public static final ExtensionPoint<PhaseOverride> PHASE_OVERRIDES =
            new ExtensionPoint<>("phase_overrides", PhaseOverride.class);

    public static final ExtensionPoint<Class<? extends DuskAction>> DUSK_ACTIONS =
            new ExtensionPoint<>("dusk_actions", new TypeToken<Class<? extends DuskAction>>() {});

    public static final ExtensionPoint<LGCard> CARDS =
            new ExtensionPoint<>("cards", LGCard.class);

    public static final HandledExtensionPoint<TeamRevealer, TeamRevealerHandler> TEAM_REVEALERS =
            new HandledExtensionPoint<>("team_revealers", TeamRevealer.class, TeamRevealerHandler.class);

    public static final HandledExtensionPoint<CardRevealer, CardRevealerHandler> CARD_REVEALERS =
            new HandledExtensionPoint<>("card_revealers", CardRevealer.class, CardRevealerHandler.class);

    public static final HandledExtensionPoint<TagRevealer, TagRevealerHandler> TAG_REVEALERS =
            new HandledExtensionPoint<>("tag_revealers", TagRevealer.class, TagRevealerHandler.class);

    public static final HandledExtensionPoint<CompositionValidator, CompositionValidatorHandler> COMPOSITION_VALIDATORS =
            new HandledExtensionPoint<>("composition_validators",
                    CompositionValidator.class, CompositionValidatorHandler.class);

    public static final ExtensionPoint<ScoreboardComponent> SCOREBOARD_COMPONENTS =
            new ExtensionPoint<>("scoreboard_components", ScoreboardComponent.class);

    public static final ExtensionPoint<InventoryItem> INVENTORY_ITEMS =
            new ExtensionPoint<>("inventory_items", InventoryItem.class);

    public static final ExtensionPoint<WinCondition> WIN_CONDITIONS =
            new ExtensionPoint<>("win_conditions", WinCondition.class);

    public static final ExtensionPoint<ChatChannelViewTransformer> CHANNEL_PROPERTIES_TRANSFORMERS =
            new ExtensionPoint<>("channel_properties_transformers", ChatChannelViewTransformer.class);

    public static final ExtensionPoint<ChatChannel> CHAT_CHANNELS =
            new ExtensionPoint<>("chat_channels", ChatChannel.class);

    private static final LoadingCache<Class<? extends Descriptor<?>>, ExtensionPoint<?>>
            DESCRIPTOR_PROCESSOR_CACHE =
            CacheBuilder.newBuilder().build(new ClassExtensionPointCacheLoader<Descriptor<?>>("processor") {
                @Override
                protected <C extends Descriptor<?>> TypeToken<?> getExtensionPointType(Class<C> clazz) {
                    return new TypeToken<DescriptorProcessor<C>>() {}
                            .where(new TypeParameter<C>() {}, clazz);
                }
            });

    private LGExtensionPoints() {
    }

    @SuppressWarnings("unchecked")
    public static <D extends Descriptor<?>>
    ExtensionPoint<DescriptorProcessor<D>> descriptorProcessors(Class<D> descriptorClass) {
        return (ExtensionPoint<DescriptorProcessor<D>>)
                DESCRIPTOR_PROCESSOR_CACHE.getUnchecked(descriptorClass);
    }
}