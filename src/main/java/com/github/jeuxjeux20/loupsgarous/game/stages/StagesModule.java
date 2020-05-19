package com.github.jeuxjeux20.loupsgarous.game.stages;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import org.jetbrains.annotations.Nullable;

import static com.github.jeuxjeux20.loupsgarous.util.TypeUtils.toLiteral;
import static com.github.jeuxjeux20.loupsgarous.util.TypeUtils.toToken;

public abstract class StagesModule extends AbstractModule {
    private @Nullable Multibinder<AsyncLGGameStage.Factory<?>> stagesFactoryBinder;

    @Override
    protected final void configure() {
        configureBindings();
        actualConfigureStages();
    }

    protected void configureBindings() {
    }

    protected void configureStages() {
    }

    private void actualConfigureStages() {
        stagesFactoryBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<AsyncLGGameStage.Factory<?>>() {
        });

        configureStages();
    }

    protected final void addStage(Class<? extends AsyncLGGameStage> stage) {
        addStage(TypeLiteral.get(stage));
    }

    protected final <T extends AsyncLGGameStage> void addStage(TypeLiteral<T> stage) {
        Preconditions.checkState(stagesFactoryBinder != null, "addStage can only be used inside configureStages()");

        TypeLiteral<AsyncLGGameStage.Factory<T>> factoryLiteral = createFactoryTypeLiteral(stage);

        install(new FactoryModuleBuilder()
                .build(factoryLiteral));

        stagesFactoryBinder.addBinding().to(factoryLiteral);
    }

    private <T extends AsyncLGGameStage>
    TypeLiteral<AsyncLGGameStage.Factory<T>> createFactoryTypeLiteral(TypeLiteral<T> literal) {
        return toLiteral(
                new TypeToken<AsyncLGGameStage.Factory<T>>() {}.where(new TypeParameter<T>() {}, toToken(literal))
        );
    }
}
