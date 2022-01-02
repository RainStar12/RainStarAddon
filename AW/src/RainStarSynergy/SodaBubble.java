package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;


@AbilityManifest(
		name = "소다<버블>", rank = Rank.L, species = Species.OTHERS, explain = {
		"§3물 속성§f의 꼬마 정령, 소다.",
		"§7패시브 §8- §3스플래쉬§f: 불 속성의 피해, 모든 상태이상, 모든 포션 효과에 면역 효과를",
		" 가집니다. 또한 신발을 신고 물 속에 들어갈 때 물갈퀴 인챈트를 자동 획득합니다.",
		"§7철괴 좌클릭 §8- §3버블팝§f: $[DURATION_CONFIG]초간 물이 되어, 타게팅 불능 및 무적 상태가 됩니다.",
		" 물이 된 동안 지면에 맞닿아서만 이동할 수 있으며, 지속 시간이 끝날 때 물 상태가",
		" 해제되고 주변 $[RANGE_CONFIG]칸 내 적에게 $[EFFECT_DURATION]초간 부식 상태이상을 겁니다. $[COOLDOWN_CONFIG]",
		"§7상태이상 §8- §7부식§f: 철 광물을 사용하는 모든 아이템을 사용할 수 없습니다.",
		" 또한 갑옷의 방어력이 착용 광물에 비례해 희귀성이 낮을수록 더 많이 감소합니다."
		})

public class SodaBubble extends Synergy {

}
