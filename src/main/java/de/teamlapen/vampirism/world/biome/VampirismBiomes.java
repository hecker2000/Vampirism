package de.teamlapen.vampirism.world.biome;

import de.teamlapen.vampirism.core.ModEntities;
import de.teamlapen.vampirism.world.gen.VampirismFeatures;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;

public class VampirismBiomes {

    public static Biome createVampireForest() {
        MobSpawnSettings.Builder mobSpawnBuilder = new MobSpawnSettings.Builder();
        mobSpawnBuilder.creatureGenerationProbability(0.25f);
        mobSpawnBuilder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.VAMPIRE.get(), 35, 1, 3));
        mobSpawnBuilder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.VAMPIRE_BARON.get(), 10, 1, 1));
        mobSpawnBuilder.addSpawn(MobCategory.AMBIENT, new MobSpawnSettings.SpawnerData(ModEntities.BLINDING_BAT.get(), 60, 2, 4));
        mobSpawnBuilder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ModEntities.DUMMY_CREATURE.get(), 80, 3, 6));

        BiomeSpecialEffects.Builder biomeSpecialEffectsBuilder =  new BiomeSpecialEffects.Builder().waterColor(0x7d0000).waterFogColor(0x7d0000).fogColor(0x7d3535).skyColor(0x7d3535).foliageColorOverride(0x1E1F1F).grassColorOverride(0x2c2132).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);

        return prepareVampireForestBuilder(mobSpawnBuilder, biomeSpecialEffectsBuilder).build();
    }

    public static Biome.BiomeBuilder prepareVampireForestBuilder(MobSpawnSettings.Builder spawnBuilder, BiomeSpecialEffects.Builder ambienceBuilder) {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        addDefaultCarversWithoutLakes(builder);
        addModdedWaterLake(builder);

        addVampireFlower(builder);
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VampirismFeatures.FOREST_GRASS_PLACED.getHolder().orElseThrow());

        BiomeDefaultFeatures.addDefaultUndergroundVariety(builder);
        BiomeDefaultFeatures.addDefaultOres(builder);
        BiomeDefaultFeatures.addDefaultSoftDisks(builder);

        addVampireTrees(builder);

        addWaterSprings(builder);
        return new Biome.BiomeBuilder().precipitation(Biome.Precipitation.NONE).temperature(0.3F).downfall(0F).specialEffects(ambienceBuilder.build()).mobSpawnSettings(spawnBuilder.build()).generationSettings(builder.build());
    }


    public static void addVampireFlower(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VampirismFeatures.VAMPIRE_FLOWER_PLACED.getHolder().orElseThrow());
    }

    public static void addWaterSprings(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, MiscOverworldPlacements.SPRING_WATER);
    }

    public static void addModdedWaterLake(BiomeGenerationSettings.Builder builder) {
         builder.addFeature(GenerationStep.Decoration.LAKES, VampirismFeatures.WATER_LAKE_PLACED.getHolder().orElseThrow());
    }

    public static void addVampireTrees(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VampirismFeatures.VAMPIRE_TREES_PLACED.getHolder().orElseThrow());
    }

    public static void addDefaultCarversWithoutLakes(BiomeGenerationSettings.Builder builder) {
        builder.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE);
        builder.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE_EXTRA_UNDERGROUND);
        builder.addCarver(GenerationStep.Carving.AIR, Carvers.CANYON);
    }
}
