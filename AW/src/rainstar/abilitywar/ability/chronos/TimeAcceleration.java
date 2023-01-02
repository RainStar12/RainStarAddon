package rainstar.abilitywar.ability.chronos;

import org.bukkit.World;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@ModuleBase(TimeAcceleration.class)
public class TimeAcceleration extends GameTimer implements Module {

	public final World world;
	public final int speed;
	
	public TimeAcceleration(AbstractGame abstractGame, World world, int speed) {
		abstractGame.super(TaskType.INFINITE, -1);
		setPeriod(TimeUnit.TICKS, 1);
		this.world = world;
		this.speed = speed;
	}
	
	@Override
	public void run(int count) {
		world.setTime(world.getTime() + speed);
	}

	@Override
	public void register() {
		start();
	}

	@Override
	public void unregister() {
	}

}
