package rainstar.abilitywar.ability.killerbunny;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.ArmorStand;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;

@ModuleBase(HealthBar.class)
public class HealthBar extends GameTimer implements Module {

	public final AbstractGame abstractGame;
	public final int speed;
	public final Map<Participant, ArmorStand> armorstands = new HashMap<>();
	
	public HealthBar(AbstractGame abstractGame, int speed) {
		abstractGame.super(TaskType.INFINITE, -1);
		setPeriod(TimeUnit.TICKS, 1);
		this.abstractGame = abstractGame;
		this.speed = speed;
	}
	
	@Override
	public void run(int count) {
		for (Participant participant : abstractGame.getParticipants()) {
			if (armorstands.containsKey(participant)) {
				armorstands.get(participant).teleport(participant.getPlayer().getEyeLocation().add(0, 0.6, 0));
				armorstands.get(participant).setCustomName("");
			} else {
				
			}
		}
	}

	@Override
	public void register() {
		start();
	}

	@Override
	public void unregister() {
	}

}