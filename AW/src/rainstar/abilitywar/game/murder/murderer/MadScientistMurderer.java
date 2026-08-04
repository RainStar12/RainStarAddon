package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

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
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.effect.Drug;

@AbilityManifest(name = "머더: 매드 사이언티스트", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§b신체 개조§8]§f 모든 부정 효과를 받지 않습니다.",
		"§8[§3인체 실험§8]§f 금 좌클릭으로 가진 금을 전부 소모해 소모한 금 × 0.25초간",
		"두 명의 감각을 §5교란§f시킵니다. 교란된 §a시민 A§f는 §b시민 B§f의 슬롯바를",
		"컨트롤하며, §b시민 B§f는 §a시민 A§f의 시점을 컨트롤합니다.",
		"§8[§c재구성§8]§f 감각을 교란중인 대상을 죽이면 §5스택§f을 획득합니다.",
		"공격받으면 §5스택§f × 10초간 부활합니다. 부활 중에는 §b신속 2§f를 받고,",
		"공격을 받아도 사망하지 않고 4초의 시간을 소모합니다. §8(§71회용§8)"
})
public class MadScientistMurderer extends AbstractMurderer {

	public MadScientistMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5매드 사이언티스트", "§f모든 §a시민§f과 §5탐정§f을 감각을 혼란시키세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.start();
			passive.start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private Participant targetA;
	private Participant targetB;
	private ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private int stack = 0;
	private boolean revived = false;
	private BossBar bossBar = null;
	private int maxtime = 0;
	
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
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (!skill.isRunning()) {
					MurderMystery murderMystery = (MurderMystery) getGame();
					if (targetA != null && targetB != null) {
						int gold = murderMystery.getGold(getParticipant());
						if (murderMystery.consumeGold(getParticipant(), gold)) {
							getPlayer().sendMessage("§4[§c!§4] §a" + targetA.getPlayer().getName() + "§f님, §b" + targetB.getPlayer().getName() + "§f님의 감각을 §e" + df.format(gold * 0.25) + "§f초간 뒤틉니다...");	
							skill.start();
							skill.setCount(gold * 5);
						}
					} else getPlayer().sendMessage("§4[§c!§4] §c지정할 무작위 대상이 2명보다 적습니다.");	
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 지정 대상이 있습니다.");
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (!e.getEffectType().equals(Drug.registration)) {
			e.setCancelled(true);	
		}
	}
	
