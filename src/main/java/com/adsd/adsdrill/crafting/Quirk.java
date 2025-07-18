package com.adsd.adsdrill.crafting;


import com.adsd.adsdrill.registry.AdsDrillItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;


/**
 * 인공 광맥 노드의 특수 효과(Quirk)를 정의하는 열거형입니다.
 */
public enum Quirk {
    // --- Common Tier ---
    STEADY_HANDS("steady_hands", Tier.COMMON, AdsDrillItems.RAW_STEEL_CHUNK),
    SIGNAL_AMPLIFICATION("signal_amplification", Tier.COMMON, AdsDrillItems.CINNABAR),
    WILD_MAGIC("wild_magic", Tier.COMMON, AdsDrillItems.XOMV),
    PETRIFIED_HEART("petrified_heart", Tier.COMMON, AdsDrillItems.SILKY_JEWEL),
    AQUIFER("aquifer", Tier.COMMON, AdsDrillItems.ULTRAMARINE),
    TRACE_MINERALS("trace_minerals", Tier.COMMON, AdsDrillItems.CINNABAR),

    // --- Rare Tier ---
    STATIC_CHARGE("static_charge", Tier.RARE, AdsDrillItems.THUNDER_STONE),
    BONE_CHILL("bone_chill", Tier.RARE, AdsDrillItems.THE_FOSSIL),
    BOTTLED_KNOWLEDGE("bottled_knowledge", Tier.RARE, AdsDrillItems.ULTRAMARINE),
    PURIFYING_RESONANCE("purifying_resonance", Tier.RARE, AdsDrillItems.IVORY_CRYSTAL),
    POLARITY_POSITIVE("polarity_positive", Tier.RARE, AdsDrillItems.CINNABAR),
    POLARITY_NEGATIVE("polarity_negative", Tier.RARE, AdsDrillItems.CINNABAR),
    BURIED_TREASURE("buried_treasure", Tier.RARE, AdsDrillItems.SILKY_JEWEL),

    // --- Epic Tier ---
    OVERLOAD_DISCHARGE("overload_discharge", Tier.EPIC, AdsDrillItems.THUNDER_STONE),
    WITHERING_ECHO("withering_echo", Tier.EPIC, AdsDrillItems.THE_FOSSIL),
    GEMSTONE_FACETS("gemstone_facets", Tier.EPIC, AdsDrillItems.KOH_I_NOOR),
    AURA_OF_VITALITY("aura_of_vitality", Tier.EPIC, AdsDrillItems.ROSE_GOLD),
    CHAOTIC_OUTPUT("chaotic_output", Tier.EPIC, AdsDrillItems.XOMV),
    VOLATILE_FISSURES("volatile_fissures", Tier.EPIC, AdsDrillItems.ROSE_GOLD);


    public enum Tier {
        COMMON("Common", ChatFormatting.WHITE),
        RARE("Rare", ChatFormatting.BLUE),
        EPIC("Epic", ChatFormatting.LIGHT_PURPLE);

        private final String name;
        private final ChatFormatting color;

        Tier(String name, ChatFormatting color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public ChatFormatting getColor() {
            return color;
        }
    }

    private final String id;
    private final Tier tier;
    private final Supplier<Item> catalyst; // 이 특성을 부여하는 주요 촉매 아이템

    Quirk(String id, Tier tier, Supplier<Item> catalyst) {
        this.id = id;
        this.tier = tier;
        this.catalyst = catalyst;
    }

    /**
     * @return 번역 키에 사용될 고유 ID (예: "quirk.adsdrill.steady_hands")
     */
    public String getId() {
        return id;
    }

    public Tier getTier() {
        return tier;
    }

    /**
     * @return 이 특성을 부여하는 주된 촉매 아이템 (Supplier)
     */
    public Supplier<Item> getCatalyst() {
        return catalyst;
    }

    /**
     * @return 번역된 특성 이름 (예: "굳건한 기반")
     */
    public Component getDisplayName() {
        return Component.translatable("quirk.adsdrill." + this.id);
    }

    /**
     * @return 번역된 특성 설명
     */
    public Component getDescription() {
        return Component.translatable("quirk.adsdrill." + this.id + ".description");
    }
}