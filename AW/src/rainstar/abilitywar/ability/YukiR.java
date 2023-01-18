package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "유키", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b만년설§f: 상태이상을 받을 때 지속시간 절반의 §b§n빙결§f로 교체합니다.",
		" ",
		"§7",
		""
		},
		summarize = {
		""
		})
public class YukiR extends AbilityBase {
	
	public YukiR(Participant participant) {
		super(participant);
	}

}
