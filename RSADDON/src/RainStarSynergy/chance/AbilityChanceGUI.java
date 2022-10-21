package RainStarSynergy.chance;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@ModuleBase(AbilityChanceGUI.class)
public class AbilityChanceGUI extends GameTimer implements Module {
	
	public AbilityChanceGUI(AbstractGame abstractGame) {
		abstractGame.super(TaskType.REVERSE, 600);
		setPeriod(TimeUnit.TICKS, 1);
	}
	
	@Override
	public void run(int count) {
	}

	@Override
	public void register() {
		for (Participant participant : getGame().getParticipants()) {
			if (participant.hasAbility()) {
				Mix mix = (Mix) participant.getAbility();
				if (mix.hasSynergy()) {
					if (mix.getSynergy().getClass().equals(Chance.class)) {
						new AbilitySelectChance(getGame(), participant.getPlayer()).start();
					} else {
						new RainStarSynergy.chance.AbilitySelect(getGame(), participant.getPlayer(), mix.getSynergy().getRegistration()).start();
					}
				}
			}
		}
		start();
	}

	@Override
	public void unregister() {
	}
	
	
	
}