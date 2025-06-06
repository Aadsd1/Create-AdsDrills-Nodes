package com.yourname.mycreateaddon.registry;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.kinetics.drill.DrillCoreBlock;
import com.yourname.mycreateaddon.content.kinetics.module.FrameModuleBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyAddonBlocks {

    private static final CreateRegistrate REGISTRATE = MyCreateAddon.registrate();

    public static final BlockEntry<DrillCoreBlock> DRILL_CORE =
            REGISTRATE.block("drill_core", DrillCoreBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.noOcclusion())
                    .blockstate((ctx, prov) -> {
                        ModelFile model = prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block"));
                        prov.getVariantBuilder(ctx.get())
                                .forAllStates(state -> {
                                    Direction.Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
                                    int x = axis == Direction.Axis.X ? 90 : 0;
                                    int y = axis == Direction.Axis.Z ? 90 : (axis == Direction.Axis.X ? 90 : 0);

                                    return ConfiguredModel.builder()
                                            .modelFile(model)
                                            .rotationX(x)
                                            .rotationY(y)
                                            .build();
                                });
                    })
                    .loot((tables, block) -> tables.dropSelf(block))
                    .item()
                    .build()
                    .register();

    public static final BlockEntry<FrameModuleBlock> FRAME_MODULE =
            REGISTRATE.block("frame_module", FrameModuleBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.noOcclusion())
                    .blockstate((ctx, prov) -> prov.simpleBlock(
                            ctx.get(),
                            prov.models().getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block"))
                    ))
                    .loot((tables, block) -> tables.dropSelf(block))
                    .item()
                    .build()
                    .register();

    public static Iterable<Block> getAllBlocks() {
        return Stream.of(DRILL_CORE, FRAME_MODULE)
                .map(RegistryEntry::get).collect(Collectors.toList());
    }

    public static void register() {}
}