
package com.adsd.adsdrill.content.kinetics.drill.core;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.adsd.adsdrill.etc.AdsDrillPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class DrillCoreVisual extends KineticBlockEntityVisual<DrillCoreBlockEntity> {

    public enum CoreShaft { INPUT, OUTPUT }
    protected final Map<CoreShaft, RotatingInstance> coreShafts = new EnumMap<>(CoreShaft.class);
    protected final Map<Direction, RotatingInstance> moduleShafts = new EnumMap<>(Direction.class);
    protected Direction sourceDirection;

    public DrillCoreVisual(VisualizationContext context, DrillCoreBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);

        RotatingInstance inputShaft = createInstanceFor(AllPartialModels.SHAFT_HALF);
        // 입력 샤프트는 FACING 방향에 위치합니다.
        inputShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH,facing);
        coreShafts.put(CoreShaft.INPUT, inputShaft);

        RotatingInstance outputShaft = createInstanceFor(AdsDrillPartialModels.SHAFT_FOR_DRILL);
        // 출력 샤프트는 FACING의 반대 방향에 위치합니다.
        outputShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH,facing.getOpposite());
        coreShafts.put(CoreShaft.OUTPUT, outputShaft);

        update(partialTick);
    }

    private RotatingInstance createInstanceFor(PartialModel model) {
        return instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model)).createInstance();
    }

    @Override
    public void update(float pt) {
        updateSourceDirection();

        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);
        Axis primaryAxis = facing.getAxis();

        float inputSpeed = blockEntity.getInputSpeed();
        // [핵심 수정] 클라이언트에서 직접 계산하는 대신, 동기화된 visualSpeed를 사용합니다.
        float finalSpeed = blockEntity.getVisualSpeed();

        // 입력 샤프트 렌더링
        coreShafts.get(CoreShaft.INPUT).setup(blockEntity, primaryAxis, (sourceDirection == facing) ? inputSpeed : 0f).setChanged();

        // 출력축 렌더링 (이제 finalSpeed가 0이면 알아서 멈춥니다)
        coreShafts.get(CoreShaft.OUTPUT).setup(blockEntity, primaryAxis, -finalSpeed).setChanged();

        // 모듈 샤프트 렌더링
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == primaryAxis) continue;

            boolean shouldExist = blockEntity.isModuleConnectedAt(dir);
            boolean doesExist = moduleShafts.containsKey(dir);

            if (shouldExist && !doesExist) {
                RotatingInstance newShaft = createInstanceFor(AdsDrillPartialModels.SHAFT_FOR_MODULE);
                newShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, dir);
                moduleShafts.put(dir, newShaft);
                changed = true;
            } else if (!shouldExist && doesExist) {
                moduleShafts.remove(dir).delete();
                changed = true;
            }

            if (shouldExist) {
                moduleShafts.get(dir).setup(blockEntity, dir.getAxis(), finalSpeed).setChanged();
            }
        }

        if (changed) {
            this.updateLight(pt);
        }
    }


    @Override
    public void updateLight(float partialTick) {
        relight(coreShafts.values().toArray(new RotatingInstance[0]));
        relight(moduleShafts.values().toArray(new RotatingInstance[0]));
    }

    @Override
    protected void _delete() {
        coreShafts.values().forEach(Instance::delete);
        moduleShafts.values().forEach(Instance::delete);
        coreShafts.clear();
        moduleShafts.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        coreShafts.values().forEach(consumer);
        moduleShafts.values().forEach(consumer);
    }

    protected void updateSourceDirection() {
        if (blockEntity.hasSource() && blockEntity.source != null) {
            BlockPos source = blockEntity.source.subtract(pos);
            sourceDirection = Direction.getNearest(source.getX(), source.getY(), source.getZ());
        } else {
            sourceDirection = null;
        }
    }
}