package com.yourname.mycreateaddon.crafting;


import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;


/**
 * 인공 광맥 노드의 특수 효과(Quirk)를 정의하는 열거형입니다.
 */
public enum Quirk {
    // --- Common Tier ---
    STEADY_HANDS("steady_hands", Tier.COMMON, MyAddonItems.RAW_STEEL_CHUNK),
    SIGNAL_AMPLIFICATION("signal_amplification", Tier.COMMON, MyAddonItems.CINNABAR),
    WILD_MAGIC("wild_magic", Tier.COMMON, MyAddonItems.XOMV),
    PETRIFIED_HEART("petrified_heart", Tier.COMMON, MyAddonItems.SILKY_JEWEL),
    AQUIFER("aquifer", Tier.COMMON, MyAddonItems.ULTRAMARINE),
    TRACE_MINERALS("trace_minerals", Tier.COMMON, MyAddonItems.CINNABAR),

    // --- Rare Tier ---
    STATIC_CHARGE("static_charge", Tier.RARE, MyAddonItems.THUNDER_STONE),
    BONE_CHILL("bone_chill", Tier.RARE, MyAddonItems.THE_FOSSIL),
    BOTTLED_KNOWLEDGE("bottled_knowledge", Tier.RARE, MyAddonItems.ULTRAMARINE),
    PURIFYING_RESONANCE("purifying_resonance", Tier.RARE, MyAddonItems.IVORY_CRYSTAL),
    POLARITY_POSITIVE("polarity_positive", Tier.RARE, MyAddonItems.CINNABAR),
    POLARITY_NEGATIVE("polarity_negative", Tier.RARE, MyAddonItems.CINNABAR),
    BURIED_TREASURE("buried_treasure", Tier.RARE, MyAddonItems.SILKY_JEWEL),

    // --- Epic Tier ---
    OVERLOAD_DISCHARGE("overload_discharge", Tier.EPIC, MyAddonItems.THUNDER_STONE),
    WITHERING_ECHO("withering_echo", Tier.EPIC, MyAddonItems.THE_FOSSIL),
    GEMSTONE_FACETS("gemstone_facets", Tier.EPIC, MyAddonItems.KOH_I_NOOR),
    AURA_OF_VITALITY("aura_of_vitality", Tier.EPIC, MyAddonItems.ROSE_GOLD),
    CHAOTIC_OUTPUT("chaotic_output", Tier.EPIC, MyAddonItems.XOMV),
    VOLATILE_FISSURES("volatile_fissures", Tier.EPIC, MyAddonItems.ROSE_GOLD);


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
     * @return 번역 키에 사용될 고유 ID (예: "quirk.mycreateaddon.steady_hands")
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
        return Component.translatable("quirk.mycreateaddon." + this.id);
    }

    /**
     * @return 번역된 특성 설명
     */
    public Component getDescription() {
        return Component.translatable("quirk.mycreateaddon." + this.id + ".description");
    }
}