package RainStarAbility;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;

@AbilityManifest(
		name = "쿠로(개안)", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7패시브 §8- §8어둠 군주§f: 자신이 있는 위치가 어두울수록 스킬 피해량이 증가합니다.",
		"§7근접 타격 후 F §8- §5시간 절단§f: 바라보는 방향으로 시간 도약해 ",
		"§7철괴 좌클릭 §8- §8심연의 초대§f: 주변 $[RANGE]칸 내의 모든 플레이어가 $[DURATION]초간",
		" 나를 바라본 채 천천히 땅 속으로 침식당합니다. $[ACTIVE_COOLDOWN]",
		"§7패시브 §8- §c마안 폭주§f: 모든 회복 효과를 받을 수 없는 대신 회복 효과의 10%만큼",
		" 영구적인 추가 피해가 최대 10까지 상승합니다."
		})

public class KuroEye {

}
