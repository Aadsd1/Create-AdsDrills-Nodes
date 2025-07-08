package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.yourname.mycreateaddon.etc.MyAddonPartialModels;
import com.yourname.mycreateaddon.registry.MyAddonBlocks;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;

import java.util.function.Consumer;





public class RotaryDrillHeadVisual extends KineticBlockEntityVisual<RotaryDrillHeadBlockEntity> {


    protected RotatingInstance bodyModel;
    protected RotatingInstance tipModel;

    // 현재 팁 모델이 무엇인지 추적하기 위한 변수
    private PartialModel currentTipPartial = null;

    public RotaryDrillHeadVisual(VisualizationContext context, RotaryDrillHeadBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        // 1. 몸통 모델 생성
        PartialModel bodyPartial;
        if (this.blockState.getBlock() == MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD.get()) {
            bodyPartial = MyAddonPartialModels.NETHERITE_DRILL_BODY;
        } else if (this.blockState.getBlock() == MyAddonBlocks.DIAMOND_ROTARY_DRILL_HEAD.get()) {
            bodyPartial = MyAddonPartialModels.DIAMOND_DRILL_BODY;
        } else {
            bodyPartial = MyAddonPartialModels.IRON_DRILL_BODY;
        }

        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);

        bodyModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(bodyPartial)).createInstance();
        bodyModel.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, facing);

        update(partialTick);
    }
//        rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING,
    //              Models.partial(MyAddonPartialModels.DIAMOND_ROTARY_DRILL_HEAD)).createInstance();

    private void updateRotation() {
        Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);
        float speed = this.blockEntity.getVisualSpeed();

        bodyModel.setPosition(getVisualPosition()).setup(blockEntity, facing.getAxis(), speed).setChanged();
        if (tipModel != null) {
            tipModel.setPosition(getVisualPosition()).setup(blockEntity, facing.getAxis(), speed).setChanged();
        }
    }

    @Override
    public void update(float partialTick) {
        // 회전 업데이트
        updateRotation();

        // --- 색상 제어 ---
        // 1. 가열 색상 계산
        float heat = blockEntity.getClientHeat();
        float heatRatio = heat / 100f;
        int heatR = 255;
        int heatG = 255 - (int) (heatRatio * (255 - 96));
        int heatB = 255 - (int) (heatRatio * (255 - 96));
        Color heatOverlayColor = new Color(heatR, heatG, heatB);

        // 2. 몸통 모델에는 가열 색상만 적용
        bodyModel.setColor(heatOverlayColor);

        // 3. 팁 모델 동적 교체 및 색상 적용
        updateTipModel(heatOverlayColor);
    }

    private void updateTipModel(Color heatOverlayColor) {
        // 1. 이번 프레임에 렌더링해야 할 팁 모델을 결정합니다.
        PartialModel requiredTipPartial;

        if (blockEntity.hasSilkTouch()) {
            requiredTipPartial = MyAddonPartialModels.EMERALD_DRILL_TIP;
        } else if (blockEntity.getFortuneLevel() > 0) {
            requiredTipPartial = MyAddonPartialModels.GOLD_DRILL_TIP;
        } else {
            if (this.blockState.getBlock() == MyAddonBlocks.NETHERITE_ROTARY_DRILL_HEAD.get()) {
                requiredTipPartial = MyAddonPartialModels.NETHERITE_DRILL_TIP;
            } else if (this.blockState.getBlock() == MyAddonBlocks.DIAMOND_ROTARY_DRILL_HEAD.get()) {
                requiredTipPartial = MyAddonPartialModels.DIAMOND_DRILL_TIP;
            } else {
                requiredTipPartial = MyAddonPartialModels.IRON_DRILL_TIP;
            }
        }

        // 2. 현재 팁 모델과 필요한 모델이 다를 경우, 교체합니다.
        if (currentTipPartial != requiredTipPartial) {
            if (tipModel != null) {
                tipModel.delete();
            }
            Direction facing = this.blockState.getValue(DirectionalKineticBlock.FACING);
            tipModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(requiredTipPartial)).createInstance();
            tipModel.setPosition(getVisualPosition()).rotateToFace(Direction.SOUTH, facing);
            currentTipPartial = requiredTipPartial;
            relight(tipModel);
        }

        // 3. [핵심 수정] 현재 팁 모델에 '가열 효과'만 적용합니다.
        // 모델 자체가 가진 고유 텍스처 색상 위에 가열 틴트(흰색~붉은색)만 덧씌웁니다.
        if (tipModel != null) {
            tipModel.setColor(heatOverlayColor);
        }
    }

    @Override
    protected void _delete() {
        bodyModel.delete();
        tipModel.delete();
    }

    @Override
    public void updateLight(float partialTick) {

        relight(bodyModel);
        relight(tipModel);
    }


    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {

        consumer.accept(bodyModel);
        consumer.accept(tipModel);

    }

}
