package rainstar.abilitywar.game.murder.murderer;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.boundary.CenteredBoundingBox;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import rainstar.abilitywar.game.murder.system.Shield;
import rainstar.abilitywar.game.murder.system.config.statictype.DetectiveDetector;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

@AbilityManifest(name = "머더: 사기꾼", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 속이고 죽이세요!",
		"금 우클릭으로 금 7개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§7위장§8]§f 자신의 직업이 조사당할 때, 전직 내용을 §a광대§f로 속일 수 있습니다.",
		"이때 자신이 누군가를 활로 죽이면 §a광대§f처럼 보입니다.",
		"§8[§c연극§8]§f 금 좌클릭으로 금 1개를 소모해 의사, 경찰, 군인, 바텐더,",
		"육상선수, 아이돌 중 하나의 무작위 직업 스킬을 사용합니다. §8(§73회용§8)",
		"§8[§3사기§8]§f 누군가가 자신을 화살로 죽이려 할 때 §b관전§f 상태가",
		"되어 20~60초간 죽은 척을 할 수 있습니다. 이후 다시 활동을",
		"재개합니다. 부활 후 10초간은 공격할 수 없습니다. §8(§71회용§8)"
})
public class ScammerMurderer extends AbstractMurderer {

	public ScammerMurderer(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5사기꾼", "§f모든 §a시민§f과 §5탐정§f을 속이세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			skillnumber = random.nextInt(6);
			ac2.update("§b위장직업§7: §a"  + getJobname(skillnumber));
			getPlayer().sendMessage("§4[§c!§4] §a" + getJobExplain(skillnumber));
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}

	private int skillnumber = 0, skillstack = 3;
	private boolean actived = false;
	private ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	private String getJobname(int number) {
		switch(number) {
		case 0:
			return "의사";
		case 1:
			return "경찰";
		case 2:
			return "군인";
		case 3:
			return "바텐더";
		case 4:
			return "육상선수";
		case 5:
			return "아이돌";
		}
		return "";
	}
	
