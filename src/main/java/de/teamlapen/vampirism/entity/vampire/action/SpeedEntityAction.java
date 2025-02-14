package de.teamlapen.vampirism.entity.vampire.action;

import de.teamlapen.vampirism.api.entity.EntityClassType;
import de.teamlapen.vampirism.api.entity.actions.EntityActionTier;
import de.teamlapen.vampirism.api.entity.actions.IEntityActionUser;
import de.teamlapen.vampirism.api.entity.actions.ILastingAction;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.core.ModParticles;
import de.teamlapen.vampirism.core.ModRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SpeedEntityAction<T extends PathfinderMob & IEntityActionUser> extends VampireEntityAction<T> implements ILastingAction<T> {

    public SpeedEntityAction(@NotNull EntityActionTier tier, EntityClassType... param) {
        super(tier, param);
    }

    @Override
    public void activate(T entity) {
    }

    @Override
    public void deactivate(@NotNull T entity) {
        entity.asEntity().getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ModRegistries.ENTITY_ACTIONS.getKey(this));
    }

    @Override
    public int getCooldown(int level) {
        return VampirismConfig.BALANCE.eaSpeedCooldown.get() * 20;
    }

    @Override
    public int getDuration(int level) {
        return VampirismConfig.BALANCE.eaSpeedDuration.get() * 20;
    }

    @Override
    public int getWeight(@NotNull PathfinderMob entity) {
        if (entity.getTarget() == null) return 0;
        double distanceToTarget = new Vec3(entity.getX(), entity.getY(), entity.getZ()).subtract(entity.getTarget().getX(), entity.getTarget().getY(), entity.getTarget().getZ()).length();
        if (distanceToTarget > 10) {
            return 3;
        } else if (distanceToTarget > 5) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public void onUpdate(@NotNull T entity, int duration) {
        ResourceLocation key = ModRegistries.ENTITY_ACTIONS.getKey(this);
        if (!entity.asEntity().getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(key)) {
            entity.asEntity().getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier(key, VampirismConfig.BALANCE.eaSpeedAmount.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (duration % 5 == 0) {
            double maxDist = 0.5D;
            ModParticles.spawnParticlesServer(entity.getCommandSenderWorld(), ParticleTypes.CLOUD, entity.getX() + (entity.getRandom().nextDouble() * maxDist) - maxDist / 2, entity.getY() + 0.1, entity.getZ() + (entity.getRandom().nextDouble() * maxDist) - maxDist / 2, 3, 0.3f, 0.3f, 0.3f, 0.02f);
        }
    }
}
