package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(
		name = "È­¿° ÆøÇ³", rank = Rank.L, species = Species.HUMAN, explain = {
		"À½~"
		})

public class FlameStorm extends Synergy {

	public FlameStorm(Participant participant) {
		super(participant);
	}
	
}
