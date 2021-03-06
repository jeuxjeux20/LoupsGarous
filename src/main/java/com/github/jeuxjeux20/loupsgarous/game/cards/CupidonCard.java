package com.github.jeuxjeux20.loupsgarous.game.cards;

import com.github.jeuxjeux20.loupsgarous.game.cards.composition.validation.annotations.Unique;
import com.github.jeuxjeux20.loupsgarous.game.powers.CupidonPower;
import com.github.jeuxjeux20.loupsgarous.game.powers.LGPower;
import com.github.jeuxjeux20.loupsgarous.game.teams.LGTeam;
import com.github.jeuxjeux20.loupsgarous.game.teams.LGTeams;
import com.google.common.collect.ImmutableSet;
import me.lucko.helper.item.ItemStackBuilder;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

@Unique
public final class CupidonCard extends AbstractLGCard {
    public static final CupidonCard INSTANCE = new CupidonCard();

    private CupidonCard()  {}

    @Override
    protected LGTeam getMainTeam() {
        return LGTeams.VILLAGEOIS;
    }

    @Override
    public String getName() {
        return "Cupidon";
    }

    @Override
    public String getPluralName() {
        return "Cupidons";
    }

    @Override
    public boolean isFeminineName() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Il doit tuer les loups-garous. Au début de la partie, il peut former un couple avec les" +
               " personnes de son choix.";
    }

    @Override
    public ImmutableSet<LGPower> createPowers() {
        return ImmutableSet.of(new CupidonPower(this));
    }

    @Override
    public ItemStack createGuiItem() {
        return ItemStackBuilder.of(Material.WHITE_BANNER)
                .transformMeta(m -> {
                    BannerMeta bannerMeta = (BannerMeta) m;

                    bannerMeta.addPattern(new Pattern(DyeColor.PINK, PatternType.HALF_HORIZONTAL));
                    bannerMeta.addPattern(new Pattern(DyeColor.PINK, PatternType.RHOMBUS_MIDDLE));
                    bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.TRIANGLE_TOP));
                    bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP));
                    bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
                }).build();
    }
}
