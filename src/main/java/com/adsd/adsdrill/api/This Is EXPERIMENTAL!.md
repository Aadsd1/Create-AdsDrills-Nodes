 ### WARNING: This API is Experimental!
 ### 경고: 이 API는 실험적인 기능입니다!

 This is an experimental, reflection-based API for addon developers. It is highly unstable, may break without any warning on future updates, and could have unintended side effects. **Use this at your own risk.** We offer no guarantees of support for addons using this API.
 *이것은 애드온 개발자를 위한 실험적인 리플렉션 기반 API입니다. 이 API는 매우 불안정하며, 향후 업데이트 시 아무런 경고 없이 손상될 수 있고, 예기치 않은 부작용을 일으킬 수 있습니다. **사용에 따르는 모든 책임은 사용자 본인에게 있습니다.** 저희는 이 API를 사용하는 애드온에 대한 지원을 보장하지 않습니다.*

 ---

 #### Why does this API exist?
 #### 이 API가 존재하는 이유

 The goal of this API is to allow addon developers to dynamically add new entries to the `ModuleType` and `Quirk` enums without needing to edit the AdsDrill source code directly. This allows for greater compatibility and easier development of new content that integrates with AdsDrill's core systems.
 *이 API의 목적은 애드온 개발자들이 AdsDrill의 소스 코드를 직접 수정하지 않고도 `ModuleType`과 `Quirk` 열거형(enum)에 새로운 항목을 동적으로 추가할 수 있게 하는 것입니다. 이를 통해 더 높은 호환성과 쉬운 콘텐츠 개발을 지원하고자 합니다.*

 #### How to Use
 #### 사용 방법

 All API calls should be made during the `FMLCommonSetupEvent`. Your mod **must** declare a dependency on AdsDrill in your `mods.toml` file to ensure it loads after AdsDrill has initialized.
 *모든 API 호출은 `FMLCommonSetupEvent` 도중에 이루어져야 합니다. 또한, 여러분의 모드가 AdsDrill 초기화 이후에 로드될 수 있도록 `mods.toml` 파일에 AdsDrill에 대한 의존성을 반드시 선언해야 합니다.*

 ---

 ### Adding a Custom `ModuleType`
 ### 커스텀 `ModuleType` 추가하기

 You can add a new module type that will be recognized by the Drill Core's structure scanning.
 *드릴 코어의 구조 스캔 시 인식될 수 있는 새로운 모듈 타입을 추가할 수 있습니다.*

 **API Call:**
 ```java
 ModuleTypeApi.addModuleType(
     String name,
     float stressImpact,
     float speedBonus,
     float heatModifier,
     int itemCapacity,
     int fluidCapacity,
     int energyCapacity,
     IModuleBehavior behavior,
     boolean isPerformanceModule
 );
 ```

 **Example:**
 *예시:*

 1.  First, create your own implementation of `IModuleBehavior`.
     *먼저, `IModuleBehavior`를 구현하는 자신만의 클래스를 만듭니다.*

     ```java
     // In your addon's package
     public class MyDrillBoosterBehavior implements IModuleBehavior {
         @Override
         public void onCoreTick(GenericModuleBlockEntity moduleBE, DrillCoreBlockEntity core) {
             // Example: Add particles or sounds when active.
             // The actual stat changes are handled by the parameters you provide.
             if (core.getLevel().getRandom().nextFloat() < 0.1f) {
                 core.getLevel().addParticle(ParticleTypes.CRIT, moduleBE.getBlockPos().getX() + 0.5, moduleBE.getBlockPos().getY() + 0.5, moduleBE.getBlockPos().getZ() + 0.5, 0, 0.1, 0);
             }
         }
     }
     ```

 2.  Call the API during `FMLCommonSetupEvent`.
     *`FMLCommonSetupEvent`에서 API를 호출합니다.*

     ```java
     @SubscribeEvent
     public static void onCommonSetup(FMLCommonSetupEvent event) {
         event.enqueueWork(() -> {
             ModuleTypeApi.addModuleType(
                 "MY_DRILL_BOOSTER", // The internal enum name. MUST BE UNIQUE.
                 0.15f,              // stressImpact
                 0.10f,              // speedBonus
                 0.08f,              // heatModifier
                 0,                  // itemCapacity
                 0,                  // fluidCapacity
                 0,                  // energyCapacity
                 new MyDrillBoosterBehavior(), // Your custom behavior instance
                 true                // isPerformanceModule
             );
         });
     }
     ```

 ### Adding a Custom `Quirk`
 ### 커스텀 `Quirk` 추가하기

 You can add a new Quirk that can be generated on Artificial Nodes.
 *인공 노드에서 생성될 수 있는 새로운 특성(Quirk)을 추가할 수 있습니다.*

 **API Call:**
 ```java
 QuirkApi.addQuirk(
     String name,
     String id,
     Quirk.Tier tier,
     Supplier<Item> catalyst,
     QuirkBehavior behavior
 );
 ```
 *   `name`: The internal enum name (e.g., "MY_AWESOME_QUIRK"). Must be unique.
     *   *내부 열거형 이름 (예: "MY_AWESOME_QUIRK"). 반드시 고유해야 합니다.*
 *   `id`: The string ID used for translation keys (e.g., "my_awesome_quirk").
     *   *번역 키에 사용될 문자열 ID (예: "my_awesome_quirk").*

 **Example:**
 *예시:*

 1.  First, create your own implementation of `QuirkBehavior`.
     *먼저, `QuirkBehavior`를 구현하는 자신만의 클래스를 만듭니다.*

     ```java
     // In your addon's package
     public class RicherHarvestQuirk implements QuirkBehavior {
         // This Quirk will guarantee one extra drop from mining.
         @Override
         public int onCalculateDrops(int originalCount, Quirk.QuirkContext context) {
             // Play a little effect
             if (context.level().getRandom().nextFloat() < 0.2f) {
                 context.playEffects(ParticleTypes.HAPPY_VILLAGER, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f, 5, 0.1);
             }
             return originalCount + 1;
         }
     }
     ```

 2.  Call the API during `FMLCommonSetupEvent`.
     *`FMLCommonSetupEvent`에서 API를 호출합니다.*

     ```java
     @SubscribeEvent
     public static void onCommonSetup(FMLCommonSetupEvent event) {
         event.enqueueWork(() -> {
             QuirkApi.addQuirk(
                 "RICHER_HARVEST",           // Enum name
                 "richer_harvest",           // ID for translation keys
                 Quirk.Tier.RARE,            // The tier of this Quirk
                 () -> MyAddonItems.FERTILIZER.get(), // The catalyst item
                 new RicherHarvestQuirk()    // Your custom behavior instance
             );
         });
     }
     ```
