package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(
		name = "가학증", rank = Rank.L, species = Species.HUMAN, explain = {
		"적에게 피해를 입힐 때마다 공격력이 $[INCREASE]%씩 상승합니다.",
		"피해를 받으면 "
		})
public class Sadism extends Synergy {
	
	public Sadism(Participant participant) {
		super(participant);
	}
	
	

}
