package de.teamlapen.vampirism.entity.player;

import de.teamlapen.lib.HelperLib;
import de.teamlapen.lib.lib.entity.IPlayerEventListener;
import de.teamlapen.lib.lib.network.ISyncable;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.runnable.IPlayerRunnable;
import de.teamlapen.vampirism.api.entity.player.runnable.ISaveablePlayerRunnable;
import de.teamlapen.vampirism.config.VampirismConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic class for all of Vampirism's players.
 * Implements basic methods for level or minion handling
 */
public abstract class FactionBasePlayer<T extends IFactionPlayer<T>> implements IFactionPlayer<T>, ISyncable.ISyncableEntityCapabilityInst, IPlayerEventListener {

    private static final Logger LOGGER = LogManager.getLogger(FactionBasePlayer.class);
    protected final Player player;
    /**
     * {@code @NotNull} on server, otherwise {@code null}
     */
    private final @Nullable TaskManager taskManager;
    private final List<IPlayerRunnable<T>> dispatchedActions = new ArrayList<>();

    public FactionBasePlayer(Player player) {
        this.player = player;
        if (player instanceof ServerPlayer) {
            this.taskManager = new TaskManager((ServerPlayer) player, this, this.getFaction());
        } else {
            this.taskManager = null;
        }
    }


    @Override
    public int getLevel() {
        return VampirismAPI.getFactionPlayerHandler(player).map(handler -> handler.getCurrentLevel(getFaction())).orElse(0);
    }

    /**
     * Only use this if you are dealing with minions.
     * Otherwise use {@link FactionBasePlayer#getRepresentingPlayer()}
     */
    @Override
    public LivingEntity getRepresentingEntity() {
        return player;
    }

    @Override
    public Player getRepresentingPlayer() {
        return player;
    }

    /**
     * null on client and @NotNull on server
     */
    @NotNull
    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public int getTheEntityID() {
        return player.getId();
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean isRemote() {
        if (player.getCommandSenderWorld() == null) {
            LOGGER.error("Trying to check if remote, but world is not set yet", new Throwable("World not loaded").fillInStackTrace());
            return false;
        }
        return player.getCommandSenderWorld().isClientSide;
    }

    public void loadData(@NotNull CompoundTag nbt) {
        if (this.taskManager != null) {
            this.taskManager.readNBT(nbt);
        } else {
            LOGGER.debug("The player is loaded on the client side and therefore taskmaster related data is missing");
        }
        nbt.getList("dispatchedActions", 10).stream().map(CompoundTag.class::cast).forEach(x -> {
            var constructor = ISaveablePlayerRunnable.CONSTRUCTORS.get(new ResourceLocation(x.getString("id")));
            if (constructor != null) {
                //noinspection unchecked
                this.dispatchedActions.add((IPlayerRunnable<T>) constructor.apply(x));
            }
        });
    }

    @Override
    public final void loadUpdateFromNBT(CompoundTag nbt) {
        loadUpdate(nbt);
    }

    @Override
    public void onDeath(DamageSource src) {
        this.getSkillHandler().damageRefinements();
    }

    @Override
    public void onPlayerClone(@NotNull Player original, boolean wasDeath) {
        original.reviveCaps();
        copyFromPlayer(original);
        original.invalidateCaps();
    }

    @Override
    public void onUpdate() {
        if (!isRemote()) {
            this.taskManager.tick();
            this.dispatchedActions.removeIf(action -> {
                if (action.run((T) this)) {
                    action.shutdown((T) this);
                    return true;
                }
                return false;
            });
        }
    }

    public void dispatchAction(IPlayerRunnable<T> task) {
        //noinspection unchecked
        task.setUp((T) this);
        this.dispatchedActions.add(task);
    }

    public void saveData(@NotNull CompoundTag nbt) {
        if (this.taskManager != null) {
            this.taskManager.writeNBT(nbt);
        } else {
            LOGGER.debug("The player is saved on the client side and therefore taskmaster related data is missing");
        }

        var dispatched = new ListTag();
        this.dispatchedActions.stream().filter(ISaveablePlayerRunnable.class::isInstance).map(s -> {
            CompoundTag tag = ((ISaveablePlayerRunnable<T>) s).writeNBT();
            tag.putString("id", ((ISaveablePlayerRunnable<T>) s).getID().toString());
            return tag;
        }).forEach(dispatched::add);
        nbt.put("dispatchedActions", dispatched);
    }

    /**
     * Sync all data
     *
     * @param all Whether all tracking players should receive this packet or only the representing player
     */
    public void sync(boolean all) {
        HelperLib.sync(this, player, all);
    }

    @Override
    public final void writeFullUpdateToNBT(CompoundTag nbt) {
        writeFullUpdate(nbt);
    }

    /**
     * Copy all relevant values from the given player and return an instance of the old players VampirismPlayer, so {@link FactionBasePlayer} can copy its values as well
     */
    protected abstract FactionBasePlayer<T> copyFromPlayer(Player old);

    /**
     * Can be overridden to load data from updates in subclasses
     */
    protected void loadUpdate(CompoundTag nbt) {
    }

    /**
     * Sync the capability using the given data
     *
     * @param all Whether all tracking players should receive this packet or only the representing player
     */
    protected void sync(@NotNull CompoundTag data, boolean all) {
        HelperLib.sync(this, data, player, all);
    }

    /**
     * Can be overridden to put data into updates in subclasses
     */
    protected void writeFullUpdate(CompoundTag nbt) {
    }

    @Override
    public void onLevelChanged(int newLevel, int oldLevel) {
        if (!isRemote()) {
            if (newLevel > 0) {
                this.getSkillHandler().addSkillPoints((int) ((newLevel - oldLevel) * VampirismConfig.BALANCE.skillPointsPerLevel.get()));
            } else {
                this.getSkillHandler().reset();
                this.getActionHandler().resetTimers();
            }

        } else {
            if (newLevel == 0) {
                this.getActionHandler().resetTimers();
                this.getSkillHandler().resetRefinements();
            }
        }
    }
}