	@SubscribeEvent
	private void onSlotChange(PlayerItemHeldEvent e) {
		if (targetA != null && targetB != null && skill.isRunning()) {
			if (e.getPlayer().equals(targetA.getPlayer()) && e.getNewSlot() != targetB.getPlayer().getInventory().getHeldItemSlot()) {
				targetB.getPlayer().getInventory().setHeldItemSlot(e.getNewSlot());
			}
			if (e.getPlayer().equals(targetB.getPlayer()) && e.getNewSlot() != targetA.getPlayer().getInventory().getHeldItemSlot()) {
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent(priority = 9999)
	private void onMurder(MurderEvent e) {
		if (e.getMurderer().getParticipant().equals(getParticipant()) && !e.isCancelled() && !revived) {
			if (passive.isRunning() && (e.getTarget().equals(targetA) || e.getTarget().equals(targetB))) {
				stack++;
				ac2.update("§c실험체 스택§7: §5" + stack);
			}
		}
	}
	
	@SubscribeEvent(priority = 9999)
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getShooter().equals(getParticipant()) && !e.isCancelled() && !revived) {
			if (passive.isRunning() && (e.getTarget().equals(targetA) || e.getTarget().equals(targetB))) {
				stack++;
				ac2.update("§c실험체 스택§7: §5" + stack);
			}
		}
	}
	
	@SubscribeEvent(priority = 0)
	private void onArrowKilled(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant())) {
			e.setCancelled(true);
			if (restructure.isRunning()) {
				restructure.setCount(Math.max(1, restructure.getCount() - 80));
				ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation(), 0.3, 1, 0.3, 25, 1, MaterialX.REDSTONE_BLOCK);
			} else {
				ac2.update(null);
				maxtime = Math.max(1, stack * 200);
				restructure.start();
				restructure.setCount(maxtime);
			}
		}
	}
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
	
	private final AbilityTimer restructure = new AbilityTimer(TaskType.REVERSE, 1000) {
		
		@Override
		protected void onStart() {
			revived = true;
			bossBar = Bukkit.createBossBar("§c재구성§7: §5" + df.format((stack * 200) / 20.0) + "§f초", BarColor.PURPLE, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    		SoundLib.ENTITY_ZOMBIE_HORSE_DEATH.playSound(getPlayer().getLocation(), 1, 0.5f);
    		SoundLib.ENTITY_ZOMBIE_VILLAGER_DEATH.playSound(getPlayer().getLocation(), 1, 0.5f);
    		SoundLib.ENTITY_HUSK_DEATH.playSound(getPlayer().getLocation(), 1, 0.5f);
    		ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation(), 0.3, 1, 0.3, 75, 1, MaterialX.SLIME_BLOCK);
		}

		@Override
		protected void onEnd() {
			SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer().getLocation(), 1, 0.5f);
			getPlayer().setHealth(0);
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
		}

		@Override
		protected void run(int count) {
			if (count % 20 == 0) ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation(), 0.3, 1, 0.3, 10, 1, MaterialX.SLIME_BLOCK);
			bossBar.setTitle("§c재구성§7: §5" + df.format(count / 20.0) + "§f초");
			bossBar.setProgress(Math.min(1, Math.max(0, count / (double) maxtime)));
			PotionEffects.SPEED.addPotionEffect(getPlayer(), 100, 1, false, false, true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer passive = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
    		getPlayer().removePotionEffect(PotionEffectType.GLOWING);
    		getPlayer().removePotionEffect(PotionEffectType.SLOW);
    		getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);
    		getPlayer().removePotionEffect(PotionEffectType.HUNGER);
    		getPlayer().removePotionEffect(PotionEffectType.CONFUSION);
    		
    		if (!skill.isRunning()) {
    			List<Participant> participants = new ArrayList<>();
				for (Participant participant : getGame().getParticipants()) {
					if (!(participant.getAbility() instanceof AbstractMurderer) && !((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && participant.attributes().TARGETABLE.getValue()) {
						participants.add(participant);
					}
				}
				if (participants.size() > 1) {
	    			if (targetA != null && targetB != null && participants.contains(targetA) && participants.contains(targetB)) {}
	    			else {
	    				targetA = random.pick(participants);
						participants.remove(targetA);
						targetB = random.pick(participants);
	    			}
	    			ac.update("§a시점 " + targetA.getPlayer().getName() + " §7/ §b슬롯 " + targetB.getPlayer().getName());
				} else {
					targetA = null;
					targetB = null;
	    			ac.update("§c실험체 부족");
				}
    		}
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer skill = new AbilityTimer(100) {
		
		@Override
		public void run(int count) {
			ac.update("§a" + targetA.getPlayer().getName() + "§7⇆⇆⇆§b" + targetB.getPlayer().getName() + "§7: §e" + df.format(count / 20.0) + "초");
			Vector direction = targetB.getPlayer().getLocation().getDirection();
			float yaw = LocationUtil.getYaw(direction), pitch = LocationUtil.getPitch(direction);
			for (Player player : Bukkit.getOnlinePlayers()) {
				NMS.rotateHead(player, targetA.getPlayer(), yaw, pitch);
			}
			targetB.getPlayer().getInventory().setHeldItemSlot(targetA.getPlayer().getInventory().getHeldItemSlot());
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			targetA = null;
			targetB = null;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
}
