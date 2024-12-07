package de.teamlapen.vampirism.effects;

import de.teamlapen.vampirism.api.effects.IHiddenEffectInstance;
import de.teamlapen.vampirism.api.entity.effect.EffectInstanceWithSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Night vision effect for vampire players which is not displayed
 */
public class VampireNightVisionEffectInstance extends MobEffectInstance implements IHiddenEffectInstance {

    public VampireNightVisionEffectInstance() {
        super(MobEffects.NIGHT_VISION, -1, 0, false, false, false);
        setCurativeItems(new ArrayList<>());
        ((EffectInstanceWithSource) this).setSource(VampirismNightVisionPotion.ID);
    }

    @Override
    public void applyEffect(@NotNull LivingEntity entityIn) {
    }

    @Override
    public boolean equals(Object p_equals_1_) {
        return p_equals_1_ == this;
    }

    @NotNull
    @Override
    public String getDescriptionId() {
        return "effect.vampirism.night_vision";
    }

   /* @Override //TODO 1.19 readd
    public boolean isNoCounter() {
        return true;
    }*/


    @NotNull
    @Override
    public CompoundTag save(@NotNull CompoundTag nbt) {
        return nbt;
    }

    @Override
    public boolean tick(@NotNull LivingEntity entityIn, @NotNull Runnable p_76455_2_) {
        return true;
    }

    @Override
    public boolean update(@NotNull MobEffectInstance other) {
        //Don't change anything
        return false;
    }
}
