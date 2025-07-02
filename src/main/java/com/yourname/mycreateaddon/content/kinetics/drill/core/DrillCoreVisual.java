
package com.yourname.mycreateaddon.content.kinetics.drill.core;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
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

        RotatingInstance outputShaft = createInstanceFor(MyAddonPartialModels.SHAFT_FOR_DRILL);
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
        // [핵심 변경] 모듈과 헤드의 최종 속도는 BE에서 계산된 값을 그대로 사용합니다.
        float finalSpeed = blockEntity.isStructureValid() ? blockEntity.getSpeed() : 0f;

        // 입력 샤프트 렌더링
        coreShafts.get(CoreShaft.INPUT).setup(blockEntity, primaryAxis, (sourceDirection == facing) ? inputSpeed : 0f).setChanged();

        // [핵심 변경] 출력축의 회전 여부를 명확한 조건으로 제어합니다.
        // 조건: 모듈 구조가 유효하고(isStructureValid), 헤드가 존재할 때(hasHead)만 회전
        boolean shouldOutputShaftSpin = blockEntity.isStructureValid() && blockEntity.hasHead();
        float outputSpeed = shouldOutputShaftSpin ? finalSpeed : 0f;
        coreShafts.get(CoreShaft.OUTPUT).setup(blockEntity, primaryAxis, -outputSpeed).setChanged();

        // 모듈 샤프트 렌더링 (이 부분은 기존과 거의 동일)
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == primaryAxis) continue;

            boolean shouldExist = blockEntity.isModuleConnectedAt(dir);
            boolean doesExist = moduleShafts.containsKey(dir);

            if (shouldExist && !doesExist) {
                RotatingInstance newShaft = createInstanceFor(MyAddonPartialModels.SHAFT_FOR_MODULE);
                newShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, dir);
                moduleShafts.put(dir, newShaft);
                changed = true;
            } else if (!shouldExist && doesExist) {
                moduleShafts.remove(dir).delete();
                changed = true;
            }

            if (shouldExist) {
                // 모듈 샤프트는 모듈 구조만 유효하면 회전합니다.
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