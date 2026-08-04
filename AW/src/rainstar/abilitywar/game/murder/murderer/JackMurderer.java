package rainstar.abilitywar.game.murder.murderer;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

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
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 사형수 잭", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c수감§8]§f 전직하고 나서 20초 후 자신의 위치로부터 10칸이 §3감옥§f이 됩니다.",
		"§3감옥§f 바깥에서, 60초가 지나거나 화살을 맞으면 §c수감§f되고 좌표를 하나 공개합니다.",
		"§c수감§f되면 15초 동안 §3감옥§f에서 벗어날 수 없고 공격할 수 없으며 §e발광§f됩니다.",
		"§8[§3대탈출§8]§f 탈옥 중 금 좌클릭 시, 금을 20~64개 소모하여 §3감옥§f으로 즉시 이동합니다.",
		"§3감옥§f과의 거리가 가까울수록 소모량이 증가합니다. §8(§73회용§8)",
		"이후 신속 3, 점프 강화 4를 받고 지정 불가 상태가 됩니다.",
		"효과는 블록 250칸을 이동하거나 화살을 맞을 경우 종료되고 순간 기절합니다.",
		"2번째에는 125, 3번째에는 50칸 이동 시 종료됩니다."
})
public class JackMurderer extends AbstractMurderer {
	
