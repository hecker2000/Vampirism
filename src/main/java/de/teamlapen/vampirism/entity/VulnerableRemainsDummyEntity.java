package de.teamlapen.vampirism.entity;

import de.teamlapen.vampirism.api.entity.IEntityLeader;
import de.teamlapen.vampirism.blockentity.VulnerableRemainsBlockEntity;
import de.teamlapen.vampirism.core.ModBlocks;
import de.teamlapen.vampirism.core.ModDamageTypes;
import de.teamlapen.vampirism.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class VulnerableRemainsDummyEntity extends LivingEntity implements IEntityLeader, IRemainsEntity {

    private BlockPos ownerPos = null;

    public VulnerableRemainsDummyEntity(EntityType<VulnerableRemainsDummyEntity> type, Level pLevel) {
        super(type, pLevel);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    @Override
    public float getHealth() {
        return getTile().map(VulnerableRemainsBlockEntity::getHealth).orElse(1);
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return Vec3.ZERO;
    }

    @Override
    public void setDeltaMovement(@NotNull Vec3 pMotion) {
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        getTile().ifPresent(vr -> {
            vr.onDamageDealt(pSource, pAmount);
        });
        return false;
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource pSource) {
        return this.isRemoved() || pSource.is(DamageTypes.ON_FIRE) || pSource.is(ModDamageTypes.HOLY_WATER) || pSource.is(DamageTypes.FREEZE);
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot pSlot, @NotNull ItemStack pStack) {

    }

    @Override
    public boolean isPickable() {
        return true; //This ensures the entity can be targeted by client which is required to be able to hit it
    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.LEFT;
    }


    @Override
    public void push(@NotNull Entity pEntity) {
    }

    public void setOwnerLocation(BlockPos pos) {
        this.ownerPos = pos;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            BlockState block = this.level().getBlockState(ownerPos);
            if(this.ownerPos == null || !block.is(ModBlocks.ACTIVE_VULNERABLE_REMAINS.get()) || block.is(ModBlocks.INCAPACITATED_VULNERABLE_REMAINS.get())) {
                if (block.is(ModBlocks.INCAPACITATED_VULNERABLE_REMAINS.get())) {
                    this.getPassengers().forEach(s -> s.setRemoved(RemovalReason.DISCARDED));
                }
                this.remove(RemovalReason.DISCARDED);
            } else if (level().getGameTime() % 400 == 16) {
                spawnDefender();
            }
        }
    }

    public void spawnDefender() {
        BlockPos pos = this.ownerPos;

        List<Direction> directionStream = Arrays.stream(Direction.values()).filter(l -> {
            var block = this.level().getBlockState(pos.relative(l)).canBeReplaced();
            return block && !hasDefender(l);
        }).toList();
        if (!directionStream.isEmpty()) {
            spawnDefender(directionStream.get(this.random.nextInt(directionStream.size())));
        }

    }

    private boolean hasDefender(Direction direction) {
        return getPassengers().stream().anyMatch(entity -> entity instanceof RemainsDefenderEntity defender && defender.getAttachFace().getOpposite() == direction);
    }

    public void spawnDefender(Direction direction) {
        RemainsDefenderEntity defender = ModEntities.REMAINS_DEFENDER.get().create(this.level());
        getTile().map(BlockEntity::getBlockPos).ifPresent(pos -> {
            defender.setPos(Vec3.atBottomCenterOf(pos.relative(direction)));
            defender.setAttachFace(direction.getOpposite());
            level().addFreshEntity(defender);
            defender.startRiding(this);
            defender.setYRot(0);
            defender.yHeadRot = getYRot();
            defender.setOldPosAndRot();
        });
    }

    @Override
    protected boolean canAddPassenger(Entity pPassenger) {
        return pPassenger instanceof RemainsDefenderEntity && getPassengers().size() < 6;
    }

    public Optional<VulnerableRemainsBlockEntity> getTile() {
        if (ownerPos != null) {
            if (this.level().getBlockEntity(ownerPos) instanceof VulnerableRemainsBlockEntity vr) {
                return Optional.of(vr);
            }
        }
        return Optional.empty();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        pCompound.putIntArray("ownerPos", new int[]{this.ownerPos.getX(), this.ownerPos.getY(), this.ownerPos.getZ()});
    }

    @Override
    protected Entity.@NotNull MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        if (pCompound.contains("ownerPos", Tag.TAG_INT_ARRAY)) {
            int[] pos = pCompound.getIntArray("ownerPos");
            this.ownerPos = new BlockPos(pos[0], pos[1], pos[2]);
        }
    }

    private int followerCount = 0;

    @Override
    public void decreaseFollowerCount() {
        followerCount--;
    }

    @Override
    public int getFollowingCount() {
        return this.followerCount;
    }

    @Override
    public int getMaxFollowerCount() {
        return isAlive() ? Integer.MAX_VALUE : 0;
    }

    @Override
    public LivingEntity getRepresentingEntity() {
        return this;
    }

    @Override
    public boolean increaseFollowerCount() {
        followerCount++;
        return true;
    }
}
