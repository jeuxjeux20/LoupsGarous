package com.github.jeuxjeux20.loupsgarous.phases.descriptor;

import com.github.jeuxjeux20.loupsgarous.descriptor.AbstractDescriptorRegistry;
import com.github.jeuxjeux20.loupsgarous.game.OrchestratorScoped;
import com.github.jeuxjeux20.loupsgarous.phases.LGPhase;
import com.google.inject.Inject;

@OrchestratorScoped
class MinecraftLGPhaseDescriptorRegistry
        extends AbstractDescriptorRegistry<LGPhaseDescriptor, LGPhase>
        implements LGPhaseDescriptor.Registry {
    @Inject
    MinecraftLGPhaseDescriptorRegistry(LGPhaseDescriptor.Factory descriptorFactory) {
        super(descriptorFactory);
    }
}