	public JackMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5사형수 잭", "§f모든 §a목격자§f를 죽이고 성공적으로 탈옥하세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.start();
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					prison = getPlayer().getLocation();
					passive.start();
				}
			}.setInitialDelay(TimeUnit.SECONDS, 20).start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
			PotionEffects.SPEED.removePotionEffect(getPlayer());
			PotionEffects.JUMP.removePotionEffect(getPlayer());
		}
	}
	
	private Location prison = null;
	private final Circle circle = Circle.of(10, 120);
	private BossBar bossBar = null;
	private DecimalFormat df = new DecimalFormat("0.0");
	private int timeout = 1200;
	private boolean selfjailed = false;
	private boolean prisonbreak = false;
	private int leftblock = 250;
	private Block lastblock = null;
	private ActionbarChannel ac = newActionbarChannel();
	private int cost = 30;
	private int usecount = 3;
	
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
				if (!LocationUtil.isInCircle(prison, getPlayer().getLocation(), 10) && !jailing.isRunning()) {
					if (usecount > 0) {
						if (!prisonbreak) {
							final MurderMystery murderMystery = (MurderMystery) getGame();
							if (murderMystery.consumeGold(getParticipant(), cost)) {
								switch(usecount) {
								case 3:
									leftblock = 250;
									break;
								case 2:
									leftblock = 125;
									break;
								case 1:
									leftblock = 50;
									break;
								default:
									leftblock = 250;
									break;
								}
								selfjailed = true;
								jailing.start();
								usecount--;
							}	
						} else getPlayer().sendMessage("§4[§c!§4] §c이미 스킬이 지속되고 있습니다.");	
					} else getPlayer().sendMessage("§4[§c!§4] §c대탈출을 이미 전부 사용하였습니다.");	
				} else getPlayer().sendMessage("§4[§c!§4] §c당신은 탈옥 중이 아닙니다.");
			}
		}
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void onStart() {
			bossBar = Bukkit.createBossBar("§3탈옥§7: §b" + df.format(1200 / 20.0) + "§f초", BarColor.WHITE, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
		}
		
		@Override
		public void run(int count) {
			if (prison != null) {
				if (count % 2 == 0) {
					for (final Location locs : circle.toLocations(prison).floor(prison.getY())) {
						ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer(), locs, 0, 0, 0, 1, 0, MaterialX.STONE_BRICKS);
					}	
				}
				
				if (!prisonbreak) {
					if (!jailed.isRunning()) {
						if (LocationUtil.isInCircle(prison, getPlayer().getLocation(), 10)) {
							if (ServerVersion.getVersion() >= 10) bossBar.setVisible(false);
							bossBar.setProgress(1);
							timeout = 1200;
							cost = 65;
						} else {
							if (!jailing.isRunning()) timeout = Math.max(0, timeout - 1);
							if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);	
							bossBar.setColor(BarColor.WHITE);
							bossBar.setTitle("§3탈옥§7: §b" + df.format(timeout / 20.0) + "§f초");
							bossBar.setProgress(timeout / 1200.0);
							if (timeout <= 0) jailing.start();
							cost = (int) Math.max(20, Math.min(64, 160 - Math.min(160, Math.sqrt(getPlayer().getLocation().distanceSquared(prison)))));
							ac.update("§3탈출 비용§7: §e금 " + cost + "§f개");
						}	
					} else {
						if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);	
						bossBar.setTitle("§c수감됨!§7: §b" + df.format(jailed.getCount() / 20.0) + "§f초");
						bossBar.setColor(BarColor.RED);
						bossBar.setProgress(jailed.getCount() / 300.0);
						PotionEffects.GLOWING.addPotionEffect(getPlayer(), 20, 1, true, false, true);
					}	
				} else {
					if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);	
					bossBar.setTitle("§a대탈출§7: §c" + leftblock + "§f칸");
					bossBar.setColor(BarColor.GREEN);
					bossBar.setProgress(leftblock / 250.0);
					PotionEffects.SPEED.addPotionEffect(getPlayer(), 100, 2, true, false, true);
					PotionEffects.JUMP.addPotionEffect(getPlayer(), 100, 3, true, false, true);
					
					if (leftblock <= 0) {
						timeout = 1200;
						prisonbreak = false;
						getParticipant().attributes().TARGETABLE.setValue(true);
						PotionEffects.SPEED.removePotionEffect(getPlayer());
						PotionEffects.JUMP.removePotionEffect(getPlayer());
						Stun.apply(getParticipant(), TimeUnit.TICKS, 20);
					}
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	
	@SubscribeEvent(priority = 0, eventPriority = EventPriority.LOWEST)
	private void onMurder(MurderEvent e) {
		if (jailed.isRunning() && getParticipant().equals(e.getMurderer().getParticipant())) e.setCancelled(true); 
	}
	
	@SubscribeEvent(priority = 0, eventPriority = EventPriority.LOWEST)
	private void onArrowKilled(ArrowKillEvent e) {
		if (jailed.isRunning() && getParticipant().equals(e.getShooter())) e.setCancelled(true);
	}
	
	private AbilityTimer jailing = new AbilityTimer() {

		private Location location;
		private float flySpeed;

		@Override
		protected void onStart() {
			this.location = LocationUtil.floorY(LocationUtil.getRandomLocation(prison, 7.7));
			this.flySpeed = getPlayer().getFlySpeed();
			getParticipant().attributes().TARGETABLE.setValue(false);
			getPlayer().setGameMode(GameMode.SPECTATOR);
		}

		@Override
		protected void run(int count) {
			if (getPlayer().getGameMode() == GameMode.SPECTATOR) {
				getPlayer().setSpectatorTarget(null);
			}
			getPlayer().setFlySpeed(0f);
			Location playerLocation = getPlayer().getLocation();
			getPlayer().setVelocity(location.toVector().subtract(playerLocation.toVector()).normalize().multiply(3));
			if (playerLocation.distanceSquared(location) < 4.0) {
				stop(false);
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
			getPlayer().teleport(location);
		}

		@Override
		protected void onSilentEnd() {
			PotionEffects.BLINDNESS.addPotionEffect(getPlayer(), 5, 1, true, false, true);
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 0.5f);
			getPlayer().setGameMode(GameMode.ADVENTURE);
			getPlayer().setVelocity(new Vector());
			getPlayer().setFlySpeed(flySpeed);
			getPlayer().setFlying(false);
			getParticipant().attributes().TARGETABLE.setValue(true);
			if (!selfjailed) {
				jailed.start();
				String loctext = "";
				switch(random.nextInt(3)) {
				case 0:
					loctext = "§cX §e" + df.format(prison.getX());
					break;
				case 1:
					loctext = "§cY §e" + df.format(prison.getY());
					break;
				case 2:
					loctext = "§cZ §e" + df.format(prison.getZ());
					break;
				}
				Bukkit.broadcastMessage("§4[§b속보§4] §c§o사형수 잭§b이 수감되었습니다. 감옥 위치: " + loctext);
			} else {
				Bukkit.broadcastMessage("§4[§b속보§4] §c§o사형수 잭§b이 탈옥했습니다!");
				SoundLib.ENTITY_WITHER_SPAWN.playSound(getPlayer().getLocation(), 1000, 0.7f);
				prisonbreak = true;
				getParticipant().attributes().TARGETABLE.setValue(false);
			}
			selfjailed = false;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer jailed = new AbilityTimer(300) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = 7)
	private void onArrowKill(ArrowKillEvent e) {
		if (!e.isCancelled()) {
			if (e.getTarget().equals(getParticipant())) {
				if (prisonbreak) {
					e.setCancelled(true);
					timeout = 1200;
					prisonbreak = false;
					getParticipant().attributes().TARGETABLE.setValue(true);
					PotionEffects.SPEED.removePotionEffect(getPlayer());
					PotionEffects.JUMP.removePotionEffect(getPlayer());
					Stun.apply(getParticipant(), TimeUnit.TICKS, 20);
				} else if (!LocationUtil.isInCircle(prison, getPlayer().getLocation(), 10)) {
					e.setCancelled(true);
					jailing.start();
				}
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
	
	@SubscribeEvent
	private void onPlayerMove(PlayerMoveEvent e) {
		if (jailed.isRunning() && e.getPlayer().equals(getPlayer()) && !LocationUtil.isInCircle(prison, e.getTo(), 10)) {
			e.setTo(e.getFrom());
		}
		if (e.getPlayer().equals(getPlayer())) {
			if (prisonbreak) {
				Block down = getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
				if (!down.isEmpty() && !down.equals(lastblock)) {
					leftblock = Math.max(0, leftblock - 1);
					lastblock = down;
				}
	        }
		}
	}
	
	@SubscribeEvent
	private void onGameModeChange(PlayerGameModeChangeEvent e) {
		if (e.getPlayer().equals(getPlayer()) && jailing.isRunning() && !e.getNewGameMode().equals(GameMode.SPECTATOR)) {
			e.setCancelled(true);
		}
	}
	
}
