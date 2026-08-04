package rainstar.abilitywar.synergy.chance;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
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
				if (participant.getAbility() instanceof Mix) {
					Mix mix = (Mix) participant.getAbility();
					if (mix.hasSynergy()) {
						if (mix.getSynergy().getClass().equals(Chance.class)) {
							new AbilitySelectChance(getGame(), participant.getPlayer(), true).start();
						} else {
							new rainstar.abilitywar.synergy.chance.AbilitySelect(getGame(), participant.getPlayer(), mix.getSynergy().getRegistration(), true).start();
						}
					} else if (SynergyFactory.isSynergy(mix.getFirst().getRegistration())) {
						if (mix.getFirst().getClass().equals(Chance.class)) {
							new AbilitySelectChance(getGame(), participant.getPlayer(), false).start();
						} else {
							new rainstar.abilitywar.synergy.chance.AbilitySelect(getGame(), participant.getPlayer(), mix.getFirst().getRegistration(), false).start();
						}
					} else if (SynergyFactory.isSynergy(mix.getSecond().getRegistration())) {
						if (mix.getSecond().getClass().equals(Chance.class)) {
							new AbilitySelectChance(getGame(), participant.getPlayer(), false).start();
						} else {
							new rainstar.abilitywar.synergy.chance.AbilitySelect(getGame(), participant.getPlayer(), mix.getSecond().getRegistration(), false).start();
						}
					}
				} else if (SynergyFactory.isSynergy(participant.getAbility().getRegistration())) {
					if (participant.getAbility().getClass().equals(Chance.class)) {
						new AbilitySelectChance(getGame(), participant.getPlayer(), false).start();
					} else {
						new rainstar.abilitywar.synergy.chance.AbilitySelect(getGame(), participant.getPlayer(), participant.getAbility().getRegistration(), false).start();
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