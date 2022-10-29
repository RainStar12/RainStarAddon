package RainStarAbility;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;

@AbilityManifest(name = "아카식 레코드", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §b초우주 연산§f: 다른 플레이어가 §a액티브 능력§f을 사용할 때마다",
		" 대상 플레이어가 사용한 능력 및 대상의 위치를 알 수 있습니다.",
		" 이 패시브는 철괴 좌클릭으로 켜고 끌 수 있습니다.",
		"§7철괴 우클릭 §8- §a기록의 재현§f: 가장 최근에 사용된 §a액티브 능력§f 2개를 §6책§f으로 만듭니다.",
		" 이미 §6책§f이 있다면 덮어씌웁니다. §6책§f은 2권까지 소지할 수 있으며,",
		" §6책§f을 분실할 경우 효과가 사라집니다. §6책§f을 우클릭하면 §6책§f을 비우고",
		" 담겨진 §a액티브 능력§f의 모든 스킬을 사용합니다. 스킬은 §6지속시간§f까지 유지되며,",
		" 유지되는 동안은 사용한 능력의 §b패시브§f도 같이 획득합니다. $[COOLDOWN]"
		})

public class AkashicRecords {

}
