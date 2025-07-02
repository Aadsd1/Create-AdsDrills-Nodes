package com.yourname.mycreateaddon.content.kinetics.module; // 또는 원하는 경로

import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import com.simibubi.create.foundation.block.IBE; // IBE 임포트
import com.yourname.mycreateaddon.registry.MyAddonBlockEntity; // BE 레지스트리 임포트
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;


public class GenericModuleBlock extends Block implements IBE<GenericModuleBlockEntity> {

    private final ModuleType moduleType;

    public GenericModuleBlock(Properties properties, ModuleType moduleType) {
        super(properties);
        this.moduleType = moduleType;
    }

    public ModuleType getModuleType() {
        return moduleType;
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide()) {
            return;
        }
        findAndNotifyCore(level, pos);
    }

    public static void findAndNotifyCore(Level level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        searchForCore(level, startPos, visited, 0);
    }

    private static void searchForCore(Level level, BlockPos currentPos, Set<BlockPos> visited, int depth) {
        if (depth > 64 || !visited.add(currentPos)) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = currentPos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            if (level.getBlockEntity(neighborPos) instanceof DrillCoreBlockEntity core) {
                core.scheduleStructureCheck();
            }
            else if (neighborBlock instanceof GenericModuleBlock) {
                searchForCore(level, neighborPos, visited, depth + 1);
            }
        }
    }

    @Override
    public Class<GenericModuleBlockEntity> getBlockEntityClass() {
        return GenericModuleBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GenericModuleBlockEntity> getBlockEntityType() {
        return MyAddonBlockEntity.GENERIC_MODULE.get();
    }
}
