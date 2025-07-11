package com.yourname.mycreateaddon.crafting;


import com.yourname.mycreateaddon.registry.MyAddonItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

/**
 * 인공 광맥 노드의 특수 효과(Quirk)를 정의하는 열거형입니다.
 */
public enum Quirk {
    // --- 특수 효과 목록 ---
    STEADY_HANDS("steady_hands", MyAddonItems.RAW_STEEL_CHUNK),
    STATIC_CHARGE("static_charge", MyAddonItems.THUNDER_STONE),
    OVERLOAD_DISCHARGE("overload_discharge", MyAddonItems.THUNDER_STONE),
    BONE_CHILL("bone_chill", MyAddonItems.THE_FOSSIL),
    WITHERING_ECHO("withering_echo", MyAddonItems.THE_FOSSIL),
    BOTTLED_KNOWLEDGE("bottled_knowledge", MyAddonItems.ULTRAMARINE),
    AURA_OF_VITALITY("aura_of_vitality", MyAddonItems.ROSE_GOLD),
    PURIFYING_RESONANCE("purifying_resonance", MyAddonItems.IVORY_CRYSTAL),
    POLARITY_POSITIVE("polarity_positive", MyAddonItems.CINNABAR),
    POLARITY_NEGATIVE("polarity_negative", MyAddonItems.CINNABAR),
    SIGNAL_AMPLIFICATION("signal_amplification", MyAddonItems.CINNABAR),
    GEMSTONE_FACETS("gemstone_facets", MyAddonItems.KOH_I_NOOR),
    CHAOTIC_OUTPUT("chaotic_output", MyAddonItems.XOMV),
    WILD_MAGIC("wild_magic", MyAddonItems.XOMV);


    private final String id;
    private final Supplier<Item> catalyst; // 이 특성을 부여하는 주요 촉매 아이템

    Quirk(String id, Supplier<Item> catalyst) {
        this.id = id;
        this.catalyst = catalyst;
    }

    /**
     * @return 번역 키에 사용될 고유 ID (예: "quirk.mycreateaddon.steady_hands")
     */
    public String getId() {
        return id;
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