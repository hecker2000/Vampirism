package de.teamlapen.vampirism.entity.player.skills;

import de.teamlapen.lib.lib.storage.ISyncableSaveData;
import de.teamlapen.lib.lib.storage.UpdateParams;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.factions.ISkillNode;
import de.teamlapen.vampirism.api.entity.factions.ISkillTree;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.ISkillPlayer;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillPointProvider;
import de.teamlapen.vampirism.api.entity.player.skills.SkillPointProviders;
import de.teamlapen.vampirism.core.ModAdvancements;
import de.teamlapen.vampirism.core.ModEffects;
import de.teamlapen.vampirism.core.ModRegistries;
import de.teamlapen.vampirism.core.ModStats;
import de.teamlapen.vampirism.data.ISkillTreeData;
import de.teamlapen.vampirism.util.RegUtil;
import de.teamlapen.vampirism.util.VampirismEventFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class SkillHandler<T extends IFactionPlayer<T> & ISkillPlayer<T>> implements ISkillHandler<T>, ISyncableSaveData {
    private static final String NBT_KEY = "skill_handler";
    private final static Logger LOGGER = LogManager.getLogger();
    private final Map<Holder<ISkillTree>, List<Holder<ISkill<T>>>> enabledSkills = new HashMap<>();
    private final T player;
    private final Holder<? extends IPlayableFaction<T>> faction;
    private final ISkillPointProvider skillPoints = new SkillPoints();
    public LinkedHashSet<Holder<ISkillTree>> unlockedTrees = new LinkedHashSet<>();
    private boolean dirty = false;
    private final ISkillTreeData treeData;

    public SkillHandler(T player, Holder<? extends IPlayableFaction<T>> faction) {
        this.player = player;
        this.faction = faction;
        this.treeData = ISkillTreeData.getData(player.asEntity().level());
    }

    public @NotNull Optional<ISkillNode> anyLastNode() {
        return unlockedTrees.stream().flatMap(s -> this.treeData.getAnyLastNode(s, this::isNodeEnabled).stream()).findAny();
    }

    public ISkillTreeData getTreeData() {
        return this.treeData;
    }

    @Override
    public @NotNull Result canSkillBeEnabled(@NotNull Holder<ISkill<?>> skill, Holder<ISkillTree> skillTree) {
        var preResult = VampirismEventFactory.fireSkillUnlockCheckEvent(this.player, skill);
        if (preResult != null) {
            return preResult;
        }

        if (player.asEntity().getEffect(ModEffects.OBLIVION) != null) {
            return Result.LOCKED_BY_PLAYER_STATE;
        }
        if (isSkillEnabled(skill)) {
            return Result.ALREADY_ENABLED;
        }
        Optional<SkillTreeConfiguration.SkillTreeNodeConfiguration> node = unlockedTrees.stream().flatMap(x -> treeData.getNodeForSkill(unlockedTrees, skill).stream()).findFirst();
        if (node.isPresent()) {
            if (isSkillNodeLocked(node.get().node().value())) {
                return Result.LOCKED_BY_OTHER_NODE;
            }
            if (this.treeData.isRoot(this.unlockedTrees, node.get()) || this.treeData.getParent(node.get()).stream().anyMatch(x -> isNodeEnabled(x.value()))) {
                if (getLeftSkillPoints(skillTree) >= skill.value().getSkillPointCost()) {
                    return isNodeEnabled(node.get().node().value()) ? Result.OTHER_NODE_SKILL : Result.OK;//If another skill in that node is already enabled this one cannot be enabled
                } else {
                    return Result.NO_POINTS;
                }

            } else {
                return Result.PARENT_NOT_ENABLED;
            }
        } else {
            LOGGER.warn("Node for skill {} could not be found", skill);
            return Result.NOT_FOUND;
        }
    }

    public void disableAllSkills() {
        for (Map.Entry<Holder<ISkillTree>, List<Holder<ISkill<T>>>> entry : enabledSkills.entrySet()) {
            for (Holder<ISkill<T>> skill : entry.getValue()) {
                VampirismEventFactory.fireSkillDisabledEvent(player, skill);
                skill.value().onDisable(player);
            }
        }
        enabledSkills.clear();
        dirty = true;
    }

    @Override
    public void disableSkill(@NotNull Holder<ISkill<T>> skill, Holder<ISkillTree> tree) {
        if (enabledSkills.containsKey(tree) && enabledSkills.get(tree).remove(skill)) {
            VampirismEventFactory.fireSkillDisabledEvent(player, skill);
            skill.value().onDisable(player);
            dirty = true;
        }
    }

    @Override
    public void enableSkill(@NotNull Holder<ISkill<T>> skill, Holder<ISkillTree>  tree, boolean fromLoading) {
        if (!enabledSkills.contains(skill)) {
            VampirismEventFactory.fireSkillEnableEvent(player, skill, fromLoading);
            skill.value().onEnable(player);
            enabledSkills.add(skill);
            if (!fromLoading) {
                this.player.asEntity().awardStat(ModStats.SKILL_UNLOCKED.get().get(skill.value()));
            }
            dirty = true;
            //noinspection ConstantValue
            if (this.player.asEntity() instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
                ModAdvancements.TRIGGER_SKILL_UNLOCKED.get().trigger(serverPlayer, skill.value());
            }
        }

    }

    @Override
    public void updateUnlockedSkillTrees(Collection<Holder<ISkillTree>> skillTrees) {
        List<Holder<ISkillTree>> removedTrees = this.unlockedTrees.stream().filter(x -> !skillTrees.contains(x)).toList();
        removedTrees.forEach(this::lockSkillTree);
        skillTrees.stream().filter(x -> !this.unlockedTrees.contains(x)).forEach(this::unlockSkillTree);
        this.dirty = true;
    }

    private void unlockSkillTree(Holder<ISkillTree> tree) {
        this.unlockedTrees.add(tree);
        SkillTreeConfiguration.SkillTreeNodeConfiguration root = this.treeData.root(tree);
        root.elements().forEach(x -> enableSkill((Holder<ISkill<T>>) (Object) x, tree,true));
        this.dirty = true;
    }

    private void lockSkillTree(Holder<ISkillTree> tree) {
        var enabledSkills = new ArrayList<>(this.enabledSkills);
        for (Holder<ISkill<T>> enabledSkill : enabledSkills) {
            if (enabledSkill.value().allowedSkillTrees().map(tree::is, tree::is)) {
                this.disableSkill(enabledSkill);
            }
        }
        this.unlockedTrees.remove(tree);
        this.dirty = true;
    }

    public @NotNull Collection<Holder<ISkillTree>> unlockedSkillTrees() {
        return Collections.unmodifiableCollection(this.unlockedTrees);
    }

    @Override
    public int getLeftSkillPoints(Holder<ISkillTree> tree) {
        if (this.skillPoints.ignoreSkillPointLimit(this.player, tree)) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, this.skillPoints.getSkillPoints(this.player, tree) - this.enabledSkills.stream().map(Holder::value).mapToInt(ISkill::getSkillPointCost).sum());
    }

    public void reset() {
        disableAllSkills();
        this.unlockedTrees.clear();
        this.dirty = true;
    }

    @Override
    public List<Holder<ISkill<?>>> getParentSkills(@NotNull Holder<ISkill<?>> skill) {
        Optional<SkillTreeConfiguration.SkillTreeNodeConfiguration> nodeForSkill = this.treeData.getNodeForSkill(this.unlockedTrees, skill);
        return nodeForSkill.flatMap(this.treeData::getParent).stream().flatMap(x -> x.value().skills().stream()).collect(Collectors.toList());
    }

    public T getPlayer() {
        return player;
    }

    public boolean noSkillEnabled() {
        List<ISkill<?>> list = this.unlockedTrees.stream().map(this.treeData::root).flatMap(x -> x.elements().stream()).map(Holder::value).collect(Collectors.toList());
        //noinspection SuspiciousMethodCalls
        return this.enabledSkills.isEmpty() || new HashSet<>(list).containsAll(this.enabledSkills);
    }

    @SuppressWarnings("unchecked")
    public boolean isNodeEnabled(@NotNull ISkillNode node) {
        for (Holder<ISkill<T>> s : enabledSkills) {
            if (node.containsSkill((Holder<ISkill<?>>) (Object) s)) return true;
        }
        return false;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean areSkillsEnabled(Collection<Holder<ISkill<?>>> skill) {
        return enabledSkills.containsAll(skill);
    }

    @Override
    public boolean isSkillEnabled(Holder<ISkill<?>> skill) {
        return enabledSkills.contains(skill);
    }

    public boolean isSkillNodeLocked(@NotNull ISkillNode nodeIn) {
        Registry<ISkillNode> nodes = player.asEntity().level().registryAccess().registryOrThrow(VampirismRegistries.Keys.SKILL_NODE);
        return nodeIn.lockingNodes().stream().flatMap(s -> nodes.getOptional(s).stream()).flatMap(s -> s.skills().stream()).anyMatch(this::isSkillEnabled);
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        if (nbt.contains("skills")) {
            for (String id : nbt.getCompound("skills").getAllKeys()) {
                ModRegistries.SKILLS.getHolder(ResourceLocation.parse(id)).ifPresentOrElse(holder -> {
                    //noinspection unchecked
                    enableSkill((Holder<ISkill<T>>) (Object) holder, true);
                }, () -> LOGGER.warn("Skill {} does not exist anymore", id));

            }
        }

        if (nbt.contains("unlocked_trees")) {
            ListTag unlockedTrees = nbt.getList("unlocked_trees", StringTag.TAG_STRING);
            this.unlockedTrees.clear();
            unlockedTrees.stream().map(StringTag.class::cast).forEach(tag -> {
                this.unlockedTrees.add(RegUtil.getSkillTree(getPlayer().asEntity().level(), tag.getAsString()));
            });
        }
    }

    @Override
    public void deserializeUpdateNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        if (nbt.contains("skills", Tag.TAG_COMPOUND)) {

            //noinspection unchecked
            List<Holder<ISkill<T>>> old = (List<Holder<ISkill<T>>>) enabledSkills.clone();
            for (String id : nbt.getCompound("skills").getAllKeys()) {
                ModRegistries.SKILLS.getHolder(ResourceLocation.parse(id)).ifPresent(holder -> {
                    if (old.contains(holder)) {
                        old.remove(holder);
                    } else {
                        enableSkill((Holder<ISkill<T>>) (Object) holder, true);
                    }
                });
            }
            for (Holder<ISkill<T>> skill : old) {
                disableSkill(skill);
            }
        }

        if (nbt.contains("unlocked_trees", Tag.TAG_LIST)) {
            ListTag unlockedTrees = nbt.getList("unlocked_trees", StringTag.TAG_STRING);
            this.unlockedTrees.clear();
            unlockedTrees.stream().map(StringTag.class::cast).forEach(tag -> {
                this.unlockedTrees.add(RegUtil.getSkillTree(getPlayer().asEntity().level(), tag.getAsString()));
            });
        }
    }

    public void resetSkills() {
        disableAllSkills();
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag nbt = new CompoundTag();
        CompoundTag skills = new CompoundTag();
        for (Holder<ISkill<T>> skill : enabledSkills) {
            skill.unwrapKey().map(ResourceKey::location).map(ResourceLocation::toString).ifPresent(id -> skills.putBoolean(id, true));
        }
        nbt.put("skills", skills);
        ListTag unlockedTrees = new ListTag();
        for (Holder<ISkillTree> tree : this.unlockedTrees) {
            unlockedTrees.add(StringTag.valueOf(RegUtil.id(getPlayer().asEntity().level(), tree.value()).toString()));
        }
        nbt.put("unlocked_trees", unlockedTrees);
        return nbt;
    }

    @Override
    public @NotNull CompoundTag serializeUpdateNBTInternal(HolderLookup.@NotNull Provider provider, UpdateParams params) {
        CompoundTag nbt = new CompoundTag();
        CompoundTag skills = new CompoundTag();
        for (Holder<ISkill<T>> skill : enabledSkills) {
            skill.unwrapKey().map(ResourceKey::location).map(ResourceLocation::toString).ifPresent(id -> skills.putBoolean(id, true));
        }
        nbt.put("skills", skills);
        ListTag unlockedTrees = new ListTag();
        for (Holder<ISkillTree> tree : this.unlockedTrees) {
            unlockedTrees.add(StringTag.valueOf(RegUtil.id(getPlayer().asEntity().level(), tree.value()).toString()));
        }
        nbt.put("unlocked_trees", unlockedTrees);
        return nbt;
    }

    @Override
    public boolean needsUpdate() {
        return this.dirty;
    }

    @Override
    public void updateSend() {
        this.dirty = false;
    }

    @Override
    public String nbtKey() {
        return NBT_KEY;
    }

    public static class SkillPoints implements ISkillPointProvider {
        private final Map<ResourceLocation, ISkillPointProvider> provider;

        public SkillPoints() {
            this.provider = SkillPointProviders.MODIFIERS_VIEW;
        }

        @Override
        public int getSkillPoints(IFactionPlayer<?> factionPlayer, ISkillTree tree) {
            return this.provider.values().stream().mapToInt(x -> Math.max(0, x.getSkillPoints(factionPlayer, tree))).sum();
        }

        @Override
        public boolean ignoreSkillPointLimit(IFactionPlayer<?> factionPlayer, ISkillTree tree) {
            return this.provider.values().stream().anyMatch(l -> l.ignoreSkillPointLimit(factionPlayer));
        }
    }
}
