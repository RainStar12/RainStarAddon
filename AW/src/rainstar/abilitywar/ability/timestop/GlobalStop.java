package rainstar.abilitywar.ability.timestop;

import org.bukkit.World;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;


@ModuleBase(GlobalStop.class)
public class GlobalStop extends GameTimer implements Module {


	public GlobalStop(AbstractGame abstractGame) {
		abstractGame.super(TaskType.INFINITE, -1);
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
