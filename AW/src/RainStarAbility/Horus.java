package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "호루스", rank = Rank.S, species = Species.HUMAN, explain = {
		"$[PERIOD]초마다 지난 $[PERIOD]초간 남들에게 피해를 가장 많이 준 적에게",
		"§5고대 저주§f를 부여하고, §5저주 단계§f만큼 피해를 입힙니다.",
		"그 적이 $[PERIOD]초간 입힌 피해의 $[PERCENTAGE]%만큼 자신의 §c공격력§f이 증가합니다.",
		"§0[§5고대 저주§0]§f 중복 부여 시마다 단계가 상승합니다. 단계에 비례하여",
		" 공격력이 증가하지만 받는 피해는 더 증가합니다. 이 비율은 2:3입니다."
		})

public class Horus extends AbilityBase {
	
	public Horus(Participant participant) {
		super(participant);
	}

}
