package RainStarSynergy.chance;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@ModuleBase(ChanceBossbarModule.class)
public class ChanceBossbarModule extends GameTimer implements Module {

	public ChanceBossbarModule(AbstractGame abstractGame, int count) {
		abstractGame.super(TaskType.REVERSE, count);
		setPeriod(TimeUnit.TICKS, 1);
	}
	
	@Override
	public void register() {
		start();
	}

	@Override
	public void unregister() {
	}
	
}
