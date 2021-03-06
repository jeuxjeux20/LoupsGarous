package com.github.jeuxjeux20.loupsgarous.game.cards.composition.validation;

public final class LGCompositionValidatorsModule extends CompositionValidatorsModule {
    @Override
    protected void configureBindings() {
        bind(CompositionValidator.class).to(CompositionValidatorAggregator.class);
    }

    @Override
    protected void configureCompositionValidators() {
        addCompositionValidator(PossibleCouplesCupidonCompositionValidator.class);
        addCompositionValidator(UniqueCardCompositionValidator.class);
        addCompositionValidator(MultipleTeamsCompositionValidator.class);
    }
}
