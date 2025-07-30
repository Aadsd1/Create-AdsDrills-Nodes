All commands require operator permission level 2 or higher.  
모든 명령어는 2레벨 이상의 OP 권한을 요구합니다.  

For a brief in-game explanation, use the help subcommand (e.g., /adsdrill create_node help).  
게임 내에서 간단한 설명을 보려면 help 하위 명령어를 사용하세요 (예: /adsdrill create_node help).  

/adsdrill create_node  
Creates a custom Ore Node at the block the player is looking at.  
플레이어가 바라보는 블록 위치에 커스텀 광맥 노드를 생성합니다.

simple  
Creates a node with random stats. Only composition is required.  
무작위 능력치를 가진 노드를 생성합니다. 광물 구성만 필수입니다.  
Syntax: /adsdrill create_node simple <composition> [max_yield]  
사용법: /adsdrill create_node simple <광물구성> [최대매장량]  
Example: /adsdrill create_node simple "minecraft:iron_ore:1 minecraft:coal_ore:1"  
예시: /adsdrill create_node simple "minecraft:iron_ore:1 minecraft:coal_ore:1"  

full  
Creates a natural-style node with fully specified stats.  
모든 능력치를 직접 지정하여 자연 광맥 노드 스타일로 생성합니다.  
Syntax: /adsdrill create_node full <comp> <yield> <hardness> <richness> <regen> [fluid_id] [fluid_cap]  
사용법: /adsdrill create_node full <구성> <매장량> <경도> <풍부함> <재생력> [유체ID] [유체용량]  
Example: /adsdrill create_node full "minecraft:diamond_ore:1" 50000 2.5 1.5 0.005 minecraft:lava 10000  
예시: /adsdrill create_node full "minecraft:diamond_ore:1" 50000 2.5 1.5 0.005 minecraft:lava 10000  

artificial  
Creates an artificial node, allowing you to specify Quirks and fluids.  
인공 광맥 노드를 생성하며, 특성(Quirk)과 유체를 지정할 수 있습니다.  
Syntax: /adsdrill create_node artificial <comp> <yield> <hardness> <richness> <regen> [fluid_id] [fluid_cap] [quirks]  
사용법: /adsdrill create_node artificial <구성> <매장량> <경도> <풍부함> <재생력> [유체ID] [유체용량] [특성들]  
Example: /adsdrill create_node artificial "minecraft:gold_ore:1" 30000 1.2 1.2 0.002 STEADY_HANDS AQUIFER  
예시: /adsdrill create_node artificial "minecraft:gold_ore:1" 30000 1.2 1.2 0.002 STEADY_HANDS AQUIFER  

/adsdrill findOreMods  
A utility command to scan for ore generation features from other mods.  
다른 모드들의 광물 생성 정보를 스캔하는 유틸리티 명령어입니다.  

(no subcommand)
Lists all mod IDs that have recognized ore generation.  
인식된 광물 생성을 가진 모든 모드의 ID를 나열합니다.  
Syntax: /adsdrill findOreMods  
사용법: /adsdrill findOreMods  

details  
Shows how AdsDrill maps specific ore blocks to items for a given mod ID.  
특정 모드 ID에 대해, AdsDrill이 광석 블록을 어떤 아이템에 매핑하는지 보여줍니다.  
Syntax: /adsdrill findOreMods details <mod_id>  
사용법: /adsdrill findOreMods details <모드ID>  
Example: /adsdrill findOreMods details create  
예시: /adsdrill findOreMods details create  
