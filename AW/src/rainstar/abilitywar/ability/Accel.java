package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;

@AbilityManifest(name = "순간 가속", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §b액셀§f: §3가속§f을 획득하고, 5.0초간 §3가속§f × 10%만큼 §b이동 속도§f가",
		" §a증가§f합니다. 7번 사용 시 §3가속§f을 초기화하고 §c쿨타임§f을 가집니다. §c쿨타임 §7: §f60초",
		"§7근접 공격 §8- §eE=mc²§f: 내 현재 §b이동 속도§f에 비례하여 피해량이 강력해집니다."
		},
		summarize = {
		""
		})
public class Accel {

}
