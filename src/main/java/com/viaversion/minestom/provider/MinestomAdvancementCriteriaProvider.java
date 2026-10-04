package com.viaversion.minestom.provider;

import com.viaversion.viabackwards.protocol.v1_20_2to1_20.provider.AdvancementCriteriaProvider;

public final class MinestomAdvancementCriteriaProvider extends AdvancementCriteriaProvider {
    @Override
    public String[] getCriteria(final String advancementKey) {
        return new String[]{advancementKey};
    }
}
