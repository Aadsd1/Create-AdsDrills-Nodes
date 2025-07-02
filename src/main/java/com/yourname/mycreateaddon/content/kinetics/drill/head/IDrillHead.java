package com.yourname.mycreateaddon.content.kinetics.drill.head;


import com.yourname.mycreateaddon.content.kinetics.drill.core.DrillCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IDrillHead {

    /**
     * DrillCoreBlockEntity에 의해 매 틱 호출되어 실제 채굴 작업을 수행합니다.
     * @param level 월드
     * @param headPos 헤드 자신의 위치
     * @param headState 헤드 자신의 블록 상태
     * @param core The DrillCoreBlockEntity instance controlling this head.
     */
    void onDrillTick(Level level, BlockPos headPos, BlockState headState, DrillCoreBlockEntity core);

    // --- [추가] 아래 메서드들을 추가합니다. ---

    /**
     * 드릴이 작동할 때 틱당 발생하는 열의 양을 반환합니다.
     * @return 틱당 열 발생량
     */
    float getHeatGeneration();

    /**
     * 헤드 자체의 기본 방열 성능을 반환합니다.
     * 코어의 기본 냉각률에 더해집니다.
     * @return 틱당 열 냉각량
     */
    float getCoolingRate();
}