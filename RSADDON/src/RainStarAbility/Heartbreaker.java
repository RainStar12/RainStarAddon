package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "", rank = Rank.L, species = Species.HUMAN, explain = {
		"치명적인 피해를 입으면 최대 체력의 $[PERCENTAGE]%만큼 깨진 체력을 획득합니다.",
		"깨진 체력당, 공격력이 $[DAMAGE_INCREASE]% 증가합니다.",
		"깨진 체력: 소지한 만큼의 최대 체력을 차지해, 차지한 만큼의 최대 체력은 채울 수 없습니다."
		},
		summarize = {
		""
		})

public class Heartbreaker extends AbilityBase {
	
	public Heartbreaker(Participant participant) {
		super(participant);
	}
	
	

}
