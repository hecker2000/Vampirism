package de.teamlapen.vampirism.api.event;

import de.teamlapen.vampirism.api.entity.IVillageCaptureEntity;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.world.ITotem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class VampirismVillageEvent extends Event {


    protected final ITotem totem;

    public VampirismVillageEvent(ITotem totem) {
        this.totem = totem;
    }

    @Nullable
    public IFaction getCapturingFaction() {
        return this.totem.getCapturingFaction();
    }

    @Nullable
    public IFaction getControllingFaction(){
        return this.totem.getControllingFaction();
    }

    @Nonnull
    public AxisAlignedBB getVillageArea() {
        return totem.getVillageArea();
    }

    @Nonnull
    public AxisAlignedBB getVillageAreaReduced() {
        return totem.getVillageAreaReduced();
    }

    /**
     * Fired when a new villager will be spawned.
     * Deny if none should spawn, allow and set villager if you own villager should spawn.
     * Default spawns a standard villager.
     * <p>
     * Your villager should have the position set, but should not be spawned in the world.
     * <p>
     * The {@linkplain #willBeConverted} field tells if the villager will be converted to an aggressive version. You can change this.
     * DON'T set an aggressive villager even if this field is true
     */
    @HasResult
    public static class SpawnNewVillager extends VampirismVillageEvent {

        /**
         * Random existing villager in totemTile
         * Used as a "seed" villager to get a valid spawn point.
         */
        private final @Nullable
        MobEntity oldEntity;
        private @Nullable
        VillagerEntity newVillager;
        private boolean replace;
        private boolean willBeConverted;

        public SpawnNewVillager(ITotem totem, @Nullable MobEntity oldEntity, @Nonnull VillagerEntity newVillager, boolean replace, boolean willBeConverted) {
            super(totem);
            this.oldEntity = oldEntity;
            this.newVillager = newVillager;
            this.replace = replace;
            this.willBeConverted = willBeConverted;
        }

        @Nullable
        public VillagerEntity getNewVillager() {
            return newVillager;
        }

        /**
         * The villager that should be spawned
         * The position should already be set
         *
         * @param newVillager
         */
        public void setNewVillager(@Nullable VillagerEntity newVillager) {
            this.newVillager = newVillager;
        }

        /**
         * A random existing villager which can be used as a seed (e.g. for the position)
         *
         * @return
         */
        @Nullable
        public MobEntity getOldEntity() {
            return oldEntity;
        }

        /**
         * If the villager will be converted afterwards (e.g. to a vampire version)
         * Default: Is sometimes true if the village is not controlled by hunters. Can be overridden by {@link #setWillBeConverted}
         */
        public boolean isWillBeConverted() {
            return willBeConverted;
        }

        /**
         * @return if the {@link #oldEntity} will be replaced by {@link #newVillager}
         */
        public boolean isReplace() {
            return replace;
        }

        /**
         * Overwrite the default value.
         */
        public void setWillBeConverted(boolean willBeConverted) {
            this.willBeConverted = willBeConverted;
        }

        /**
         * Faction that owns the village
         */
        public IFaction<?> getFaction() {
            return this.totem.getControllingFaction();
        }
    }

    /**
     * Fired when a normal villager should be converted to angry villager.
     * You can set a custom replacement and cancel this event to make it take effect.
     * The {@link #oldVillager} is probably not added to a world
     */
    @Cancelable
    public static class MakeAggressive extends VampirismVillageEvent {

        private final VillagerEntity oldVillager;
        private @Nullable
        IVillageCaptureEntity captureVillager;

        public MakeAggressive(ITotem totem, @Nonnull VillagerEntity villager) {
            super(totem);
            this.oldVillager = villager;
        }

        @Nullable
        public IVillageCaptureEntity getAggressiveVillager() {
            return captureVillager;
        }

        /**
         * Set the aggressive version of the old villager.
         * Event has to be canceled for this to take effect
         */
        public void setAggressiveVillager(@Nullable IVillageCaptureEntity captureVillager) {
            this.captureVillager = captureVillager;
        }

        /**
         * @return The villager which should be made aggressive
         */
        public VillagerEntity getOldVillager() {
            return oldVillager;
        }
    }

    /**
     * Fired when the Capture process is finished the the Villager should be affected by the faction change
     * if result is {@link Result#DENY} the Vanilla code is skipped
     */
    @HasResult
    public static class VillagerCaptureFinish extends VampirismVillageEvent {

        private final @Nonnull
        List<VillagerEntity> villager;
        private final boolean forced;

        public VillagerCaptureFinish(ITotem totem, @Nonnull List<VillagerEntity> villagerIn, boolean forced) {
            super(totem);
            villager = villagerIn;
            this.forced = forced;
        }

        /**
         * @returns all {@link VillagerEntity} that are in the village boundingBox
         */
        @Nonnull
        public List<VillagerEntity> getVillager() {
            return villager;
        }

        public void updateTrainer(boolean toDummy) {
            this.totem.updateTrainer(toDummy);
        }

        public boolean isForced() {
            return forced;
        }
    }

    /**
     * Fired when a new Capture Entity should be spawned
     */
    public static class SpawnCaptureEntity extends VampirismVillageEvent {

        private EntityType<? extends MobEntity> entity;

        public SpawnCaptureEntity(ITotem totem) {
            super(totem);
        }

        /**
         * set the Entity to spawn
         *
         * @param {@link ResourceLocation}
         *               of the entity
         */
        public void setEntity(EntityType<? extends MobEntity> entity) {
            this.entity = entity;
        }

        /**
         * @returns {@link ResourceLocation} of the capture entity which should be spawned
         */
        @Nullable
        public EntityType<? extends MobEntity> getEntity() {
            return entity;
        }
    }

    /**
     * Fired when blocks around a village should be replaced
     * Only fired if Vampirism didn't already replace a block.
     * Can be used to replace a block on your own
     */
    public static class ReplaceBlock extends VampirismVillageEvent {

        private final @Nonnull
        World world;
        private final @Nonnull
        BlockState state;
        private final @Nonnull
        BlockPos pos;

        public ReplaceBlock(ITotem totem, @Nonnull World world, @Nonnull BlockState b, @Nonnull BlockPos pos) {
            super(totem);
            this.world = world;
            this.state = b;
            this.pos = pos;
        }

        /**
         * @returns the world of the block
         */
        @Nonnull
        public World getWorld() {
            return world;
        }

        /**
         * @returns blockstate of the block to be replaced
         */
        @Nonnull
        public BlockState getState() {
            return state;
        }

        /**
         * @returns the position of the block
         */
        @Nonnull
        public BlockPos getBlockPos() {
            return pos;
        }

    }

    /**
     * fired when the caption process is started
     * set the result to DENY to skip the vanilla code
     */
    @HasResult
    public static class InitiateCapture extends VampirismVillageEvent {

        private final @Nonnull
        World world;
        private final @Nonnull
        IFaction<?> capturingFaction;
        private String message;

        public InitiateCapture(ITotem totem, @Nonnull World world, @Nonnull IFaction<?> capturingFaction) {
            super(totem);
            this.world = world;
            this.capturingFaction = capturingFaction;
        }

        @Nonnull
        public World getWorld() {
            return world;
        }

        /**
         * @returns capturing faction
         */
        @Override
        @Nonnull
        public IFaction<?> getCapturingFaction() {
            return capturingFaction;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * fired when the village area is updated (used for vampire fog rendering & sundamage)
     */
    public static class UpdateBoundingBox extends VampirismVillageEvent {

        private final @Nonnull
        MutableBoundingBox bb;

        public UpdateBoundingBox(ITotem totem, @Nonnull MutableBoundingBox bb) {
            super(totem);
            this.bb = bb;
        }

        /**
         * @returns bounding box of the village
         */
        @Nonnull
        public MutableBoundingBox getBoundingBox() {
            return bb;
        }

    }
}
