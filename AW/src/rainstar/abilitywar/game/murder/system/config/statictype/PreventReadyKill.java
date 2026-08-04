package rainstar.abilitywar.game.murder.system.config.statictype;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.game.list.murdermystery.event.PreAddGoldEvent;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;

@ModuleBase(PreventReadyKill.class)
public class PreventReadyKill implements ListenerModule {
	
	private final MurderMystery game;
	private final Random random = new Random();
	
	public PreventReadyKill(AbstractGame game) {
		this.game = (MurderMystery) game;
		for (Participant participant : game.getParticipants()) {
			NMS.setArrowsInBody(participant.getPlayer(), 0);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	protected void onArrowKill(ArrowKillEvent e) {
		if (!game.isGameStarted()) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	protected void onMurder(MurderEvent e) {
		if (!game.isGameStarted()) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	private void onAddGold(PreAddGoldEvent e) {
		if (e.getPlayer().hasPotionEffect(PotionEffectType.LUCK) && random.nextInt(10) == 0) {
			if (game.getGold(e.getParticipant()) < 64) {
				e.getPlayer().sendMessage("§aLucky!");
				e.setAmount(e.getAmount() * 2);
			}	
		}
		if (e.getPlayer().hasPotionEffect(PotionEffectType.UNLUCK) && random.nextInt(20) == 0) {
			e.getPlayer().sendMessage("§6Unlucky...");
			e.setCancelled(true);
		}
	}

}
