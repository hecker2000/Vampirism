package de.teamlapen.vampirism.world.gen.structure.mother;

import com.mojang.serialization.MapCodec;
import de.teamlapen.vampirism.core.ModStructures;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

public class MotherStructure extends SinglePieceStructure {
    public static final MapCodec<MotherStructure> CODEC = simpleCodec(MotherStructure::new);


    public MotherStructure(StructureSettings settings) {
        super(MotherPiece::new, 40, 20, settings);
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.MOTHER_TYPE.get();
    }
}
