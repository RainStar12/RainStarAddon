package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Sphere;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import rainstar.abilitywar.effect.ElectricShock;

@AbilityManifest(name = "스파크", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7기본 공격 §8- §e전기 구체§f: 가하는 모든 피해량이 $[DAMAGE_DECREASE]% 감소하지만, 바라보는 방향으로",
		" §e전기 구체§f를 쏘아낼 수 있습니다. §e전기 구체§f는 1초간 일직선으로 이동하다",
		" 다른 대상에게 적중하면 그 대상에게 $[ELECTRIC_SHOCK_DURATION]초간 §d§n감전§f 및 내가 손에 든 무기의",
		" $[ORB_DAMAGE]%의 피해를 입히고, $[CHAIN_RANGE]칸 내의 다른 대상이 있다면 그 대상을 향해",
		" 방향을 선회합니다. 이는 최대 5초까지, §a§l무한히§f 반복하며 점차 약해집니다.",
		" 매 $[ORB_COOLDOWN]초마다, 최대 $[DEFAULT_ORB_MAXIMUM]개의 §e전기 구체§f가 §e충전§f됩니다.",
		"§7철괴 우클릭 §8- §b축전기§f: §e전기 구체§f의 최대 수용량을 영구적으로 늘립니다.",
		" 즉시 모든 §e전기 구체§f를 §e충전§f합니다. 적을 처치해야 재사용할 수 있습니다.",
		"§7철괴 좌클릭 §8- §a임계 초과§f: 철괴를 든 채로 §e전기 구체§f를 발사하면 §e전기 구체§f가",
		" 추가로 대상을 $[STUN_DURATION]초간 §e§n기절§f시킵니다.",
		"§7패시브 §8- §d전기 역학§f: 번개나 §d§n감전§f에 의한 피해를 받지 않습니다."
		})
public class Spark extends AbilityBase implements ActiveHandler {
	
