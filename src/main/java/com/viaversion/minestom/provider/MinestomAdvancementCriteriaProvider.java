package com.viaversion.minestom.provider;

import com.viaversion.viabackwards.protocol.v1_20_2to1_20.provider.AdvancementCriteriaProvider;

/**
 * Minestom tracks every advancement through a single criterion named after the advancement itself.
 */
public final class MinestomAdvancementCriteriaProvider extends AdvancementCriteriaProvider {

    @Override
    public String[] getCriteria(final String advancementKey) {
        return new String[]{advancementKey};
    }
}