	private String getJobExplain(int number) {
		switch(number) {
		case 0:
			return "의사§7: §b사용 시 6초간 모든 머더가 사망하지 않게 만듭니다.";
		case 1:
			return "경찰§7: §b사용 시 10칸 이내의 모든 대상을 기절시킵니다.";
		case 2:
			return "군인§7: §b사용 시 즉시 화살 1발을 지급받고 다음 투사체를 군인 투사체로 변경합니다. 1발만 맞혀도 사망합니다.";
		case 3:
			return "바텐더§7: §b사용 시 갤럭시 매직을 발동시켜 2초간 내가 맞는 화살이 공격자만 사망합니다.";
		case 4:
			return "육상선수§7: §b사용 시 7초간 이동 속도가 2배가 됩니다.";
		case 5:
			return "아이돌§7: §b사용 시 28초간 무적이 되어 주변 대상들의 금을 빼앗고, 종료 시 빼앗은 금 5개당 실드를 1개 얻습니다. 이 상태로 사람을 죽일 수 없습니다.";
		}
		return "";
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId()));
		}
	};
	
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
				if (skillstack > 0) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), 1)) {
						switch(skillnumber) {
						case 0:
							doctor();
							break;
						case 1:
							police();
							break;
						case 2:
							soldier();
							break;
						case 3:
							bartender();
							break;
						case 4:
							athlete();
							break;
						case 5:
							idol();
							break;
						}
						skillstack--;
					}
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 연극을 사용하였습니다.");
			}
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onMurder(MurderEvent e) {
		if ((cantkill.isRunning() || idol.isRunning()) && getParticipant().equals(e.getMurderer().getParticipant())) e.setCancelled(true); 
	}
	
	@SubscribeEvent(priority = 10)
	private void onArrowKill(ArrowKillEvent e) {
		if ((cantkill.isRunning() || idol.isRunning()) && getParticipant().equals(e.getShooter())) e.setCancelled(true);
		if (!e.isCancelled()) {
			if (e.getShooter().equals(getParticipant()) && !(e.getTarget().getAbility() instanceof AbstractMurderer)) {
				if (e.getTarget().getAbility().getClass().getSimpleName().contains("Detective")) {
					new BukkitRunnable() {
						@Override
						public void run() {
							Bukkit.broadcastMessage("§5[§3광대§5] §5광대§f §3" + getPlayer().getName() + "§f님이 §e" + e.getTarget().getPlayer().getName() + "§f님을 §4처형§f하였습니다.");	
						}
					}.runTaskLater(AbilityWar.getPlugin(), 4L);
				} else {
					new BukkitRunnable() {
						@Override
						public void run() {
							Bukkit.broadcastMessage("§5[§3광대§5] §5광대§f §3" + getPlayer().getName() + "§f님이 §e" + e.getTarget().getPlayer().getName() + "§f님을 §c사살§f하였습니다.");	
						}
					}.runTaskLater(AbilityWar.getPlugin(), 4L);
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onArrowDeath(ArrowKillEvent e) {
		if (!(e.getShooter().getAbility() instanceof AbstractMurderer) && e.getTarget().equals(getParticipant()) && !actived && !e.isCancelled()) {
			e.setCancelled(true);
			NMS.broadcastEntityEffect(getPlayer(), (byte) 2);
			skill.start();
			skill.setCount(random.nextInt(801) + 400);
			SoundLib.ENTITY_PLAYER_DEATH.playSound(getPlayer().getLocation());
			Bukkit.broadcastMessage("§4" + getPlayer().getName() + "§c" + KoreanUtil.getJosa(getPlayer().getName(), Josa.이가) + " 죽었습니다.");
			if (getGame().hasModule(DetectiveDetector.class)) {
				DetectiveDetector module = getGame().getModule(DetectiveDetector.class);
				module.fakeHologram(getPlayer().getLocation(), getGame());
			}
		}
	}
	
	@SubscribeEvent
	private void onGameModeChange(PlayerGameModeChangeEvent e) {
		if (e.getPlayer().equals(getPlayer()) && skill.isRunning() && !e.getNewGameMode().equals(GameMode.SPECTATOR)) {
			e.setCancelled(true);
		}
	}
	
	private final AbilityTimer cantkill = new AbilityTimer(200) {
		
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
	
	private final AbilityTimer skill = new AbilityTimer(100) {

		@Override
		protected void onStart() {
			getPlayer().setGameMode(GameMode.SPECTATOR);
			getPlayer().sendMessage("§4[§c!§4] §3사기§f를 시작합니다.");
			getParticipant().attributes().TARGETABLE.setValue(false);
			actived = true;
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			cantkill.start();
			getPlayer().setGameMode(GameMode.ADVENTURE);
			getParticipant().attributes().TARGETABLE.setValue(true);
		}

		@Override
		protected void run(int count) {
			ac.update("§a부활까지§f: §3" + df.format(count / 20.0) + "§f초");
			getPlayer().setGameMode(GameMode.SPECTATOR);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	//의사
	
	public void doctor() {
		getPlayer().sendMessage("§a의술 §f능력을 사용했습니다.");
		doctor.start();
	}
	
	private static final Circle CIRCLE = Circle.of(5, 60);
	private static final RGB color = RGB.of(0, 0, 0);

	private final Duration doctor = new Duration(30) {
		@Override
		protected void onDurationProcess(int count) {
			for (final Location loc : CIRCLE.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(loc, color);
			}
		}
	}.setPeriod(TimeUnit.TICKS, 4);
	
	@SubscribeEvent
	private void onDoctorArrowKill(final ArrowKillEvent e) {
		if (doctor.isRunning()) {
			final Participant target = getGame().getParticipant(e.getTarget().getPlayer());
			if (target.getAbility() instanceof AbstractMurderer) {
				e.setCancelled(true);
				new BukkitRunnable() {
					@Override
					public void run() {
						NMS.broadcastEntityEffect(e.getTarget().getPlayer(), (byte) 35);
					}
				}.runTaskLater(AbilityWar.getPlugin(), 3L);
			}
		}
	}

	@SubscribeEvent
	private void onDoctorMurder(final MurderEvent e) {
		if (doctor.isRunning() && e.getTarget().getPlayer().getLocation().distanceSquared(getPlayer().getLocation()) <= 49) {
			final Participant target = e.getTarget();
			if (target.getAbility() instanceof AbstractMurderer) {
				e.setCancelled(true);
				new BukkitRunnable() {
					@Override
					public void run() {
						NMS.broadcastEntityEffect(e.getTarget().getPlayer(), (byte) 35);
					}
				}.runTaskLater(AbilityWar.getPlugin(), 3L);
			}
		}
	}
	
	//경찰
	
	public void police() {
		getPlayer().sendMessage("§6전기 충격 §f능력을 사용했습니다.");
		for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
			Stun.apply(getGame().getParticipant(player), TimeUnit.TICKS, 40);
			player.sendMessage("§6전기 충격!");
		}
	}
	
	//군인
	
	private boolean soldierarrow = false;
	
	public void soldier() {
		getPlayer().sendMessage("§a다음 화살은 군인의 특수 투사체가 적용됩니다.");
		final MurderMystery murderMystery = (MurderMystery) getGame();
		if (!addArrow()) {
			murderMystery.addGold(getParticipant());
		} else {
			if (!hasBow()) {
				getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
			}
		}
		soldierarrow = true;
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(EntityShootBowEvent e) {
		if (getPlayer().equals(e.getEntity()) && e.getProjectile() instanceof Arrow && soldierarrow) {
			e.setCancelled(true);
			ItemLib.removeItem(getPlayer().getInventory(), Material.ARROW, 1);
			final Arrow arrow = (Arrow) e.getProjectile();
			new Bullet(getPlayer(), arrow.getLocation(), arrow.getVelocity()).start();
			SoundLib.ENTITY_WITHER_BREAK_BLOCK.playSound(getPlayer().getLocation(), 4f, 2f);
			soldierarrow = false;
		}
	}
	
    public static Vector getForwardVector(Location location) {
        final float yaw = location.getYaw(), pitch = location.getPitch();
        final double radYaw = Math.toRadians(yaw), radPitch = Math.toRadians(pitch);
        final double cosPitch = FastMath.cos(radPitch);

        final double x = -FastMath.sin(radYaw) * cosPitch;
        final double y = -FastMath.sin(radPitch);
        final double z = FastMath.cos(radYaw) * cosPitch;

        return new Vector(x, y, z).normalize();
    }
    
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CenteredBoundingBox boundingBox;
		private final Vector forward;
		private final Predicate<Entity> predicate;

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity) {
			super(60);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.boundingBox = CenteredBoundingBox.of(startLocation, -.25, -.25, -.25, .25, .25, .25);
			this.forward = arrowVelocity.multiply(10);
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity.equals(getPlayer())) return false;
					return (entity instanceof Player) && getGame().isParticipating(entity.getUniqueId())
							&& !((MurderMystery) getGame()).isDead(entity.getUniqueId());
				}
			};
		}

		private Location lastLocation;

		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount) throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			}; iterator.hasNext(); ) {
				final Location location = iterator.next();
				boundingBox.setCenter(location);
				for (Player player : LocationUtil.getConflictingEntities(Player.class, shooter.getWorld(), boundingBox, predicate)) {
					if (!shooter.equals(player)) {
						if (((MurderMystery) getGame()).fireArrowHitEvent(getPlayer(), player)) {
						}
						stop(false);
						return;
					}
				}
				ParticleLib.TOWN_AURA.spawnParticle(location, 0, 0, 0, 1, 0);
			}
			lastLocation = newLocation;
		}

	}
	
	//바텐더
	
	public void bartender() {
		galaxymagic.start();
	}
	
	private BossBar bossBar = null;
	
	private final AbilityTimer galaxymagic = new AbilityTimer(40) {

		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§5갤럭시 매직", BarColor.PURPLE, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
		}

		@Override
		protected void run(int count) {
			bossBar.setProgress(count / 140.0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
	
	@SubscribeEvent(priority = 1)
	private void onArrowKillGalaxyMagic(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			if (galaxymagic.isRunning()) {
				e.setCancelled(true);
				ParticleLib.PORTAL.spawnParticle(e.getShooter().getPlayer().getEyeLocation(), 0, 0, 0, 150, 3);
				e.getShooter().getPlayer().setHealth(0);
			}
		}
	}
	
	//육상선수
	
	public void athlete() {
		getPlayer().sendMessage("§4[§c!§4] §b질주§f를 사용하였습니다.");
		athlete.start();
	}
	
	private final AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", 1.0, Operation.ADD_SCALAR);
	private final ActionbarChannel athac = newActionbarChannel();
	
	private final AbilityTimer athlete = new AbilityTimer(140) {

		@Override
		protected void onStart() {
			try {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
			} catch (IllegalArgumentException ignored) {
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			athac.update(null);
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
		}

		@Override
		protected void run(int count) {
			athac.update("§b질주§f: §3" + df.format(count / 20.0) + "§f초");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	//아이돌
	
	public void idol() {
		getPlayer().sendMessage("§4[§c!§4] §a라이브 개시!§f를 사용하였습니다.");
		song.start();
		idol.start();	
	}
	
	private final Circle livecircle = Circle.of(10, 100);
	
	public AbilityTimer idol = new AbilityTimer(560) {
		
		private int stack = 0;
		
		@Override
		public void run(int count) {
			getPlayer().getInventory().setHeldItemSlot(0);
			ac.update("§a라이브 모드§7: §d" + df.format(count / 20.0) + "§e초");
			PotionEffects.SPEED.addPotionEffect(getPlayer(), 20, 0, false, false, true);
			ParticleLib.END_ROD.spawnParticle(getPlayer().getLocation(), 10, 10, 10, 3, 0);
			if (count % 4 == 0) {
				for (final Location locs : livecircle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.TOTEM.spawnParticle(locs, 0, 0, 0, 1, 0.08f);
				}	
			}
			if (count % 40 == 0) {
				MurderMystery murderMystery = (MurderMystery) getGame();
				for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
					if (murderMystery.consumeGold(getGame().getParticipant(player), 1)) {
						murderMystery.addGold(getParticipant(), 1);
						stack++;
					}
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			Shield.addShield(getParticipant(), stack / 5);
			stack = 0;
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer song = new AbilityTimer(TaskType.NORMAL, 562) {
		
		private final Note As = new Note(16), Gs = new Note(14), C = new Note(18), Dslow = new Note(9),
				F = new Note(11), Cslow = new Note(7), Clow = new Note(6), Dlow = new Note(8),
				Cs = new Note(19), Ds = new Note(21), G = new Note(13);
		
		private void play(Note note) {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 3.9f, note);
		}
		
		@Override
		public void run(int count) {
			switch(count) {
			case 184:
			case 428:
			case 464:
			case 468:
				play(Clow);
				break;
			case 180:
			case 188:
			case 424:
			case 432:
			case 472:
			case 476:
				play(Cslow);
				break;
			case 192:
				play(Dlow);
				break;
			case 148:
			case 156:
			case 164:
			case 168:
			case 172:
			case 176:
			case 416:
			case 420:
			case 460:
				play(Dslow);
				break;
			case 152:
			case 200:
			case 216:
			case 220:
			case 408:
			case 456:
				play(F);
				break;
			case 404:
			case 452:
				play(G);
				break;
			case 8:
			case 16:
			case 28:
			case 32:
			case 40:
			case 52:
			case 60:
			case 76:
			case 92:
			case 116:
			case 136:
			case 140:
			case 144:
			case 208:
			case 248:
			case 260:
			case 268:
			case 280:
			case 284:
			case 292:
			case 312:
			case 320:
			case 324:
			case 332:
			case 336:
			case 384:
			case 392:
			case 400:
			case 440:
			case 444:
			case 500:
			case 516:
			case 520:
			case 532:
			case 544:
			case 556:
			case 560:
				play(Gs);
				break;
			case 4:
			case 12:
			case 36:
			case 68:
			case 84:
			case 120:
			case 244:
			case 256:
			case 264:
			case 288:
			case 328:
			case 352:
			case 380:
			case 484:
			case 488:
			case 552:
				play(As);
				break;
			case 20:
			case 44:
			case 124:
			case 224:
			case 240:
			case 272:
			case 296:
			case 360:
			case 376:
				play(C);
				break;
			case 232:
			case 368:
				play(Cs);
				break;
			case 304:
			case 504:
			case 536:
				play(Ds);
				break;
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = 1)
	private void onIdolArrowKill(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && idol.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}

}