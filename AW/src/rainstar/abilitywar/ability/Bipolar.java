package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "", rank = Rank.S, species = Species.HUMAN, explain = {
		"",
		" "
		},
		summarize = {
		""
		})
public class Bipolar extends AbilityBase {
	
	public Bipolar(Participant participant) {
		super(participant);
	}

}
