package com.adsd.adsdrill.content.kinetics.node;


import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.crafting.Quirk;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// OreNodeBlockEntity를 상속받습니다.
public class ArtificialNodeBlockEntity extends OreNodeBlockEntity {

    // 인공 노드의 고유한 배경 블록 ID
    private static final ResourceLocation ARTIFICIAL_CASING_ID =
            ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, "block/artificial_node");
    // 이 노드가 가진 특수 효과 목록을 저장할 필드를 추가합니다.
    private final List<Quirk> quirks = new ArrayList<>();
    private int volatileFissureCounter = 0;

    public ArtificialNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    public void configureFromCrafting(
            List<Quirk> quirks,
            Map<Item, Float> composition,
            Map<Item, Block> itemToBlockMap,
            int maxYield,
            float hardness,
            float richness,
            float regeneration,
            FluidStack fluid,
            int fluidCapacity
    ) {
        // 1. 부모 클래스의 설정 메서드를 호출하여 기본 노드 속성을 설정합니다.
        super.configureNode(composition, itemToBlockMap, maxYield, hardness, richness, regeneration, fluid, fluidCapacity);

        // 2. 이 클래스 고유의 데이터(특성)를 설정합니다.
        this.quirks.clear();
        this.quirks.addAll(quirks);

        // 3. 변경사항을 저장하고 클라이언트에 동기화합니다.
        // 부모의 configureNode에서 이미 setChanged를 호출하므로 여기서 또 호출할 필요는 없습니다.
    }

    // 외부에서 이 노드가 특정 특수 효과를 가졌는지 확인할 수 있는 public 메서드
    public boolean hasQuirk(Quirk quirk) {
        return this.quirks.contains(quirk);
    }
    public void incrementVolatileFissureCounter() {
        if (!hasQuirk(Quirk.VOLATILE_FISSURES)) return;

        this.volatileFissureCounter++;
        if (this.volatileFissureCounter >= 10) {
            this.volatileFissureCounter = 0;
            if (level != null && !level.isClientSide) {
                // 폭발 효과 (엔티티 피해, 블록 파괴 없음)
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1.5f, Level.ExplosionInteraction.NONE);
                // 균열 상태 설정
                this.setCracked(true);
            }
        }
    }
    @Override
    public ResourceLocation getBackgroundBlockId() {
        return ARTIFICIAL_CASING_ID;
    }

    // NBT 저장/로드 로직을 추가하여 특수 효과 목록을 영구적으로 보존합니다.
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
        tag.putInt("VolatileCounter", volatileFissureCounter);
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
        volatileFissureCounter = tag.getInt("VolatileCounter");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // 부모 클래스(OreNodeBlockEntity)의 툴팁을 먼저 표시합니다.
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (!quirks.isEmpty()) {
            tooltip.add(Component.literal("")); // 구분선
            tooltip.add(Component.translatable("adsdrill.quirk.header").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            for (Quirk quirk : quirks) {
                Quirk.Tier tier = quirk.getTier();
                MutableComponent tierComponent = Component.literal("[" + tier.getName() + "] ").withStyle(tier.getColor());

                tooltip.add(Component.literal(" - ").append(tierComponent).append(quirk.getDisplayName().copy().withStyle(ChatFormatting.AQUA)));

                // 쉬프트 누를 때만 설명 표시
                if (isPlayerSneaking) {
                    // [!!! 핵심 수정: 긴 설명을 여러 줄로 나누어 추가하는 로직 !!!]
                    Font font = Minecraft.getInstance().font;
                    Component description = quirk.getDescription();
                    int maxWidth = 200; // 툴팁 한 줄의 최대 너비 (픽셀 단위)
                    String indentation = "    "; // 들여쓰기

                    // 설명이 비어있지 않은 경우에만 처리
                    if (!description.getString().isEmpty()) {
                        String[] words = description.getString().split(" ");
                        StringBuilder currentLine = new StringBuilder();

                        for (String word : words) {
                            // 현재 줄에 다음 단어를 추가했을 때 최대 너비를 초과하는지 확인
                            if (font.width(currentLine.toString() + word) > maxWidth) {
                                // 최대 너비를 초과하면, 현재까지의 줄을 툴팁에 추가
                                tooltip.add(Component.literal(indentation + currentLine.toString().trim()).withStyle(ChatFormatting.GRAY));
                                // 새 줄 시작
                                currentLine = new StringBuilder();
                            }
                            currentLine.append(word).append(" ");
                        }

                        // 마지막 줄 추가
                        if (!currentLine.toString().trim().isEmpty()) {
                            tooltip.add(Component.literal(indentation + currentLine.toString().trim()).withStyle(ChatFormatting.GRAY));
                        }
                    }
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
                    if (random.nextFloat() < 0.05f) { // 3% 확률
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