package de.teamlapen.vampirism.util;

import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import de.teamlapen.vampirism.api.EnumStrength;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.hunter.IHunterMob;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import de.teamlapen.vampirism.api.items.IFactionLevelItem;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.core.ModEffects;
import de.teamlapen.vampirism.core.ModTags;
import de.teamlapen.vampirism.entity.CrossbowArrowEntity;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.player.VampirismPlayerAttributes;
import de.teamlapen.vampirism.items.CrossbowArrowItem;
import de.teamlapen.vampirism.items.StakeItem;
import de.teamlapen.vampirism.world.LevelFog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;


public class Helper {


    private final static Logger LOGGER = LogManager.getLogger();

    /**
     * Checks if the entity can get sundamage at its current position.
     * It is recommended to cache the value for a few ticks.
     */
    public static boolean gettingSundamge(LivingEntity entity, LevelAccessor world, @Nullable ProfilerFiller profiler) {
        if (entity instanceof Player && entity.isSpectator()) return false;
        if (world instanceof Level level && !level.isRaining() && VampirismAPI.sundamageRegistry().hasSunDamage(world, entity.blockPosition()) && isDay(world)) {
            BlockPos pos = new BlockPos(entity.getBlockX(), entity.getBlockY() + (int) entity.getEyeHeight(), entity.getBlockZ());
            return canBlockSeeSun(world, pos) && !LevelFog.get(level).isInsideArtificialVampireFogArea(pos);
        }
        return false;
    }

    public static boolean isDay(LevelAccessor level) {
        float angle = level.getTimeOfDay(1.0F);
        return angle > 0.78 || angle < 0.24;
    }

    public static boolean canBlockSeeSun(@NotNull LevelAccessor world, @NotNull BlockPos pos) {
        if (pos.getY() >= world.getSeaLevel()) {
            return world.canSeeSky(pos);
        } else {
            BlockPos blockpos = new BlockPos(pos.getX(), world.getSeaLevel(), pos.getZ());
            if (!world.canSeeSky(blockpos)) {
                return false;
            } else {
                int liquidBlocks = 0;
                for (blockpos = blockpos.below(); blockpos.getY() > pos.getY(); blockpos = blockpos.below()) {
                    BlockState state = world.getBlockState(blockpos);
                    if (state.liquid()) { // if fluid than it propagates the light until `vpSundamageWaterBlocks`
                        liquidBlocks++;
                        if (liquidBlocks >= VampirismConfig.BALANCE.vpSundamageWaterblocks.get()) {
                            return false;
                        }
                    } else if (state.canOcclude() && (state.isFaceSturdy(world, pos, Direction.DOWN) || state.isFaceSturdy(world, pos, Direction.UP))) { //solid block blocks the light (fence is solid too?)
                        return false;
                    } else if (state.getLightBlock(world, blockpos) > 0) { //if not solid, but propagates no light
                        return false;
                    }
                }
                return true;
            }
        }
    }


    @NotNull
    public static EnumStrength getGarlicStrength(@NotNull Entity e, LevelAccessor world) {
        return getGarlicStrengthAt(world, e.blockPosition());
    }

    @NotNull
    public static EnumStrength getGarlicStrengthAt(LevelAccessor world, @NotNull BlockPos pos) {
        return world instanceof Level ? VampirismAPI.garlicHandler((Level) world).getStrengthAtChunk(new ChunkPos(pos)): EnumStrength.NONE;
    }

    @NotNull
    public static ResourceKey<Level> getWorldKey(LevelAccessor world) {
        return world instanceof Level ? ((Level) world).dimension() : world instanceof ServerLevelAccessor ? ((ServerLevelAccessor) world).getLevel().dimension() : Level.OVERWORLD;
    }

    public static boolean canBecomeVampire(@NotNull Player player) {
        return FactionPlayerHandler.get(player).canJoin(VReference.VAMPIRE_FACTION);
    }

    public static boolean canTurnPlayer(IVampire biter, @Nullable Player target) {
        if (target != null && (target.isCreative() || target.isSpectator())) return false;
        if (biter instanceof IVampirePlayer player) {
            if (!VampirismConfig.SERVER.playerCanTurnPlayer.get()) return false;
            return !(player instanceof ServerPlayer) || Permissions.INFECT_PLAYER.isAllowed((ServerPlayer) player);
        } else {
            return !VampirismConfig.SERVER.disableMobBiteInfection.get();
        }
    }

    /**
     * Checks if
     *
     * @return If the given entity is a vampire (Either a player in the vampire faction or a vampire entity
     */
    public static boolean isVampire(Entity entity) {
        return VReference.VAMPIRE_FACTION.equals(VampirismAPI.factionRegistry().getFaction(entity));
    }

    public static boolean isHunter(Entity entity) {
        return VReference.HUNTER_FACTION.equals(VampirismAPI.factionRegistry().getFaction(entity));
    }

    public static boolean isHunter(Player entity) {
        return VReference.HUNTER_FACTION.equals(VampirismPlayerAttributes.get(entity).faction);
    }

    public static boolean isVampire(Player entity) {
        return VReference.VAMPIRE_FACTION.equals(VampirismPlayerAttributes.get(entity).faction);
    }

