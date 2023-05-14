package rainstar.abilitywar.game.Simple.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "유도 화살", rank = Rank.S, species = Species.HUMAN, explain = {
		"§6활§f을 드는 동안 §5유도 게이지바§f가 차오릅니다.",
		"§6활§f 발사 시 게이지를 소모해 더 강한 §5유도력§f으로 화살이 §5유도§f됩니다."
		})

public class HomingArrow extends AbilityBase {
	
	public HomingArrow(Participant participant) {
		super(participant);
	}
	
	public AbilityTimer gauge = new AbilityTimer() {
	}

}
