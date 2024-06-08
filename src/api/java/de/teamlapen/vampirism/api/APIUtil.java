package de.teamlapen.vampirism.api;

import com.google.common.base.Suppliers;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.ILastingAction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import java.util.Objects;
import java.util.function.Supplier;

class APIUtil {

    @SuppressWarnings("unchecked")
    static <T> Supplier<DataComponentType<T>> supplyDataComponent(ResourceLocation key) {
        return Suppliers.memoize(() -> Objects.requireNonNull((DataComponentType<T>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(key)));
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    static <T, Z> Supplier<DataMapType<T,Z>> supplyDataMap(ResourceKey<Registry<T>> registry, ResourceLocation key) {
        return Suppliers.memoize(() -> Objects.requireNonNull((DataMapType<T,Z>) RegistryManager.getDataMap(registry, key)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> Supplier<T> supplyRegistry(ResourceKey<T> key) {
        return Suppliers.memoize(() -> Objects.requireNonNull((T) BuiltInRegistries.REGISTRY.get((ResourceKey) key)));
    }

    static <T> ResourceKey<Registry<T>> registryKey(String name) {
        return ResourceKey.createRegistryKey(new ResourceLocation(VReference.MODID, name));
    }
}
