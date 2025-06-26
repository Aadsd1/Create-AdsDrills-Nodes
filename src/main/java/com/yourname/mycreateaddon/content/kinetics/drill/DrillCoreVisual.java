
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

    public enum ShaftComponent {
        INPUT_SHAFT,
        OUTPUT_SHAFT
    }

    protected final Map<ShaftComponent, RotatingInstance> components = new EnumMap<>(ShaftComponent.class);
    protected Direction sourceDirection;

    public DrillCoreVisual(VisualizationContext context, DrillCoreBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        // 1. 생성자에서 '변하지 않는' 위치와 방향을 단 한 번만 설정합니다.

        // 현재 블록의 방향과 회전축을 가져옵니다.
        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);


        // 입력 샤프트 생성 및 설정
        RotatingInstance inputShaft = createInstanceFor(AllPartialModels.SHAFT_HALF);
        inputShaft.setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, facing);
        components.put(ShaftComponent.INPUT_SHAFT, inputShaft);

        // 출력 샤프트 생성 및 설정
        RotatingInstance outputShaft = createInstanceFor(MyAddonPartialModels.SHAFT_FOR_DRILL);
        outputShaft.setPosition(getVisualPosition())
                .rotateToFace(Direction.SOUTH, facing.getOpposite());
        components.put(ShaftComponent.OUTPUT_SHAFT, outputShaft);

        // 초기 속도 설정을 위해 update 호출
        update(partialTick);
    }

    private RotatingInstance createInstanceFor(PartialModel model) {
        return instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model))
                .createInstance();
    }

    @Override
    public void update(float pt) {
        // 2. update에서는 '변하는' 정보인 속도만 갱신합니다.

        updateSourceDirection();

        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);
        Axis axis = facing.getAxis();
        float speed = blockEntity.getSpeed();

        // 입력 샤프트 속도 업데이트
        RotatingInstance inputShaft = components.get(ShaftComponent.INPUT_SHAFT);
        float inputSpeed = (sourceDirection == facing) ? speed : 0f;//.getOpposite()
        inputShaft.setup(blockEntity, axis, inputSpeed).setChanged();

        // 출력 샤프트 속도 업데이트
        RotatingInstance outputShaft = components.get(ShaftComponent.OUTPUT_SHAFT);
        float outputSpeed = 0f;//(sourceDirection != null) ? speed :
        outputShaft.setup(blockEntity, axis, outputSpeed).setChanged();
    }

    protected void updateSourceDirection() {
        if (blockEntity.hasSource() && blockEntity.source != null) {
            BlockPos source = blockEntity.source.subtract(pos);
            sourceDirection = Direction.getNearest(source.getX(), source.getY(), source.getZ());
        } else {
            sourceDirection = null;
        }
    }

    // --- 나머지 유틸리티 메서드들 ---

    @Override
    public void updateLight(float partialTick) {
        relight(components.values().toArray(new RotatingInstance[0]));
    }

    @Override
    protected void _delete() {
        components.values().forEach(Instance::delete);
        components.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        components.values().forEach(consumer);
    }
}