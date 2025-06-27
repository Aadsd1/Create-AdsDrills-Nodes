package com.yourname.mycreateaddon.content.kinetics.module.Frame;

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



public class FrameModuleVisual extends KineticBlockEntityVisual<FrameModuleBlockEntity> {

    protected final Map<Direction, RotatingInstance> moduleShafts = new EnumMap<>(Direction.class);

    public FrameModuleVisual(VisualizationContext context, FrameModuleBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }


    @Override
    public void update(float partialTick) {
        super.update(partialTick);

        Set<Direction> requiredShafts = blockEntity.getVisualConnections();
        float speed = blockEntity.getVisualSpeed();
        boolean changed = false; // <<< 라이팅 업데이트를 위한 플래그

        // 현재 존재하는 모든 샤프트 방향을 순회
        for (Direction dir : moduleShafts.keySet().stream().toList()) {
            if (!requiredShafts.contains(dir)) {
                moduleShafts.get(dir).delete();
                moduleShafts.remove(dir);
                changed = true; // <<< 변경 발생
            }
        }

        // 필요한 모든 샤프트 방향을 순회
        for (Direction dir : requiredShafts) {
            if (!moduleShafts.containsKey(dir)) {
                RotatingInstance newShaft = createInstanceFor(MyAddonPartialModels.SHAFT_FOR_MODULE);
                newShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, dir);
                moduleShafts.put(dir, newShaft);
                changed = true; // <<< 변경 발생
            }
            moduleShafts.get(dir).setup(blockEntity, dir.getAxis(), speed).setChanged();
        }

        // ▼▼▼ 라이팅 업데이트 호출 ▼▼▼
        if (changed) {
            this.updateLight(partialTick);
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