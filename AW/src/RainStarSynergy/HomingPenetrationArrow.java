package RainStarSynergy;

import com.google.common.base.Strings;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Sphere;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

@AbilityManifest(name = "유도 관통화살", rank = Rank.L, species = Species.OTHERS, explain = {
		"활을 쏘면 벽과 생명체를 통과하며 특수한 능력이 있는 발사체를 쏩니다.",
		"발사체는 가장 가까운 대상에게 유도되며, 대상이 피격 시 제외하고 다음으로",
		"가장 가까운 대상에게 다시 유도됩니다. 화살은 1.5초간 지속됩니다.",
		"탄창에는 $[AMMO_SIZE_CONFIG]개의 탄약이 들어있습니다. 탄약을 모두 소진하면 1.5초간 재장전하며,",
		"임의의 능력을 가진 탄약으로 탄창이 다시 채워집니다.",
		"탄약을 쏠 때마다 0.5초의 §3대기시간§f을 가지며, 이 §3대기시간§f은 매번 0.25초씩 3초까지",
		"늘어납니다. 30초간 발사를 중단하면, §3대기시간§f이 다시 0.5초로 초기화됩니다.",
		"§c절단§f: 대상에게 추가 근접 대미지를 입힙니다.",
		"§5중력§f: 대상을 0.5초간 기절시키고, 대상 주위 4칸의 생명체를 대상에게 끌어갑니다.",
		"§e풍월§f: 대상을 멀리 밀쳐냅니다."
})
public class HomingPenetrationArrow extends Synergy {

	public static final SettingObject<Integer> AMMO_SIZE_CONFIG = synergySettings.new SettingObject<Integer>(HomingPenetrationArrow.class, "ammo-size", 4,
			"# 탄창 크기") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	private static final RGB RED = new RGB(219, 64, 66);
	private static final RGB PURPLE = new RGB(138, 9, 173);
	private static final RGB YELLOW = new RGB(255, 246, 122);
	private static final Sphere sphere = Sphere.of(4, 10);
	private final Random random = new Random();
	private double delay = 0.5;
	private final DecimalFormat df = new DecimalFormat("0.00");
	private final List<ArrowType> arrowTypes = Arrays.asList(
			new ArrowType(ChatColor.RED, "절단") {
				@Override
				protected void launchArrow(Arrow arrow, int powerLevel) {
					SoundLib.ENTITY_ARROW_SHOOT.playSound(getPlayer());
					new Parabola(getPlayer(), OnHitBehavior.CUT, arrow.getLocation(), arrow.getVelocity(), getPlayer().getLocation().getPitch(), powerLevel, RED).start();
				}
			},
			new ArrowType(ChatColor.DARK_PURPLE, "중력") {
				@Override
				protected void launchArrow(Arrow arrow, int powerLevel) {
					SoundLib.ENTITY_ARROW_SHOOT.playSound(getPlayer());
					SoundLib.PIANO.playInstrument(getPlayer(), Note.flat(0, Note.Tone.D));
					new Parabola(getPlayer(), OnHitBehavior.GRAVITY, arrow.getLocation(), arrow.getVelocity(), getPlayer().getLocation().getPitch(), powerLevel, PURPLE).start();
				}
			},
			new ArrowType(ChatColor.YELLOW, "풍월") {
				@Override
				protected void launchArrow(Arrow arrow, int powerLevel) {
					SoundLib.ENTITY_ARROW_SHOOT.playSound(getPlayer());
					SoundLib.PIANO.playInstrument(getPlayer(), Note.flat(1, Note.Tone.B));
					new Parabola(getPlayer(), OnHitBehavior.WIND, arrow.getLocation(), arrow.getVelocity(), getPlayer().getLocation().getPitch(), powerLevel, YELLOW).start();
				}
			}
	);

	private class Ammo {

		private final int ammoSize = AMMO_SIZE_CONFIG.getValue();
		private final LinkedList<ArrowType> ammo = new LinkedList<>();

		private Ammo() {
			reload();
		}

		private void reload() {
			ammo.clear();
			for (int i = 0; i < ammoSize; i++) {
				ammo.add(random.pick(arrowTypes));
			}
		}

