package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "공기", rank = Rank.A, species = Species.HUMAN, explain = {
		"능력도 없는 당신은 존재감이 너무 없는 나머지,", 
		"다른 능력의 지정 대상이 되지 않습니다."
		})
public class Air extends AbilityBase {

	public Air(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) getParticipant().attributes().TARGETABLE.setValue(false);	
		if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) getParticipant().attributes().TARGETABLE.setValue(true);
	}
	
}