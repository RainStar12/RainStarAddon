package rainstar.abilitywar.game.murder.system.config.variabletype;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractJob;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.event.PreAddGoldEvent;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.random.Random;
import rainstar.abilitywar.game.murder.system.Shield;

@ModuleBase(HeadcountBalance.class)
public class HeadcountBalance implements ListenerModule {

	private final MurderMystery game;
	private boolean enableMurder = false;
	private boolean enableInnocent = false;
	private Random random = new Random();

	public HeadcountBalance(AbstractGame game) {
		this.game = (MurderMystery) game;
	}
	
	@Override
	public void register() {
		final List<Participant> participants = new ArrayList<>(game.getParticipants());
		final int div = (int) Math.max(Math.ceil(participants.size() / 9.0), 1);
		if ((participants.size() / (double) div) >= 8) {
			enableMurder = true;
		} else if ((participants.size() / (double) div) <= 4) {
			enableInnocent = true;
		}
		
		if (enableMurder) {
			for (Participant participant : participants) {
				if (participant.getAbility() instanceof AbstractMurderer) Shield.addShield(participant, 1);
			}
			Bukkit.broadcastMessage("§4[§c!§4] §f인원 수 밸런스로 머더는 실드 1개와 25%로 금을 1개 더 얻습니다.");
		}
		
		if (enableInnocent) {
			new BukkitRunnable() {
				
				@Override
				public void run() {
					for (Participant participant : participants) {
						AbstractJob job = (AbstractJob) participant.getAbility();
						if (!job.hasBow()) {
							participant.getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
						job.addArrow();
					}
				}
				
			}.runTaskLater(AbilityWar.getPlugin(), 20L);
			Bukkit.broadcastMessage("§4[§c!§4] §f인원 수 밸런스로 모든 플레이어가 화살을 1개 얻고 시작합니다.");
		}
		
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@Override
	public void unregister() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler
	private void onAddGold(PreAddGoldEvent e) {
		if (enableMurder && e.getParticipant().getAbility() instanceof AbstractMurderer) {
			if (random.nextInt(4) == 0) e.setAmount(e.getAmount() + 1);
		}
	}
	

}
