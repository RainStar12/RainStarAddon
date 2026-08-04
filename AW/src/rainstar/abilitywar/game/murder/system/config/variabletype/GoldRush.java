package rainstar.abilitywar.game.murder.system.config.variabletype;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;

@ModuleBase(GoldRush.class)
public class GoldRush extends GameTimer implements Module {

	private final AbstractGame game;
	
	public GoldRush(AbstractGame abstractGame, int duration) {
		abstractGame.super(TaskType.REVERSE, duration);
		setPeriod(TimeUnit.SECONDS, 1);
		this.game = abstractGame;
	}

	@Override
	public void register() {
		start();
	}

	@Override
	public void unregister() {

	}
	
	@Override
	public void run(int count) {
		if (count % 6 == 0) {
			for (Participant participant : game.getParticipants()) {
				Player player = participant.getPlayer();
				Location loc = LocationUtil.getRandomLocations(player.getLocation(), 15, 1).get(0);
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), player.getLocation().getY(), loc.getBlockX(), loc.getBlockZ()));
				player.getWorld().dropItemNaturally(loc, Items.GOLD.getStack());
			}
		}
	}
	
}
