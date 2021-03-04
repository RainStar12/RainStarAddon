package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(name = "플랫포머", rank = Rank.L, species = Species.OTHERS, explain = {
		"대시 + 이단 점프",
		" "
		})

public class Platformers extends Synergy {
	
	public Platformers(Participant participant) {
		super(participant);
	}

}
