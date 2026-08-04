package rainstar.abilitywar.game.murder.innocent;

import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "시민: 광대", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 7개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§5룰 브레이커§8]§f 나를 쏜 사람이 죽지 않고, 내가 남을 쏴도 죽지 않습니다.",
		"누구를 죽이든 자신의 직업이 공개됩니다. §3탐정§f을 죽이려 들면 광대만 사망합니다.",
		"§8[§4대혼돈§8]§f 금 좌클릭으로 금 14개를 소모해 다음 화살 적중을 §4처형 판정§f으로 만듭니다.",
		"이 판정으로는 §3탐정§f도 죽일 수 있습니다."
})
public class Clown extends AbstractInnocent {

	public Clown(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a광대", "§f모든 사람을 혼란시켜 게임을 뒤집어보세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		}
	}
	
	private boolean charged = false;
	private ActionbarChannel ac = newActionbarChannel();
	
	@SubscribeEvent(priority = 3, eventPriority = EventPriority.LOW)
	private void onArrowKillExecution(ArrowKillEvent e) {
		if (charged && e.getShooter().equals(getParticipant())) {
			e.setCancelled(true);
			if (e.getTarget() != null) e.getTarget().getPlayer().setHealth(0);
			ac.update(null);
			charged = false;
			new BukkitRunnable() {
				@Override
				public void run() {
					Bukkit.broadcastMessage("§5[§3광대§5] §5광대§f §3" + getPlayer().getName() + "§f님이 §e" + e.getTarget().getPlayer().getName() + "§f님을 §4처형§f하였습니다.");	
				}
			}.runTaskLater(AbilityWar.getPlugin(), 4L);
		}
	}
	
	@SubscribeEvent(priority = 11, eventPriority = EventPriority.HIGHEST)
	private void onArrowKill(ArrowKillEvent e) {
		if (!e.isCancelled()) {
			if (e.getShooter().equals(getParticipant()) || e.getTarget().equals(getParticipant())) {				
				if (e.getShooter().equals(getParticipant())) {
					if (!(e.getTarget().getAbility() instanceof AbstractMurderer)) {
						if (e.getTarget().getAbility().getClass().getSimpleName().contains("Detective")) getPlayer().setHealth(0);
						else {
							new BukkitRunnable() {
								@Override
								public void run() {
									Bukkit.broadcastMessage("§5[§3광대§5] §5광대§f §3" + getPlayer().getName() + "§f님이 §e" + e.getTarget().getPlayer().getName() + "§f님을 §c사살§f하였습니다.");	
								}
							}.runTaskLater(AbilityWar.getPlugin(), 4L);
						}
					} else {
						new BukkitRunnable() {
							@Override
							public void run() {
								Bukkit.broadcastMessage("§5[§3광대§5] §5광대§f §3" + getPlayer().getName() + "§f님이 §e" + e.getTarget().getPlayer().getName() + "§f님을 §c사살§f하였습니다.");	
							}
						}.runTaskLater(AbilityWar.getPlugin(), 4L);
					}
				}
				e.setCancelled(true);
				if (e.getTarget() != null && !(e.getTarget().getAbility().getClass().getSimpleName().contains("Detective"))) e.getTarget().getPlayer().setHealth(0);
			}		
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 7)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					} else {
						if (!hasBow()) {
							getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
					}
				}
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				MurderMystery murderMystery = (MurderMystery) getGame();
				if (!charged) {
					if (murderMystery.consumeGold(getParticipant(), 14)) {
						charged = true;
						ac.update("§4처형 화살");
					}	
				}
			}
		}
	}
	
}
