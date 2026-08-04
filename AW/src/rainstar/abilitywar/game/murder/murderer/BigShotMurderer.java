package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.event.PreAddGoldEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import rainstar.abilitywar.effect.Mute;

@AbilityManifest(name = "머더: 거물", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§3매수§8]§f 직업 배정 시 무작위 §3탐정§f을 §5머더 직업§f인 §9부패 탐정§f으로 만듭니다.",
		"§8[§3잠적§8]§f 배정 후 1분간 살인할 수 없습니다.",
		"§8[§b연루§8]§f 자신이 사망하면 §9부패 탐정§f의 정체가 발각됩니다.",
		"§8[§e재력§8]§f 50% 확률로 금을 획득할 때 금을 2배로 얻습니다.",
		"§8[§c입막음§8]§f 공격당할 시 공격자에게 금 20개를 쥐어주고 공격을 막아냅니다.",
		"대상은 30초간 침묵 상태가 되고, 자신은 5초간 공격 불가가 됩니다."
})
public class BigShotMurderer extends AbstractMurderer {
	
	public BigShotMurderer(Participant participant) {
		super(participant);
	}
	
	private boolean changed = false;
	private Participant mydetective = null;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5거물", "§3탐정§f을 회유해 자신의 편으로 만드세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			if (!changed) {
				List<Participant> detectives = new ArrayList<>();
				for (Participant participant : getGame().getParticipants()) {
					if (participant.hasAbility()) {
						if (participant.getAbility().getClass().getSimpleName().contains("Detective") && !participant.getAbility().getClass().equals(CorruptionDetectiveMurderer.class)) detectives.add(participant);	
					}
				}
				
				if (detectives.size() > 0) {
					Participant detective = random.pick(detectives);
					try {
						detective.setAbility(CorruptionDetectiveMurderer.class);
						mydetective = detective;
						getPlayer().sendMessage("§4[§c!§4] §f당신의 §9부패 탐정§f은 §c" + detective.getPlayer().getName() + "§f님입니다.");
						detective.getPlayer().sendMessage("§4[§c!§4] §f당신의 §4거물§f은 §c" + getPlayer().getName() + "§f님입니다.");
						changed = true;
					} catch (UnsupportedOperationException | ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}
			}
			passive.start();
			PASSIVE.start();
			cantattack.start();
			cantattack.setCount(1200);
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
			if (mydetective != null && !mydetective.getPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
				Bukkit.broadcastMessage("§8[§b연루§8] §4거물 §c" + getPlayer().getName() + "§f님이 청탁한 §9부패 탐정§f은 §3" + mydetective.getPlayer().getName() + "§님입니다.");
			}
		}
	}
	
	@SubscribeEvent
	private void onAddGold(PreAddGoldEvent e) {
		if (random.nextBoolean()) {
			if (e.getPlayer().equals(getPlayer())) {
				e.setAmount(e.getAmount() * 2);
			}	
		}
	}
	
	@SubscribeEvent(priority = 4)
	public void onMurder(MurderEvent e) {
		if (e.getMurderer().getParticipant().equals(getParticipant()) && !e.isCancelled()) {
			if (cantattack.isRunning()) e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = 4)
	public void onArrowKilled(ArrowKillEvent e) {
		if (e.getShooter().equals(getParticipant()) && !e.isCancelled()) {
			if (cantattack.isRunning()) e.setCancelled(true);
		}
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			final MurderMystery murderMystery = (MurderMystery) getGame();
			if (murderMystery.consumeGold(getParticipant(), 20)) {
				e.setCancelled(true);
				murderMystery.addGold(e.getShooter(), 20);
				Mute.apply(e.getShooter(), TimeUnit.SECONDS, 30);
				if (cantattack.isRunning()) cantattack.setCount(100);
				else cantattack.start();
			}	
		}
	}
	
	public AbilityTimer cantattack = new AbilityTimer(100) {
		
		@Override
		public void run(int count) {
			ac.update("§3공격 불가§7: §c" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!changed) {
				List<Participant> detectives = new ArrayList<>();
				for (Participant participant : getGame().getParticipants()) {
					if (participant.hasAbility()) {
						if (participant.getAbility().getClass().getSimpleName().contains("Detective") && !participant.getAbility().getClass().equals(CorruptionDetectiveMurderer.class)) detectives.add(participant);	
					}
				}
				
				if (detectives.size() > 0) {
					Participant detective = random.pick(detectives);
					try {
						detective.setAbility(CorruptionDetectiveMurderer.class);
						mydetective = detective;
						getPlayer().sendMessage("§4[§c!§4] §f당신의 §9부패 탐정§f은 §c" + detective.getPlayer().getName() + "§f님입니다.");
						detective.getPlayer().sendMessage("§4[§c!§4] §f당신의 §4거물§f은 §c" + getPlayer().getName() + "§f님입니다.");
						changed = true;
					} catch (UnsupportedOperationException | ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
	}.setPeriod(TimeUnit.SECONDS, 10).register();
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 8)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					} else {
						if (!hasBow()) {
							getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
					}
				}
			}
		}
	}

}
