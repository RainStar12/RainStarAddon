package rainstar.abilitywar.game.murder.system.config.variabletype;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import rainstar.abilitywar.game.murder.system.config.statictype.CheckMurdererSword;

@ModuleBase(NoKill.class)
public class NoKill implements ListenerModule {

	private CheckMurdererSword module;
	private final int time;
	
	public NoKill(AbstractGame game, int time) {
		module = game.getModule(CheckMurdererSword.class);
		this.time = time;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	protected void onArrowKill(ArrowKillEvent e) {
		if (module.getCount() >= module.getTime() - time) {
			e.setCancelled(true);
			e.getShooter().getPlayer().sendMessage("§4[§c!§4] §c아직 공격 가능 시간이 아닙니다.");
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	protected void onMurder(MurderEvent e) {
		if (module.getCount() >= module.getTime() - time) {
			e.setCancelled(true);
			e.getMurderer().getPlayer().sendMessage("§4[§c!§4] §c아직 공격 가능 시간이 아닙니다.");
		}
	}
	
	public boolean isKillTime() {
		return module.getCount() < (module.getTime() - time);
	}

}