		private ArrowType poll() {
			return ammo.poll();
		}

		private boolean hasAmmo() {
			return !ammo.isEmpty();
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			for (ArrowType type : ammo) {
				builder.append(type.color).append("▐");
			}
			builder.append(ChatColor.GRAY.toString()).append(Strings.repeat("▐", ammoSize - ammo.size()));
			return builder.toString();
		}
	}

	private final ActionbarChannel actionbarChannel = newActionbarChannel();
	private final Ammo ammo = new Ammo();
	private AbilityTimer reload = null;

	public HomingPenetrationArrow(AbstractGame.Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			actionbarChannel.update(ammo.toString());
			resetcount.start();
		}
	}

	@SubscribeEvent(ignoreCancelled = true)
	private void onProjectileLaunch(EntityShootBowEvent e) {
		if (getPlayer().equals(e.getEntity()) && NMS.isArrow(e.getProjectile())) {
			e.setCancelled(true);
			if (reload == null) {
				if (!shotdelay.isRunning()) {
					if (!ammo.hasAmmo()) {
						startReload();
						return;
					}
					ammo.poll().launchArrow((Arrow) e.getProjectile(), e.getBow().getEnchantmentLevel(Enchantment.ARROW_DAMAGE));
					shotdelay.start();
					delay = Math.min(3, delay + 0.25);
					if (resetcount.isRunning()) {
						resetcount.setCount(600);
					} else {
						resetcount.start();
					}
					actionbarChannel.update(ammo.toString());
					if (!ammo.hasAmmo()) {
						startReload();
					}	
				} else {
					getPlayer().sendMessage("§c[§5!§e] §3발사 대기시간§f입니다. §3남은 시간§7: §f" + df.format(shotdelay.getCount() * 0.05) + "초");
				}
			} else {
				getPlayer().sendMessage("§c[§5!§e] §b재장전 §f중입니다.");
			}
		}
	}

	private final AbilityTimer shotdelay = new AbilityTimer(10) {
		
		@Override
		public void onStart() {
			shotdelay.setCount((int) (delay * 20));
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer resetcount = new AbilityTimer(600) {
		
		@Override
		public void onEnd() {
			delay = 0.5;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private void startReload() {
		final int reloadCount = Wreck.isEnabled(GameManager.getGame()) ? (int) (Wreck.calculateDecreasedAmount(50) * 30.0) : 30;
		this.reload = new AbilityTimer(reloadCount) {
			private final ProgressBar progressBar = new ProgressBar(reloadCount, 20);

			@Override
			protected void run(int count) {
				progressBar.step();
				actionbarChannel.update("재장전: " + progressBar.toString());
			}

			@Override
			protected void onEnd() {
				ammo.reload();
				HomingPenetrationArrow.this.reload = null;
				actionbarChannel.update(ammo.toString());
			}
		}.setPeriod(TimeUnit.TICKS, 4).setBehavior(RestrictionBehavior.PAUSE_RESUME);
		reload.start();
	}

	public interface OnHitBehavior {
		OnHitBehavior CUT = new OnHitBehavior() {
			@Override
			public void onHit(HomingPenetrationArrow ability, Damageable damager, Damageable victim) {
				ParticleLib.SWEEP_ATTACK.spawnParticle(victim.getLocation(), 1, 1, 1, 3);
				if (victim instanceof LivingEntity) {
					((LivingEntity) victim).setNoDamageTicks(0);
				}
				victim.damage(5, damager);
			}
		};
		OnHitBehavior GRAVITY = new OnHitBehavior() {
			@Override
			public void onHit(HomingPenetrationArrow ability, Damageable damager, Damageable victim) {
				for (Location location : sphere.toLocations(victim.getLocation())) {
					ParticleLib.REDSTONE.spawnParticle(location, PURPLE);
				}
				for (LivingEntity entity : LocationUtil.getNearbyEntities(LivingEntity.class, victim.getLocation(), 4, 4, new Predicate<Entity>() {
					@Override
					public boolean test(Entity entity) {
						if (entity.equals(damager)) return false;
						if (entity instanceof Player) {
							if (!ability.getGame().isParticipating(entity.getUniqueId())
									|| (ability.getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) ability.getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
									|| !ability.getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
								return false;
							}
							if (ability.getGame() instanceof Teamable) {
								final Teamable teamGame = (Teamable) ability.getGame();
								final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(damager.getUniqueId());
								return participant == null || !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
						return true;
					}
				})) {
					entity.setVelocity(victim.getLocation().toVector().subtract(entity.getLocation().toVector()).multiply(0.75));
				}
				final Participant participant = ability.getGame().getParticipant(victim.getUniqueId());
				if (participant != null) {
					Stun.apply(participant, TimeUnit.TICKS, 10);
				}
			}
		};
		OnHitBehavior WIND = new OnHitBehavior() {
			@Override
			public void onHit(HomingPenetrationArrow ability, Damageable damager, Damageable victim) {
				Vector vector = damager.getLocation().toVector().subtract(victim.getLocation().toVector()).multiply(-1);
				if (vector.length() > 0.01) {
					vector.normalize().multiply(2);
				}
				victim.setVelocity(vector.setY(0));
			}
		};

		void onHit(HomingPenetrationArrow ability, Damageable damager, Damageable victim);
	}

	private abstract static class ArrowType {

		private final ChatColor color;
		private final String name;

		private ArrowType(ChatColor color, String name) {
			this.color = color;
			this.name = color.toString() + name;
		}

		protected abstract void launchArrow(Arrow arrow, int powerEnchant);

	}

	public class Parabola extends AbilityTimer {

		private final LivingEntity shooter;
		private final OnHitBehavior onHitBehavior;
		private final CustomEntity entity;
		private final double velocity;
		private final int powerEnchant;
		private Vector forward;
		private final Predicate<Entity> predicate;

		private final RGB color;
		private final Set<Damageable> attacked = new HashSet<>();
		private Location lastLocation;
		private Parabola(LivingEntity shooter, OnHitBehavior onHitBehavior, Location startLocation, Vector arrowVelocity, double angle, int powerEnchant, RGB color) {
			super(30);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.onHitBehavior = onHitBehavior;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
			this.velocity = Math.sqrt((arrowVelocity.getX() * arrowVelocity.getX()) + (arrowVelocity.getY() * arrowVelocity.getY()) + (arrowVelocity.getZ() * arrowVelocity.getZ()));
			this.powerEnchant = powerEnchant;
			this.forward = arrowVelocity.setY(arrowVelocity.getY() * 0.7);
			this.color = color;
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity.equals(shooter)) return false;
					if (entity instanceof ArmorStand) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(shooter.getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
						if (attacked.contains(entity)) return false;
					}
					return true;
				}
			};
		}

		@Override
		protected void run(int i) {
			Damageable nearest = LocationUtil.getNearestEntity(Damageable.class, entity.getLocation(), predicate);
			if (nearest != null) {
				this.forward = nearest.getLocation().add(0, 1, 0).clone().subtract(lastLocation.clone()).toVector().normalize().multiply(1.1);
			} else {
				stop(false);
			}
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.2);
				private final int amount = (int) (vectorBetween.length() / 0.2);
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
				Location location = iterator.next();
				if (location.getY() < 0) {
					stop(false);
					return;
				}
				entity.setLocation(location);
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
					if (damageable.isValid() && !damageable.isDead() && !shooter.equals(damageable) && !attacked.contains(damageable)) {
						Damages.damageArrow(damageable, getPlayer(), EnchantLib.getDamageWithPowerEnchantment(Math.round(2.5f * velocity * 10) / 10.0f, powerEnchant));
						onHitBehavior.onHit(HomingPenetrationArrow.this, shooter, damageable);
						attacked.add(damageable);
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, color);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			attacked.clear();
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			attacked.clear();
			entity.remove();
		}

		public class ArrowEntity extends CustomEntity implements Deflectable {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			public Vector getDirection() {
				return forward.clone();
			}

			@Override
			public void onDeflect(Participant deflector, Vector newDirection) {
				stop(false);
				Player deflectedPlayer = deflector.getPlayer();
				new Parabola(deflectedPlayer, onHitBehavior, lastLocation, newDirection, getPlayer().getLocation().getDirection().getY() * 90, powerEnchant, color).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
				Parabola.this.stop(false);
			}

		}

	}

}