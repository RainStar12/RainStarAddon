package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "양치기 소년", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §8- §3양몰이§f: "
		},
		summarize = {
		""
		})
public class CryWolf extends AbilityBase {
	
	public CryWolf(Participant participant) {
		super(participant);
	}

}
