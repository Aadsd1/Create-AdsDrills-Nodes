package com.yourname.mycreateaddon.content.kinetics.node;


import com.yourname.mycreateaddon.MyCreateAddon; // 모드 메인 클래스
import com.yourname.mycreateaddon.crafting.Quirk;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

// OreNodeBlockEntity를 상속받습니다.
public class ArtificialNodeBlockEntity extends OreNodeBlockEntity {

    // [핵심] 인공 노드의 고유한 배경 블록 ID
    private static final ResourceLocation ARTIFICIAL_CASING_ID =
            ResourceLocation.fromNamespaceAndPath(MyCreateAddon.MOD_ID, "block/artificial_node");
    // [1] 이 노드가 가진 특수 효과 목록을 저장할 필드를 추가합니다.
    private final List<Quirk> quirks = new ArrayList<>();

    public ArtificialNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    // [1. 추가] 외부에서 이 노드가 특정 특수 효과를 가졌는지 확인할 수 있는 public 메서드
    public boolean hasQuirk(Quirk quirk) {
        return this.quirks.contains(quirk);
    }

    // getBackgroundBlockId 메서드를 오버라이드하여 항상 커스텀 케이싱을 반환합니다.
    @Override
    public ResourceLocation getBackgroundBlockId() {
        return ARTIFICIAL_CASING_ID;
    }
    // [2] NBT 저장/로드 로직을 추가하여 특수 효과 목록을 영구적으로 보존합니다.
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!quirks.isEmpty()) {
            ListTag quirkListTag = new ListTag();
            for (Quirk quirk : quirks) {
                CompoundTag quirkTag = new CompoundTag();
                quirkTag.putString("id", quirk.name());
                quirkListTag.add(quirkTag);
            }
            tag.put("Quirks", quirkListTag);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        quirks.clear();
        if (tag.contains("Quirks", 9)) { // 9 = ListTag
            ListTag quirkListTag = tag.getList("Quirks", 10); // 10 = CompoundTag
            for (int i = 0; i < quirkListTag.size(); i++) {
                try {
                    String quirkId = quirkListTag.getCompound(i).getString("id");
                    quirks.add(Quirk.valueOf(quirkId));
                } catch (IllegalArgumentException e) {
                    // 저장된 id가 Quirk enum에 없는 경우(예: 버전 업데이트로 삭제) 무시
                }
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // 부모 클래스(OreNodeBlockEntity)의 툴팁을 먼저 표시합니다.
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (!quirks.isEmpty()) {
            tooltip.add(Component.literal("")); // 구분선
            tooltip.add(Component.translatable("mycreateaddon.quirk.header").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            for (Quirk quirk : quirks) {
                // [!!! 핵심 수정 !!!]
                // getDisplayName()이 반환한 Component를 copy()하여 MutableComponent로 만든 후 스타일을 적용합니다.
                tooltip.add(Component.literal(" - ").append(quirk.getDisplayName().copy().withStyle(ChatFormatting.AQUA)));

                // 쉬프트 누를 때만 설명 표시
                if (isPlayerSneaking) {
                    tooltip.add(Component.literal("    ").append(quirk.getDescription()));
                }
            }
        }
        return true;
    }


    public void applyQuirkEffects(List<ItemStack> drops, ServerLevel level) {
        if (quirks.isEmpty() || drops.isEmpty()) return;

        RandomSource random = level.getRandom();

        for (Quirk quirk : quirks) {
            switch (quirk) {
                case STEADY_HANDS:
                    // 최소 드롭량 보장은 getNormalDrops에서 처리되므로 여기선 할 일 없음.
                    break;
                case STATIC_CHARGE:
                    if (level.isRaining() && random.nextFloat() < 0.1f) { // 10% 확률
                        for (ItemStack stack : drops) {
                            stack.grow(stack.getCount()); // 결과물 2배
                        }
                    }
                    break;
                case BONE_CHILL:
                    if (random.nextFloat() < 0.01f) { // 1% 확률
                        Skeleton skeleton = new Skeleton(EntityType.SKELETON, level);
                        skeleton.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, random.nextFloat() * 360, 0);
                        level.addFreshEntity(skeleton);
                    }
                    break;
                case BOTTLED_KNOWLEDGE:
                    if (random.nextFloat() < 0.02f) { // 2% 확률
                        drops.add(new ItemStack(Items.EXPERIENCE_BOTTLE));
                    }
                    break;
                case AURA_OF_VITALITY:
                    // 주변 플레이어 체력 회복
                    if (random.nextFloat() < 0.005f) { // 0.5% 확률
                        level.getEntitiesOfClass(Player.class, getRenderBoundingBox().inflate(8)).forEach(player -> {
                            player.heal(2.0f); // 하트 1칸 회복
                        });
                    }
                    break;
                case SIGNAL_AMPLIFICATION:
                    if (level.hasNeighborSignal(getBlockPos())) {
                        for (ItemStack stack : drops) {
                            int bonus = (int) Math.ceil(stack.getCount() * 0.1);
                            stack.grow(bonus);
                        }
                    }
                    break;
                case WILD_MAGIC:
                    if (random.nextFloat() < 0.03f) { // 3% 확률
                        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, random.nextFloat() * 2.0f);
                        level.sendParticles(ParticleTypes.NOTE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 5, 0.5, 0.5, 0.5, 0);
                    }
                    break;
                // 다른 케이스들은 다른 곳에서 처리됩니다.
                default:
                    break;
            }
        }
    }
}