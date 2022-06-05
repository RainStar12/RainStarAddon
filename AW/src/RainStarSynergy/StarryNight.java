package RainStarSynergy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Note.Tone;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(
		name = "", rank = Rank.A, species = Species.OTHERS, explain = {
		""
		})

public class StarryNight extends Synergy {
	
	public StarryNight(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> STACK_COOL = 
			synergySettings.new SettingObject<Double>(StarryNight.class, "stack-cooldown", 1.0,
            "# 스택을 쌓을 때 내부 쿨타임") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> DAMAGE_INCREASE = 
			synergySettings.new SettingObject<Integer>(StarryNight.class, "damage-increase", 15,
            "# 별 스택 당 공격력 증가 수치") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Double> STUN = 
			synergySettings.new SettingObject<Double>(StarryNight.class, "stun-duration", 0.3,
            "# 달 스택 당 기절 지속시간 증가") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private static final ImmutableMap<Boolean, String> type = ImmutableMap.<Boolean, String>builder()
			.put(true, "§e✭")
			.put(false, "§e🌙")
			.build();
	
	private static boolean isNight(long time) {
		return time > 12300 && time < 23850;
	}

	private void updateTime(World world) {
		final long diff = 15000 - world.getTime();
		if (diff < 0) {
			if (-diff > 2000) world.setTime(world.getTime() - 1500);
			else world.setTime(15000);
		} else if (diff > 0) {
			if (diff > 2000) world.setTime(world.getTime() + 1500);
			else world.setTime(15000);
		}
	}
	
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	private final int stun = (int) (STUN.getValue() * 20);
	private final double increase = DAMAGE_INCREASE.getValue() * 0.01;
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private final Random random = new Random();
	private final double stackcool = STACK_COOL.getValue();
	private static final RGB COLOR = RGB.of(235, 200, 21);
	private static final Crescent crescent = Crescent.of(1, 20);
	private int particleSide = 45;
	private static final Note[] NOTES = {
			Note.natural(0, Tone.E),
			Note.sharp(1, Tone.G),
			Note.natural(1, Tone.A),
			Note.sharp(1, Tone.C)
	};
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("StarFirework") || e.getDamager().hasMetadata("MoonFirework")) {
			e.setCancelled(true);
		}
		Participant target;
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			target = getGame().getParticipant((Player) e.getEntity());
			if (stackMap.containsKey(e.getEntity())) {
				if (stackMap.get(e.getEntity()).getCount() < (300 - (stackcool * 20))) {
					if (stackMap.get(e.getEntity()).addStack(true)) {
						int starstack = 0;
						int moonstack = 0;
						for (Boolean booleans : stackMap.get(e.getEntity()).stacks) {
							if (booleans) starstack++;
							else moonstack++;
						}
						if (starstack == 4) starstack = 6;
						if (moonstack == 4) moonstack = 6;
						e.setDamage(e.getDamage() * (starstack * increase));
						Stun.apply(target, TimeUnit.TICKS, (moonstack * stun));
						final Firework firework = getPlayer().getWorld().spawn(((Player) e.getEntity()).getEyeLocation(), Firework.class);
						final FireworkMeta meta = firework.getFireworkMeta();
						meta.addEffect(
								FireworkEffect.builder()
										.withColor(Color.fromRGB(254, 254, 108), Color.fromRGB(72, 254, 254))
										.with(Type.STAR)
										.build()
						);
						meta.setPower(0);
						firework.setFireworkMeta(meta);
						firework.setMetadata("StarFirework", NULL_VALUE);
						new SoundTimer(getPlayer(), true).start();
						new BukkitRunnable() {
							@Override
							public void run() {
								firework.detonate();
							}
						}.runTaskLater(AbilityWar.getPlugin(), 1L);
						updateTime(getPlayer().getWorld());
					} else {
						Location loc = target.getPlayer().getLocation();
						for (int iteration = 0; iteration < 5; iteration++) {
							double angle = Math.toRadians(72.0D * iteration);
							double nextAngle = Math.toRadians(72.0D * (iteration + 2));
							double x = Math.cos(angle) * 1.5D;
							double z = Math.sin(angle) * 1.5D;
							double x2 = Math.cos(nextAngle) * 1.5D;
							double z2 = Math.sin(nextAngle) * 1.5D;
							double deltaX = x2 - x;
							double deltaZ = z2 - z;
							for (double d = 0.0D; d < 1.0D; d += 0.125D) {
								loc.add(x + deltaX * d, 0.0D, z + deltaZ * d);
								ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
								loc.subtract(x + deltaX * d, 0.0D, z + deltaZ * d);
							}
						}
					}
				}
			} else {
				new Stack((Player) e.getEntity(), true).start();
				Location loc = target.getPlayer().getLocation();
				for (int iteration = 0; iteration < 5; iteration++) {
					double angle = Math.toRadians(72.0D * iteration);
					double nextAngle = Math.toRadians(72.0D * (iteration + 2));
					double x = Math.cos(angle) * 1.5D;
					double z = Math.sin(angle) * 1.5D;
					double x2 = Math.cos(nextAngle) * 1.5D;
					double z2 = Math.sin(nextAngle) * 1.5D;
					double deltaX = x2 - x;
					double deltaZ = z2 - z;
					for (double d = 0.0D; d < 1.0D; d += 0.125D) {
						loc.add(x + deltaX * d, 0.0D, z + deltaZ * d);
						ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
						loc.subtract(x + deltaX * d, 0.0D, z + deltaZ * d);
					}
				}
			}
		}
		if (NMS.isArrow(e.getDamager()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			Arrow arrow = (Arrow) e.getDamager();
			if (getPlayer().equals(arrow.getShooter())) {
				target = getGame().getParticipant((Player) e.getEntity());
				if (stackMap.containsKey(e.getEntity())) {
					if (stackMap.get(e.getEntity()).getCount() < (300 - (stackcool * 20))) {
						if (stackMap.get(e.getEntity()).addStack(false)) {
							int starstack = 0;
							int moonstack = 0;
							for (Boolean booleans : stackMap.get(e.getEntity()).stacks) {
								if (booleans) starstack++;
								else moonstack++;
							}
							if (starstack == 4) starstack = 6;
							if (moonstack == 4) moonstack = 6;
							e.setDamage(e.getDamage() * (starstack * increase));
							Stun.apply(target, TimeUnit.TICKS, (moonstack * stun));
							final Firework firework = getPlayer().getWorld().spawn(((Player) e.getEntity()).getEyeLocation(), Firework.class);
							final FireworkMeta meta = firework.getFireworkMeta();
							meta.addEffect(
									FireworkEffect.builder()
											.withColor(Color.fromRGB(254, 254, 108), Color.fromRGB(72, 254, 254))
											.with(Type.BALL_LARGE)
											.build()
							);
							meta.setPower(0);
							firework.setFireworkMeta(meta);
							firework.setMetadata("MoonFirework", NULL_VALUE);
							new SoundTimer(getPlayer(), false).start();
							new BukkitRunnable() {
								@Override
								public void run() {
									firework.detonate();
								}
							}.runTaskLater(AbilityWar.getPlugin(), 1L);
							updateTime(getPlayer().getWorld());
						} else {
							new CutParticle(particleSide).start();
							particleSide *= -1;
						}
					}
				} else {
					new CutParticle(particleSide).start();
					particleSide *= -1;
					new Stack((Player) e.getEntity(), false).start();
				}
			}
		}
	}
	
	private class SoundTimer extends AbilityTimer {

		private final Player center;
		private final boolean type;

		private SoundTimer(Player center, boolean type) {
			super(TaskType.NORMAL, NOTES.length);
			setPeriod(TimeUnit.TICKS, 3);
			this.type = type;
			this.center = center;
		}

		@Override
		protected void run(int count) {
			SoundLib.BELL.playInstrument(center.getLocation(), .4f, type ? NOTES[4 - count] : NOTES[count - 1]);
		}

	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		private List<Boolean> stacks = new ArrayList<>();
		
		private Stack(Player player, boolean stacktype) {
			super(300);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					"§7????");
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack(stacktype);
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addStack(boolean stacktype) {
			setCount(300);
			if (stacktype) {
				for (LivingEntity entity : LocationUtil.getNearbyEntities(LivingEntity.class, player.getLocation(), 4, 4, predicate)) {
					entity.setVelocity(player.getLocation().toVector().subtract(entity.getLocation().toVector()).multiply(0.75));
				}
			} else player.setVelocity(getPlayer().getLocation().toVector().subtract(player.getLocation().toVector()).multiply(0.6).setY(0));
			stack++;
			stacks.add(stacktype);
			String string = "";
			for (Boolean booleans : stacks) {
				string = string + type.get(booleans);
			}
			hologram.setText(string.concat(Strings.repeat("§7?", 4 - stack)));
			if (stack >= 4) {
				stop(false);
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(player);
		}
		
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors;

		private CutParticle(double angle) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(getPlayer().getLocation().getDirection().setY(0).normalize(), 90), getPlayer().getLocation().getDirection().setY(0).normalize(), angle);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -75);
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1.3, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
			}
			crescentVectors.rotateAroundAxis(axis, 40);
		}

	}
	
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final int sharpnessEnchant;
		private final double damage;
		private final Predicate<Entity> predicate;

		private final RGB color;
		private Location lastLocation;

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int sharpnessEnchant, double damage, RGB color) {
			super(4);
			StarryNight.this.bullet = this;
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.forward = arrowVelocity.multiply(10);
			this.sharpnessEnchant = sharpnessEnchant;
			this.damage = damage;
			this.color = color;
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(shooter)) return false;
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
			};
		}

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
				entity.setLocation(location);
				if (!isRunning()) {
					return;
				}
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (type.isSolid()) {
					startCooldown();
					stop(true);
					return;
				}
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						Damages.damageArrow(livingEntity, shooter, (float) (EnchantLib.getDamageWithSharpnessEnchantment(damage, sharpnessEnchant) * Math.max(0.25, Math.min(30, livingEntity.getLocation().distanceSquared(shooter.getLocation())) / 30) * 0.6));
						getPlayer().setVelocity(VectorUtil.validateVector(new Vector(dx, 0, dz).normalize().multiply(0.6)));
						stop(true);
						return;
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, color);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			startCooldown();
			entity.remove();
			StarryNight.this.bullet = null;
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
			StarryNight.this.bullet = null;
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
				final Player deflectedPlayer = deflector.getPlayer();
				new Bullet(deflectedPlayer, lastLocation, newDirection, sharpnessEnchant, damage, color).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
			}

		}

	}

}
