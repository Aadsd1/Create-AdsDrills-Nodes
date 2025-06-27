package com.yourname.mycreateaddon.content.kinetics.module;

import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;

import java.util.*;
import java.util.function.Consumer;




public class GenericModuleVisual extends KineticBlockEntityVisual<GenericModuleBlockEntity> {

    protected final Map<Direction, RotatingInstance> moduleShafts = new EnumMap<>(Direction.class);

    // 생성자 이름 변경
    public GenericModuleVisual(VisualizationContext context, GenericModuleBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        // 생성자에서 update를 호출하여 재접속 시 렌더링을 보장
        update(partialTick);
    }

    @Override
    public void update(float partialTick) {
        // BlockEntity의 getter를 통해 현재 렌더링해야 할 상태를 가져옵니다.
        Set<Direction> requiredShafts = blockEntity.getVisualConnections();
        float speed = blockEntity.getVisualSpeed();
        boolean changed = false;

        // 존재하지만 더 이상 필요 없는 샤프트 제거
        for (Direction dir : moduleShafts.keySet().stream().toList()) {
            if (!requiredShafts.contains(dir)) {
                moduleShafts.remove(dir).delete();
                changed = true;
            }
        }

        // 필요하지만 아직 없는 샤프트 생성
        for (Direction dir : requiredShafts) {
            if (!moduleShafts.containsKey(dir)) {
                RotatingInstance newShaft = createInstanceFor(MyAddonPartialModels.SHAFT_FOR_MODULE);
                newShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, dir);
                moduleShafts.put(dir, newShaft);
                changed = true;
            }
        }

        // 모든 샤프트의 속도 업데이트
        for (Map.Entry<Direction, RotatingInstance> entry : moduleShafts.entrySet()) {
            entry.getValue().setup(blockEntity, entry.getKey().getAxis(), speed).setChanged();
        }

        if (changed) {
            updateLight(partialTick);
        }
    }

    private RotatingInstance createInstanceFor(PartialModel model) {
        return instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model))
                .createInstance();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(moduleShafts.values().toArray(new RotatingInstance[0]));
    }

    @Override
    protected void _delete() {
        moduleShafts.values().forEach(Instance::delete);
        moduleShafts.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        moduleShafts.values().forEach(consumer);
    }
}