    /**
     * @return Checks if all given skills are enabled
     */
    public static <T extends IFactionPlayer<T>> boolean areSkillsEnabled(@NotNull ISkillHandler<T> skillHandler, @NotNull List<ISkill<T>> skills) {
        for (ISkill<T> skill : skills) {
            if (!skillHandler.isSkillEnabled(skill)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEntityInVampireBiome(@Nullable Entity e) {
        if (e == null) return false;
        Level w = e.getCommandSenderWorld();
        return w.getBiome(e.blockPosition()).is(ModTags.Biomes.IS_VAMPIRE_BIOME);
    }

    public static boolean isPosInVampireBiome(@NotNull BlockPos pos, @NotNull LevelAccessor level) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(ModTags.Biomes.IS_VAMPIRE_BIOME);
    }

    /**
     * @return Whether the entity is in a vampire fog area (does not check for vampire biome)
     */
    public static boolean isEntityInArtificalVampireFogArea(@Nullable Entity e) {
        if (e == null) return false;
        Level w = e.getCommandSenderWorld();
        return LevelFog.getOpt(w).map(vh -> vh.isInsideArtificialVampireFogArea(e.blockPosition())).orElse(false);
    }

    public static ResourceLocation getBiomeId(@NotNull Entity e) {
        return getBiomeId(e.getCommandSenderWorld(), e.blockPosition());
    }

    public static Holder<Biome> getBiome(@NotNull Entity e) {
        return e.getCommandSenderWorld().getBiome(e.blockPosition());
    }

    public static ResourceLocation getBiomeId(@NotNull CommonLevelAccessor world, @NotNull BlockPos pos) {
        return getBiomeId(world, world.getBiome(pos));
    }

    public static ResourceLocation getBiomeId(@NotNull CommonLevelAccessor world, @NotNull Holder<Biome> biome) {
        return biome.unwrap().map(ResourceKey::location, b -> world.registryAccess().registryOrThrow(Registries.BIOME).getKey(b));
    }

    /**
     * Checks if the given {@link IFactionLevelItem} can be used by the given player
     */
    public static boolean canUseFactionItem(@NotNull ItemStack stack, @NotNull IFactionLevelItem<?> item, @NotNull IFactionPlayerHandler playerHandler) {
        IFaction<?> usingFaction = item.getExclusiveFaction(stack);
        ISkill<?> requiredSkill = item.getRequiredSkill(stack);
        int reqLevel = item.getMinLevel(stack);
        if (usingFaction != null && !playerHandler.isInFaction(usingFaction)) return false;
        if (playerHandler.getCurrentLevel() < reqLevel) return false;
        if (requiredSkill == null) return true;
        return playerHandler.getCurrentFactionPlayer().map(IFactionPlayer::getSkillHandler).map(s -> s.isSkillEnabled(requiredSkill)).orElse(false);
    }

    /**
     * Returns false on client side
     * Determines the gender of the player by checking the skin and assuming 'slim'->female.
     *
     * @param p Player
     * @return True if female
     */
    public static boolean attemptToGuessGenderSafe(Player p) {
        if (p instanceof ServerPlayer) { //Could extend to also support client side, but have to use proxy then
            MinecraftProfileTextures textureMap = ((ServerPlayer) p).server.getSessionService().getTextures(p.getGameProfile());
            if (textureMap.skin() != null) {
                return "slim".equals(textureMap.skin().getMetadata("model"));
            }
        }
        return false;
    }

    public static <T extends Entity> @NotNull Optional<T> createEntity(@NotNull EntityType<T> type, @NotNull Level world) {
        T e = type.create(world);
        if (e == null) {
            LOGGER.warn("Failed to create entity of type {}", RegUtil.id(type));
            return Optional.empty();
        }
        return Optional.of(e);
    }

    /**
     * blockpos to nbt
     */
    public static @NotNull ListTag newDoubleNBTList(double @NotNull ... numbers) {
        ListTag listnbt = new ListTag();

        for (double d0 : numbers) {
            listnbt.add(DoubleTag.valueOf(d0));
        }

        return listnbt;
    }

    /**
     * Check if
     *
     * @return Whether the given damage source can kill a vampire player or go to DBNO state instead
     */
    public static boolean canKillVampires(@NotNull DamageSource source) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.is(ModTags.DamageTypes.VAMPIRE_IMMORTAL) || VampirismConfig.BALANCE.vpImmortalFromDamageSources.get().contains(source.getMsgId())) {
                if (source.getDirectEntity() instanceof LivingEntity) {
                    //Maybe use all IVampireFinisher??
                    return source.getDirectEntity() instanceof IHunterMob || ((LivingEntity) source.getDirectEntity()).getMainHandItem().getItem() instanceof StakeItem;
                } else if (source.getDirectEntity() instanceof CrossbowArrowEntity) {
                    return ((CrossbowArrowEntity) source.getDirectEntity()).getArrowType() == CrossbowArrowItem.EnumArrowType.VAMPIRE_KILLER;
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Call this in Item inventoryTick to make Vampires drop this item and get a short poison effect when held
     *
     * @param stack  item
     * @param entity Entity holding the item
     * @param held   Whether it is currently selected
     * @return If entity is vampire
     */
    public static boolean handleHeldNonVampireItem(ItemStack stack, Entity entity, boolean held) {
        if (entity instanceof LivingEntity living && entity.tickCount % 16 == 8 && (held || living.getOffhandItem() == stack)) {
            if (Helper.isVampire(entity)) {
                ((LivingEntity) entity).addEffect(new MobEffectInstance(ModEffects.POISON, 20, 1));
                if (entity instanceof Player player) {
                    player.getInventory().removeItem(stack);
                    player.drop(stack, true);
                }
                return true;
            }
        }
        return false;
    }
}
