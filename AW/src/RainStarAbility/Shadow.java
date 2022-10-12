package RainStarAbility;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;

@AbilityManifest(name = "섀도우", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §9가장 오래된 감정§f: §b공포 상태§f의 적에게 주는 피해가 20% 증가합니다.",
		"§7검 우클릭 §8- §5흑마술§f: 5초간 내게 피해를 주는 대상은 3초간 §b공포 상태§f가 됩니다.",
		" §5흑마술§f이 끝날 때, §b공포§f를 준 대상 수 × §d4HP§f를 §d회복§f합니다. §c쿨타임 §7:§f 70초",
		"§7철괴 우클릭 §8- §c그림자 베기§f: 10초간 §3그림자 상태§f가 되어, 적을 근접 공격하면",
		" 대상의 방향으로 짧게 돌진 이후 2칸 내의 적들을 1초간 공포에 빠뜨립니다.",
		" 이때 적을 처치하면 §3그림자 상태§f를 5초 증가시키고, §3그림자 상태§f가 끝날 때",
		" 천천히 줄어드는 §c공격력 30% × 처치 수§f를 획득합니다. §c쿨타임 §7:§f 110초",
		"§b[§7아이디어 제공자§b] §dspace_kdd"
		},
		summarize = {
		""
		})
public class Shadow {

}
