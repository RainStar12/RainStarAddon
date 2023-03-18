package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "새벽", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = {
		"§7패시브 §8- §e밝아오는 여명§f:",
		"§7패시브 §8- §3새벽의 가호§f:",
		"§7기본 공격 §8- §b하늘 가르기§f:",
		"§7검 F키 §8- §9하늘 찢기§f:",
		"§7철괴 우클릭 §8- §d황혼의 시간§f:"
		},
		summarize = {
		"???"
		})


public class Daybreak extends AbilityBase {
	
	public Daybreak(Participant participant) {
		super(participant);
	}

}
