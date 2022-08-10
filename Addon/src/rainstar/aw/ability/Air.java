package rainstar.aw.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "공기", rank = Rank.A, species = Species.HUMAN, explain = {
		"능력도 없는 당신은 존재감이 너무 없는 나머지,", 
		"다른 능력의 지정 대상이 되지 않습니다."
		},
		summarize = {
		"능력의 지정 대상이 되지 않습니다. §8(§7타게팅 불능§8)"
		})
public class Air extends AbilityBase {

	public Air(Participant participant) {
		super(participant);
	}

	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int count) {
			getParticipant().attributes().TARGETABLE.setValue(false);
		}
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		@Override
		protected void onSilentEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true);
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) passive.start();
	}
	
}