
package com.yourname.mycreateaddon.content.kinetics.drill;

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
        inputShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, facing);
        coreShafts.put(CoreShaft.INPUT, inputShaft);

        RotatingInstance outputShaft = createInstanceFor(MyAddonPartialModels.SHAFT_FOR_DRILL);
        outputShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, facing.getOpposite());
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
        float speed = blockEntity.getSpeed();

        // 인풋/아웃풋 샤프트는 'primaryAxis'로 회전
        coreShafts.get(CoreShaft.INPUT).setup(blockEntity, primaryAxis, (sourceDirection == facing) ? speed : 0f).setChanged();
        coreShafts.get(CoreShaft.OUTPUT).setup(blockEntity, primaryAxis, 0f).setChanged();

        boolean changed = false;

        // 동적 모듈 샤프트 업데이트
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == primaryAxis) continue;

            boolean shouldExist = blockEntity.isModuleConnectedAt(dir);
            boolean doesExist = moduleShafts.containsKey(dir);

            if (shouldExist && !doesExist) {
                RotatingInstance newShaft = createInstanceFor(MyAddonPartialModels.SHAFT_FOR_MODULE);
                newShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, dir);
                moduleShafts.put(dir, newShaft);
                changed = true; // 변경 발생
            } else if (!shouldExist && doesExist) {
                moduleShafts.get(dir).delete();
                moduleShafts.remove(dir);
                changed = true; // 변경 발생
            }

            // 속도 갱신은 shouldExist일 때만 수행
            if (shouldExist) {
                moduleShafts.get(dir).setup(blockEntity, dir.getAxis(), speed).setChanged();
            }
        }

        if (changed) {
            // 인스턴스 목록이 변경되었으므로, 즉시 라이팅을 다시 계산합니다.
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