	public Spark(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Double> CHAIN_RANGE = rsAbilitySettings.new SettingObject<Double>(Spark.class,
			"chain-range", 10.0, "# 전기 구체가 체인 라이트닝을 발동할 거리") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_DECREASE = rsAbilitySettings.new SettingObject<Integer>(Spark.class,
			"damage-decrease", 30, "# 가하는 피해량 감소 수치", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DEFAULT_ORB_MAXIMUM = rsAbilitySettings.new SettingObject<Integer>(Spark.class,
			"default-orb-maximum", 2, "# 기본 전기 구체 최대치") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> ELECTRIC_SHOCK_DURATION = rsAbilitySettings.new SettingObject<Double>(Spark.class,
			"electric-shock-duration", 4.0, "# 감전 지속 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> ORB_COOLDOWN = rsAbilitySettings.new SettingObject<Double>(Spark.class,
			"orb-cooldown", 10.0, "# 전기 구체 충전 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ORB_DAMAGE = rsAbilitySettings.new SettingObject<Integer>(Spark.class,
			"orb-damage", 200, "# 전기 구체의 무기 비례 피해량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> STUN_DURATION = rsAbilitySettings.new SettingObject<Double>(Spark.class,
			"stun-duration", 1.0, "# 임계 초과 기절 부여량") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final double chainRange = CHAIN_RANGE.getValue();
	private final double damageDecrease = 1 - (DAMAGE_DECREASE.getValue() * 0.01);
	private final int defaultOrbMaximum = DEFAULT_ORB_MAXIMUM.getValue();
	private final int electricShockDuration = (int) (ELECTRIC_SHOCK_DURATION.getValue() * 20);
	private final int orbCooldown = (int) (ORB_COOLDOWN.getValue() * 20);
	private final double orbDamage = ORB_DAMAGE.getValue() * 0.01;
	private final int stunDuration = (int) (STUN_DURATION.getValue() * 20);
	
	private ActionbarChannel ac = newActionbarChannel(), progressAc = newActionbarChannel();
	private boolean acColor = false;
	private boolean useable = true;
	
	private int maxOrb = defaultOrbMaximum;
	private int nowOrb = defaultOrbMaximum;
	
	private int getReload(int baseReload) {
		double decrease = Wreck.calculateDecreasedAmount(75);
		if (Wreck.isEnabled(GameManager.getGame())) {
			if (decrease == 100) return 1;
			else return (int) (decrease * baseReload);
		} else return baseReload;
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) passive.start();
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			if (useable) {
				getPlayer().getWorld().strikeLightningEffect(getPlayer().getLocation());
				maxOrb++;
				nowOrb = maxOrb;
				useable = false;
				return true;
			} else getPlayer().sendMessage("§b[§e!§b] §c적을 처치해야 재사용 가능합니다.");
		}
		return false;
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (nowOrb > 0) {
				nowOrb--;
				double damage = getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
				if (e.getItem() != null && e.getItem().containsEnchantment(Enchantment.DAMAGE_ALL)) damage += ((e.getItem().getEnchantmentLevel(Enchantment.DAMAGE_ALL) + 1) * 0.5);
				damage *= orbDamage;
				new Orb(getPlayer(), getPlayer().getEyeLocation().getDirection(), getPlayer().getEyeLocation(), damage, (e.getItem() != null && e.getItem().getType().equals(Material.IRON_INGOT))).start();
			}
		}
	}
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		else if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		}
		
		if (damager != null && getPlayer().equals(damager)) {
			e.setDamage(e.getDamage() * damageDecrease);
		}
		
		if (e.getDamager() instanceof ArmorStand && e.getEntity().equals(getPlayer())) {
			ArmorStand stand = (ArmorStand) e.getDamager();
		    if (stand.hasMetadata("ElectricShock")) {
		        e.setCancelled(true);
		    }
		}
		
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getCause().equals(DamageCause.LIGHTNING)) e.setCancelled(true);
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (count % 20 == 0) acColor = !acColor;
			if (nowOrb < maxOrb && !charging.isRunning()) charging.start();
			ac.update((useable ? "§8§l⚡" : "") + Strings.repeat("§7§l⚡", maxOrb - nowOrb).concat(Strings.repeat((acColor ? "§e" : "§b") + "§l⚡", nowOrb)));
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer charging = new AbilityTimer(getReload(orbCooldown)) {
		
		private ProgressBar progressBar;
		
		@Override
		protected void onStart() {
			progressBar = new ProgressBar(getReload(orbCooldown), 10);
		}
		
		@Override
		protected void run(int count) {
			progressBar.step();
			progressAc.update("§d충전§7: " + progressBar.toString());
		}
		
		@Override
		protected void onEnd() {
			nowOrb = Math.min(maxOrb, nowOrb + 1);
			SoundLib.ENTITY_FIREWORK_ROCKET_TWINKLE.playSound(getPlayer(), 1, 2);
			progressAc.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onPlayerDeath(final PlayerDeathEvent e) {
		final Player entity = e.getEntity();
		if (!getPlayer().equals(entity) && getPlayer().equals(entity.getKiller())) {
			useable = true;
		}
	}

	public class Orb extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Predicate<Entity> predicate;
		private final double damage;
		private final boolean stun;
		private Vector forward;
		private LivingEntity lastVictim = null;
		private LivingEntity target = null;
		private Location lastLocation;
		private int stack = 0;
		private boolean turns = true;
		private final List<RGB> gradation1 = Gradient.createGradient(30, RGB.of(254, 254, 254), RGB.of(254, 246, 135), RGB.of(251, 204, 20));
		private final List<RGB> gradation2 = Gradient.createGradient(30, RGB.of(221, 245, 27), RGB.of(46, 186, 209), RGB.of(26, 104, 143));
		private int showParticle = 0;
		private double hitBox = 0.65;
		private final Random random = new Random();
		private int damagedStack = 0;
		private boolean first = true;
		
		private Orb(LivingEntity shooter, Vector arrowVelocity, Location startLocation, double damage, boolean stun) {
			super(20);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new Orb.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-hitBox, -hitBox, -hitBox, hitBox, hitBox, hitBox);
			this.forward = arrowVelocity.multiply(2);
			this.lastLocation = startLocation;
			this.damage = damage;
			this.stun = stun;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity.equals(lastVictim)) return false;
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
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
			stack = random.nextInt(30);
		}
		
		@Override
		protected void run(int i) {
			if (target != null) this.forward = target.getLocation().add(0, 1, 0).clone().subtract(lastLocation.clone()).toVector().normalize().multiply(0.85);
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
				entity.setLocation(location);
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					livingEntity.setNoDamageTicks(0);
					Damages.damageMagic(livingEntity, getPlayer(), false, (float) (damage - (damage * (Math.min(damagedStack, 9) / 10.0))) * (livingEntity instanceof Player ? 1 : 5));
					lastVictim = livingEntity;
					if (LocationUtil.getNearbyEntities(LivingEntity.class, location, chainRange, chainRange, predicate).size() > 0) {
						if (first) {
							this.setCount(100);
							first = false;
						}
						target = random.pick(LocationUtil.getNearbyEntities(LivingEntity.class, location, chainRange, chainRange, predicate));
					} else stop(false);	
					if (livingEntity instanceof Player) {
						Player victim = (Player) livingEntity;
						ElectricShock.apply(getGame().getParticipant(victim), TimeUnit.TICKS, (int) (electricShockDuration - (electricShockDuration * (Math.min(damagedStack, 9) / 10.0))));
						if (stun) Stun.apply(getGame().getParticipant(victim), TimeUnit.TICKS, (int) (stunDuration - (stunDuration * (Math.min(damagedStack, 9) / 10.0))));
					}
					damagedStack++;
				}
				if (showParticle >= 7) {
					double sphereRadius = i * 0.0033;
					Sphere sphere = Sphere.of(sphereRadius, (int) Math.max(1, (i * 0.033)));
					if (turns) stack++;
					else stack--;
					if (stack % (gradation1.size() - 1) == 0) {
						turns = !turns;
					}
					RGB nowColor1 = gradation1.get(stack), nowColor2 = gradation2.get(stack);
					for (Location loc : sphere.toLocations(location)) {
						ParticleLib.REDSTONE.spawnParticle(loc, nowColor1);	
					}
					showParticle = 0;
					
					double time = i * 0.15;

				    for (int p = 0; p < 18; p++) {
				        double t = (p / 18.0) * Math.PI * 2;

				        double x = Math.cos(t + time + Math.PI) * sphereRadius;
				        double y = Math.sin(t + time * 0.7 + Math.PI) * sphereRadius;
				        double z = Math.sin(t + time + Math.PI) * sphereRadius;

				        Vector pos = new Vector(x, y, z);
				        Vector normal = pos.clone().normalize();

				        double noise = (Math.sin(t * 6 + time * 2.5) * 0.25 + (Math.random() - 0.5) * 0.1) * sphereRadius;

				        pos.add(normal.multiply(noise));

				        Location particleLoc = location.clone().add(pos);

				        ParticleLib.REDSTONE.spawnParticle(particleLoc, nowColor2);
				    }
				}
				showParticle++;
			}
			if (i % 2 == 0) SoundLib.ENTITY_FIREWORK_ROCKET_TWINKLE.playSound(newLocation, 0.4f, 1.5f);
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
		}

		public class ArrowEntity extends CustomEntity {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
			}

		}
		
	}
	
}
