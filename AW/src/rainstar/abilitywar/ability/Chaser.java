package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "추적자", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7나침반 타격 §8- §c추적 장치§f: 대상에게 추적 장치를 부착합니다.",
		" 나침반을 통해 대상의 방향을 파악할 수 있습니다.",
		""
		},
		summarize = {
		""
		})
public class Chaser extends AbilityBase {
	
	public Chaser(Participant participant) {
		super(participant);
	}

}
