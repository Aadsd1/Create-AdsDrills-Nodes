package com.yourname.mycreateaddon.content.kinetics.module.Frame;

import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.*;
import java.util.function.Consumer;




public class FrameModuleVisual extends KineticBlockEntityVisual<FrameModuleBlockEntity> {

    protected final Map<Direction, RotatingInstance> moduleShafts = new EnumMap<>(Direction.class);

    public FrameModuleVisual(VisualizationContext context, FrameModuleBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        update(partialTick);
    }

    @Override
    public void update(float partialTick) {

        CompoundTag nbt = blockEntity.getClientVisualNBT();

        Set<Direction> requiredShafts = new HashSet<>();
        if (nbt.contains("VisualConnections", 11)) { // 11 = IntArrayTag
            int[] dirs = nbt.getIntArray("VisualConnections");
            for (int dirOrdinal : dirs) {
                if (dirOrdinal >= 0 && dirOrdinal < Direction.values().length) {
                    requiredShafts.add(Direction.values()[dirOrdinal]);
                }
            }
        }
        float speed = nbt.getFloat("VisualSpeed");


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

    // --- 나머지 메서드는 기존과 동일 ---
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