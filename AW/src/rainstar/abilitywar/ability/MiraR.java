package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(
		name = "미라 R", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §",
		"§7상태이상 §8- §9속박의 연막§f: 이동 속도 및 점프가 느려집니다.",
		" 또한 엔티티에 대한 피해 이외의 모든 피해를 1.5배로 받게 됩니다.",
		},
		summarize = {
		"§7웅크린 채로 활을 발사해§f 적중 위치에 §3도약 표식§f을 만듭니다. $[ARROW_COOL]",
		"§3표식§f 주변에는 이동 속도가 느려지는 §9속박의 연막§f 효과를 받는 필드가 생성됩니다.",
		"§7철괴 좌클릭 시§f 도약 표식이 있는 곳으로 주변 플레이어와 함께 §5순간이동§f해",
		"§3도약 표식§f이 있던 지점에 폭발을 일으키고 같이 §5순간이동§f한 대상들을 기절시킵니다.",
		" $[COOLDOWN]"
		})
public class MiraR extends AbilityBase {
	
	public MiraR(Participant participant) {
		super(participant);
	}

}
