package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BlockIterator;
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
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 뱀파이어", rank = Rank.SPECIAL, species = Species.UNDEAD, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c갈망§8]§f 피를 일정 시간 흡혈하지 못하면 사망합니다.",
		"기본적으로 120초가 주어지며, 사람을 죽이면 §c피§f를 30초만큼 흡혈합니다.",
		"흡혈 후 4초간 무적이 됩니다.",
		"§8[§4의태§8]§f 금 좌클릭으로 §c피§f 3초를 소모해 바라보는 방향으로 짧게 돌진합니다.",
		"비용을 금 2개로 대체 가능하며, 이때 금이 0개 이하로 떨어지지 않습니다.",
		"돌진 중에는 관전 상태가 되어 지형을 통과하며, 무적시간이 흐르지 않습니다.",
		"사용 후 0.05초간은 검으로 살인 시도 시 대신 1.5초간 §e§n기절§f합니다.",
		"공격 불가 시간은 살인 전까지 매번 0.3초씩 증가하며 누적됩니다."
})
public class VampireMurderer extends AbstractMurderer {
	
	public VampireMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5뱀파이어", "§c피가 닳기 전에§f 모든 §a시민§f을 흡혈하세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			passive.start();
			PASSIVE.start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private static final Vector ZERO_VECTOR = new Vector();
	private Location targetLocation;
	private BossBar bossBar = null;
	private int lefttime = 2400;
	private int maxtime = 2400;
	private ActionbarChannel ac = newActionbarChannel(), ac1 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private int stack = 0;
    private final List<Integer> pumplistA = new ArrayList<Integer>(Arrays.asList(1300, 1200, 1100, 1000, 950, 900, 850, 810, 770, 730, 700, 670, 640, 610, 
    		580, 550, 530, 510, 490, 470, 450, 430, 410, 395, 380, 365, 350, 335, 320, 305, 290, 275, 260, 248, 236, 224, 212, 200, 188, 176, 164, 152, 140,
    		130, 120, 110, 100, 90, 82, 74, 66, 58, 50, 44, 38, 32, 26, 20, 16, 12, 8, 6, 4, 2));   
    private final List<Integer> pumplistE = new ArrayList<Integer>(Arrays.asList(1295, 1195, 1095, 995, 945, 895, 845, 805, 765, 725, 695, 665, 635, 605, 
    		575, 545, 525, 505, 485, 465, 445, 425, 405, 390, 375, 360, 345, 330, 315, 300, 285, 270, 255, 243, 231, 219, 207, 195, 183, 171, 159, 147, 135,
    		125, 115, 105,  95, 85, 77, 69, 61, 53, 46, 40, 34, 30, 24, 18, 14, 10, 7, 5, 3, 1));
	
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
					Block lastEmpty = null;
					try {
						for (BlockIterator iterator = new BlockIterator(getPlayer().getWorld(), getPlayer().getLocation().toVector(), getPlayer().getLocation().getDirection(), 1, 7); iterator.hasNext(); ) {
							final Block block = iterator.next();
							if (!block.getType().isSolid()) {
								lastEmpty = block;
							}
						}
					} catch (IllegalStateException ignored) {
					}
					if (lastEmpty != null) {
						final MurderMystery murderMystery = (MurderMystery) getGame();
						if (murderMystery.getGold(getParticipant()) > 2) {
							murderMystery.consumeGold(getParticipant(), 2);
							this.targetLocation = lastEmpty.getLocation();
							skill.start();
							cantkill.pause();
						} else if (decreaseTimeUse(60)) {
							this.targetLocation = lastEmpty.getLocation();
							skill.start();	
							cantkill.pause();
						} else getPlayer().sendMessage("§4[§c!§4] §c피가 모자랍니다.");
					} else getPlayer().sendMessage("§4[§c!§4] §c바라보는 방향에 이동할 수 있는 곳이 없습니다.");
				} else getPlayer().sendMessage("§4[§c!§4] §c아직 스킬 지속 중입니다.");
			}
		}
	}
	
	private AbilityTimer invincible = new AbilityTimer(80) {
		
		@Override
		public void run(int count) {
			ac1.update("§3무적§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac1.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer cantkill = new AbilityTimer(1) {
		
		@Override
		protected void run(int count) {
			ac.update("§c공격 불가§f: §3" + df.format(count / 20.0) + "§f초");
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
	
	private boolean decreaseTimeUse(int count) {
		if (lefttime - count > 0) {
			lefttime = Math.max(0, lefttime - count);
			return true;
		} else return false;
	}
	
	private void decreaseTime(int count) {
		lefttime = Math.max(0, lefttime - count);
	}
	
	private void increaseTime(int count) {
		SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.6f);
		if (invincible.isRunning()) invincible.setCount(invincible.getCount() + 80);
		else {
			invincible.start();
		}
		stack = 0;
		lefttime = lefttime + count;
		if (lefttime > maxtime) maxtime = lefttime;
		if (lefttime > 200) NMS.clearTitle(getPlayer());
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onGameModeChange(PlayerGameModeChangeEvent e) {
		if (skill.isRunning() && getPlayer().getGameMode() == GameMode.SPECTATOR) e.setCancelled(true);
	}

	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerTeleport(PlayerTeleportEvent e) {
		if (skill.isRunning() && getPlayer().getGameMode() == GameMode.SPECTATOR) e.setCancelled(true);
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
	
	@SubscribeEvent(priority = 10)
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && invincible.isRunning()) {
			e.setCancelled(true);
		}
		if (!e.isCancelled() && e.getShooter().equals(getParticipant()) && !(e.getTarget().getAbility() instanceof AbstractMurderer)) {
			increaseTime(600);
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onMurder(MurderEvent e) {
		if (cantkill.isRunning() && getParticipant().equals(e.getMurderer().getParticipant())) {
			Stun.apply(getParticipant(), TimeUnit.TICKS, 30);
			e.setCancelled(true); 
		}
		if (!e.isCancelled()) {
			if (e.getMurderer().getParticipant().equals(getParticipant())) {
				increaseTime(600);
			}	
		}
	}
	
	private final AbilityTimer passive = new AbilityTimer() {
		
		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§c남은 피§7: §c" + df.format(lefttime / 20.0) + "§f초", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
		}

		@Override
		protected void onEnd() {
			SoundLib.ENTITY_GHAST_DEATH.playSound(getPlayer().getLocation(), 1, 0.5f);
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
			if (!skill.isRunning()) decreaseTime(1);
			bossBar.setTitle("§c남은 피§7: §c" + df.format(lefttime / 20.0) + "§f초");
			bossBar.setProgress(Math.min(1, Math.max(0, lefttime / (double) maxtime)));
			if (lefttime <= 200) bossBar.setColor(BarColor.PURPLE);
			else bossBar.setColor(BarColor.RED);
			
    		if (pumplistA.contains(lefttime)) SoundLib.BASS_DRUM.playInstrument(getPlayer(), Note.natural(0, Tone.A));
    		if (pumplistE.contains(lefttime)) SoundLib.BASS_DRUM.playInstrument(getPlayer(), Note.natural(0, Tone.E));
			
			if (lefttime <= 200) {
				if (count % 20 == 0) {
					NMS.sendTitle(getPlayer(), "§c§l" + (lefttime / 20), "", 0, 20, 0);	
				}
			}
			
			if (lefttime <= 0) stop(false);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer skill = new AbilityTimer() {
		private GameMode originalMode;
		private float flySpeed;

		@Override
		protected void onStart() {
			invincible.pause();
			SoundLib.ENTITY_WITCH_AMBIENT.playSound(getPlayer().getLocation(), 1f, 1.75f);
			SoundLib.ENTITY_BAT_LOOP.playSound(getPlayer().getLocation(), 1f, 1.4f);
			this.originalMode = getPlayer().getGameMode();
			if (originalMode == GameMode.SPECTATOR) originalMode = GameMode.SURVIVAL;
			this.flySpeed = getPlayer().getFlySpeed();
			getParticipant().attributes().TARGETABLE.setValue(false);
			getPlayer().setGameMode(GameMode.SPECTATOR);
		}

		@Override
		protected void run(int count) {
			if (getPlayer().getSpectatorTarget() != null) getPlayer().setSpectatorTarget(null);
			getPlayer().setFlySpeed(0f);
			if (targetLocation != null && count <= 30) {
				final Location playerLocation = getPlayer().getLocation();
				getPlayer().setVelocity(VectorUtil.validateVector(targetLocation.toVector().subtract(playerLocation.toVector()).multiply(0.3)));
				if (playerLocation.distanceSquared(targetLocation) < 1) {
					stop(false);
					getPlayer().teleport(targetLocation.setDirection(getPlayer().getLocation().getDirection()));
				}
			} else {
				stop(true);
				if (targetLocation != null)
					getPlayer().teleport(targetLocation.setDirection(getPlayer().getLocation().getDirection()));
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			SoundLib.ENTITY_BAT_TAKEOFF.playSound(getPlayer().getLocation(), 1f, 1.75f);
			getPlayer().setGameMode(originalMode);
			getPlayer().setVelocity(ZERO_VECTOR);
			getPlayer().setFlySpeed(flySpeed);
			getPlayer().setFlying(false);
			getParticipant().attributes().TARGETABLE.setValue(true);
			NMS.setInvisible(getPlayer(), false);
			if (cantkill.isPaused()) {
				cantkill.setCount(cantkill.getCount() + (stack * 6));
				cantkill.resume();
			} else {
				cantkill.start();
				cantkill.setCount((stack * 6));
			}
			invincible.resume();
			stack++;
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();

}
