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
    // [핵심 수정] 커넥터도 RotatingInstance를 사용하되, 속도를 0으로 설정합니다.
    protected final Map<Direction, RotatingInstance> energyConnectors = new EnumMap<>(Direction.class);

    public GenericModuleVisual(VisualizationContext context, GenericModuleBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        update(partialTick);
    }

    @Override
    public void update(float partialTick) {
        updateMechanicalShafts(partialTick);
        updateEnergyConnectors(partialTick);
    }

    private void updateMechanicalShafts(float partialTick) {
        Set<Direction> requiredShafts = blockEntity.getVisualConnections();
        float speed = blockEntity.getVisualSpeed();
        boolean changed = false;

        moduleShafts.keySet().removeIf(dir -> {
            if (!requiredShafts.contains(dir)) {
                moduleShafts.get(dir).delete();
                return true;
            }
            return false;
        });

        for (Direction dir : requiredShafts) {
            if (!moduleShafts.containsKey(dir)) {
                RotatingInstance newShaft = createRotatingInstanceFor(MyAddonPartialModels.SHAFT_FOR_MODULE);
                newShaft.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, dir);
                moduleShafts.put(dir, newShaft);
                changed = true;
            }
        }

        for (Map.Entry<Direction, RotatingInstance> entry : moduleShafts.entrySet()) {
            entry.getValue().setup(blockEntity, entry.getKey().getAxis(), speed).setChanged();
        }
        if (changed) updateLight(partialTick);
    }

    private void updateEnergyConnectors(float partialTick) {
        if (blockEntity.getModuleType() != ModuleType.ENERGY_INPUT) {
            if (!energyConnectors.isEmpty()) {
                energyConnectors.values().forEach(Instance::delete);
                energyConnectors.clear();
            }
            return;
        }

        Set<Direction> requiredConnectors = blockEntity.getEnergyConnections();
        boolean changed = false;

        energyConnectors.keySet().removeIf(dir -> {
            if (!requiredConnectors.contains(dir)) {
                energyConnectors.get(dir).delete();
                return true;
            }
            return false;
        });

        for (Direction dir : requiredConnectors) {
            if (!energyConnectors.containsKey(dir)) {
                // [핵심 수정] RotatingInstance를 생성합니다.
                RotatingInstance newConnector = createRotatingInstanceFor(MyAddonPartialModels.ENERGY_PORT);

                // 위치와 방향을 설정합니다.
                newConnector.setPosition(getVisualPosition())
                        .rotateToFace(Direction.UP, dir); // 모델의 기본 방향(SOUTH)에서 목표 방향(dir)으로 회전

                energyConnectors.put(dir, newConnector);
                changed = true;
            }
        }

        // [핵심 수정] 모든 커넥터의 속도를 0으로 설정하여 정적인 상태로 만듭니다.
        for (Map.Entry<Direction, RotatingInstance> entry : energyConnectors.entrySet()) {
            entry.getValue().setup(blockEntity, entry.getKey().getAxis(), 0f).setChanged(); // 속도를 0으로 고정
        }

        if (changed) updateLight(partialTick);
    }

    private RotatingInstance createRotatingInstanceFor(PartialModel model) {
        return instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model)).createInstance();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(moduleShafts.values().toArray(new RotatingInstance[0]));
        relight(energyConnectors.values().toArray(new RotatingInstance[0]));
    }

    @Override
    protected void _delete() {
        moduleShafts.values().forEach(Instance::delete);
        moduleShafts.clear();
        energyConnectors.values().forEach(Instance::delete);
        energyConnectors.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        moduleShafts.values().forEach(consumer);
        energyConnectors.values().forEach(consumer);
    }
}