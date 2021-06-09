package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(
		name = "죽창", rank = Rank.S, species = Species.HUMAN, explain = {
		"§23초마다 0.5초의 기회§f가 주어지고, 해당 시간 내에 적을 근접 타격하면",
		"대상에게 4.5배의 피해를 입힙니다. 죽창은 대상당 한 번만 사용 가능합니다.",
		"죽창이 파괴된 대상에겐, 0.75배의 피해만을 입힙니다."
		})

public class BambooSpear extends Synergy {

	public BambooSpear(Participant participant) {
		super(participant);
	}
	
	
}
