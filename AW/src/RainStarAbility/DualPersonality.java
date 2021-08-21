package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "이중인격", rank = Rank.S, species = Species.GOD, explain = {
		""
		})

public class DualPersonality extends AbilityBase {
	
	public DualPersonality(Participant participant) {
		super(participant);
	}

}
