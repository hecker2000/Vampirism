package de.teamlapen.vampirism.items;

import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.core.tags.ModFactionTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class VampireRefinementItem extends RefinementItem {

    public VampireRefinementItem(@NotNull Properties properties, AccessorySlotType type) {
        super(properties, type);
    }

    @Override
    public @NotNull TagKey<IFaction<?>> getExclusiveFaction(@NotNull ItemStack stack) {
        return ModFactionTags.IS_VAMPIRE;
    }

    public static @NotNull RefinementItem getItemForType(@NotNull AccessorySlotType type) {
        return switch (type) {
            case AMULET -> ModItems.AMULET.get();
            case RING -> ModItems.RING.get();
            default -> ModItems.OBI_BELT.get();
        };
    }
}
