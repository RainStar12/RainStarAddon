package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "머더: 부패 탐정", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 배신해 죽이세요!",
		"머더 직업이지만, 살인자의 검이 따로 지급되지 않습니다.",
		"활이 기본적으로 지급되며, 5초마다 최대 2개까지 화살이 보급됩니다.",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§3잠적§8]§f 배정 후 1분간 살인할 수 없습니다.",
		"§8[§b연루§8]§f 청탁받은 §4거물§f이 사망하면 정체가 발각됩니다."
})
public class CorruptionDetectiveMurderer extends AbstractMurderer {
	
	public CorruptionDetectiveMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			PlayerInventory inventory = getPlayer().getInventory();
			inventory.clear();
			getPlayer().getInventory().setHeldItemSlot(0);
			inventory.setItem(1, Items.DETECTIVE_BOW.getStack());
			if (getArrowCount() < 2) {
				charge.start();
			}
			((MurderMystery) getGame()).updateGold(getParticipant());
			NMS.sendTitle(getPlayer(), "§e역할§f: §9부패 탐정", "§4거물§f과 손잡고 모든 §a시민§f을 배신하세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			cantattack.start();
		} else if (update == Update.ABILITY_DESTROY) {
			Bukkit.broadcastMessage("§3§o부패 탐정§c§o을 잡아내셨습니다!");
		}
	}
	
	private final ActionbarChannel actionbarChannel = newActionbarChannel(), ac2 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	public AbilityTimer cantattack = new AbilityTimer(1200) {
		
		@Override
		public void run(int count) {
			ac2.update("§3공격 불가§7: §c" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer charge = new AbilityTimer(10) {
		private final ProgressBar progressBar = new ProgressBar(10, 10);

		@Override
		protected void onStart() {
			progressBar.setStep(0);
		}

		@Override
		protected void run(int count) {
			if (getArrowCount() >= 2) {
				stop(true);
				return;
			}
			progressBar.step();
			actionbarChannel.update("화살 보급: " + progressBar.toString());
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
			addArrow();
			if (getArrowCount() < 2) {
				start();
			}
		}

		@Override
		protected void onSilentEnd() {
			actionbarChannel.update(null);
		}
	}.setPeriod(TimeUnit.TICKS, 10);
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 8)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					}
				}
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onShoot(EntityShootBowEvent e) {
		charge.start();
	}
	
	@SubscribeEvent(priority = 4)
	public void onArrowKilled(ArrowKillEvent e) {
		if (e.getShooter().equals(getParticipant()) && !e.isCancelled()) {
			if (cantattack.isRunning()) e.setCancelled(true);
		}
	}

}
