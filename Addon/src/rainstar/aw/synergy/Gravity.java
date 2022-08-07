package rainstar.aw.synergy;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Sphere;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

@AbilityManifest(name = "그래비티", rank = Rank.A, species = Species.OTHERS, explain = {
		"철괴 우클릭 시 특수한 §d중력 탄환§f을 장전하여 다음 활 발사시",
		"적중한 위치의 바닥으로부터 $[DURATION]초간 7칸의 중력장을 생성해냅니다. $[COOLDOWN]",
		"자신의 신체는 중력장의 영향과 낙하 피해를 무시합니다.",
		"자신이 중력장 안에 있을 때 중력장에 따른 버프를 받습니다.",
		"§5가중력§7: §f점프력과 이동 속도가 급격하게 낮아지며, 모든 엔티티가",
		" 지면으로 강하게 끌어당겨집니다. 오래 있으면 피해를 입습니다. §a버프 §f: §6힘 1",
		"§2반중력§7: §f모든 투사체가 멈춥니다. 반중력장이 해제될 때 투사체들의 정지도 해제되며",
		" 엔티티들은 반중력장의 중심으로부터 밀려납니다. §a버프 §f: §c재생 1",
		"§3무중력§7: §f모든 엔티티의 중력이 해제됩니다. 무중력장이 해제되기 전까진",
		" 무중력장을 벗어나도 유지됩니다. §a버프 §f: §3저항 2"
})

public class Gravity extends Synergy implements ActiveHandler {
	
	public Gravity(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update("§7§l⤋§f");
		}
	}
	
	private final ActionbarChannel ac = newActionbarChannel();
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private final int duration = DURATION.getValue();
	private int gravityType = 0;
	private Map<Arrow, Integer> arrowMap = new HashMap<>();
	private static final Sphere sphere = Sphere.of(7, 15);
	private static final Circle circle = Circle.of(7, 50);
	private final Vector zeroVec = new Vector(0, 0, 0);
	private boolean charged = false;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof Player) {
				if (entity.equals(getPlayer())) return false;
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
					return false;
				}
			}
			return true;
		}
	};
	
	private final Predicate<Block> blockpredicate = new Predicate<Block>() {
		@Override
		public boolean test(Block block) {
			if (!block.getType().isSolid() && !block.isLiquid()) {
				return true;
			}
			return false;
		}
	};
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(Gravity.class,
			"cooldown", 30, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DURATION = synergySettings.new SettingObject<Integer>(Gravity.class,
			"duration", 10, "# 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType.equals(ClickType.RIGHT_CLICK) && !cool.isCooldown()) {
			arrowMap.clear();
			Random random = new Random();
			switch(random.nextInt(3)) {
			case 0:
				getPlayer().sendMessage("§5[§d!§5] §f세 탄환 중 §5가중력§f 탄환이 장전되었습니다.");
				ac.update("§5§l⤋§f");
				gravityType = 1;
				break;
			case 1:
				getPlayer().sendMessage("§5[§d!§5] §f세 탄환 중 §2반중력§f 탄환이 장전되었습니다.");
				ac.update("§2§l⤋§f");
				gravityType = 2;
				break;
			case 2:
				getPlayer().sendMessage("§5[§d!§5] §f세 탄환 중 §3무중력§f 탄환이 장전되었습니다.");
				ac.update("§3§l⤋§f");
				gravityType = 3;
				break;
			}
			SoundLib.BLOCK_IRON_DOOR_CLOSE.playSound(getPlayer(), 1, 1.7f);
			charged = true;
			return cool.start();
		}
		return false;
	}		
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (gravityType != 0 && charged) {
			if (NMS.isArrow(e.getEntity()) && getPlayer().equals(e.getEntity().getShooter())) {
				switch(gravityType) {
				case 1:
					SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(0, Note.Tone.A));
					break;
				case 2:
					SoundLib.BASS_DRUM.playInstrument(getPlayer(), Note.natural(0, Note.Tone.A));
					break;
				case 3:
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Note.Tone.A));
					break;
				}
				arrowMap.put((Arrow) e.getEntity(), gravityType);
				new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1, 0), (Arrow) e.getEntity(),
						e.getEntity().getVelocity().length(), gravityType).start();
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (arrowMap.containsKey(e.getEntity()) && charged) {
			new GravityField(LocationUtil.floorY(e.getEntity().getLocation(), blockpredicate), arrowMap.get(e.getEntity())).start();
			arrowMap.remove(e.getEntity());
			charged = false;
			ac.update("§7§l⤋§f");
		}
	}
	
	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (!e.isCancelled() && getPlayer().equals(e.getEntity()) && e.getCause().equals(DamageCause.FALL)) {
			e.setCancelled(true);
			getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
		}
	}
	
	public class GravityField extends AbilityTimer implements Listener {
		
		private int type;
		private Location location;
		private Map<Projectile, Vector> vectorMap = new HashMap<>();
		private Map<Entity, Boolean> gravityMap = new HashMap<>();
		private Map<Damageable, Integer> stackMap = new HashMap<>();
		private Set<Entity> gravitySet = new HashSet<>();
		private double range;
		
		private int stacks = 0;
		private boolean turns = true;

		private RGB gradation;
		@SuppressWarnings("serial")
		private List<RGB> gradations = new ArrayList<RGB>() {
		};
		
		private GravityField(Location location, int type) {
			super(TaskType.NORMAL, duration * 20);
			setPeriod(TimeUnit.TICKS, 1);
			this.type = type;
			this.location = location;
			switch(type) {
			case 1:
				gradations.add(RGB.of(138, 9, 173));
				gradations.add(RGB.of(149, 17, 181));
				gradations.add(RGB.of(160, 25, 189));
				gradations.add(RGB.of(170, 33, 197));
				gradations.add(RGB.of(181, 41, 205));
				gradations.add(RGB.of(191, 49, 213));
				gradations.add(RGB.of(202, 58, 222));
				gradations.add(RGB.of(212, 66, 230));
				gradations.add(RGB.of(222, 74, 238));
				gradations.add(RGB.of(233, 83, 247));
				gradations.add(RGB.of(243, 91, 255));
				break;
			case 2:
				gradations.add(RGB.of(65, 155, 55));
				gradations.add(RGB.of(68, 165, 62));
				gradations.add(RGB.of(70, 175, 68));
				gradations.add(RGB.of(73, 186, 75));
				gradations.add(RGB.of(75, 196, 82));
				gradations.add(RGB.of(78, 206, 89));
				gradations.add(RGB.of(80, 215, 95));
				gradations.add(RGB.of(83, 225, 102));
				gradations.add(RGB.of(86, 235, 109));
				gradations.add(RGB.of(88, 245, 115));
				gradations.add(RGB.of(91, 255, 122));
				break;
			case 3:
				gradations.add(RGB.of(22, 178, 186));
				gradations.add(RGB.of(29, 186, 193));
				gradations.add(RGB.of(35, 193, 199));
				gradations.add(RGB.of(42, 200, 206));
				gradations.add(RGB.of(49, 208, 213));
				gradations.add(RGB.of(56, 215, 220));
				gradations.add(RGB.of(63, 222, 227));
				gradations.add(RGB.of(70, 229, 234));
				gradations.add(RGB.of(77, 236, 241));
				gradations.add(RGB.of(84, 243, 248));
				gradations.add(RGB.of(91, 251, 255));
				break;
			}
		}	
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			SoundLib.ENTITY_ELDER_GUARDIAN_DEATH.playSound(location, 1, 0.55f);
		}
		
		@Override
		protected void run(int count) {
			if (LocationUtil.isInCircle(location, getPlayer().getLocation(), range)) {
				switch(type) {
				case 1:
					PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), 20, 0, true);
					break;
				case 2:
					PotionEffects.REGENERATION.addPotionEffect(getPlayer(), 20, 0, true);
					break;
				case 3:
					PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 20, 1, true);
					break;
				}
			}
			if (count <= 26) {
				if (count % 2 == 0) {
					Sphere uppersphere = Sphere.of(count * 0.25, (int) (1 + (count * 0.5)));
					Circle uppercircle = Circle.of(count * 0.25, 24 + count);
					range = count * 0.25;
					for (Location loc : uppersphere.toLocations(location)) {
						if (loc.getY() >= location.getY() - 0.5) {
							ParticleLib.REDSTONE.spawnParticle(loc, gradations.get(1));	
						}		
					}
					for (Location loc : uppercircle.toLocations(location)) {
						ParticleLib.REDSTONE.spawnParticle(loc.add(0, -0.5, 0), gradations.get(1));	
					}
				}
			} else {
				range = 7;
				if (count % 5 == 0) {
					if (turns)
						stacks++;
					else
						stacks--;
					if (stacks % (gradations.size() - 1) == 0) {
						turns = !turns;
					}
					gradation = gradations.get(stacks);
					for (Location loc : sphere.toLocations(location)) {
						if (loc.getY() >= location.getY() - 0.5) {
							ParticleLib.REDSTONE.spawnParticle(loc, gradation);	
						}		
					}
					for (Location loc : circle.toLocations(location)) {
						ParticleLib.REDSTONE.spawnParticle(loc.add(0, -0.5, 0), gradation);	
					}
				}	
			}
			for (Entity entity : LocationUtil.getNearbyEntities(Entity.class, location, range, range, predicate)) {
				if (entity.getLocation().getY() >= location.getY() - 0.5) {
					if (count % 20 == 0) {
						if (entity instanceof Player) {
							Random random = new Random();
							if (random.nextBoolean()) {
								SoundLib.ENTITY_ELDER_GUARDIAN_HURT.playSound((Player) entity, 1, 0.5f);
							} else {
								SoundLib.ENTITY_GUARDIAN_HURT.playSound((Player) entity, 1, 0.5f);
							}	
						}
					}
					if (type == 1) {
						if (!(entity instanceof Projectile)) {
							entity.setVelocity(VectorUtil.validateVector(new Vector(entity.getVelocity().getX() * 0.9, -0.65, entity.getVelocity().getZ() * 0.9)));	
						} else {
							entity.setVelocity(VectorUtil.validateVector(new Vector(entity.getVelocity().getX() * 0.75, -2.5, entity.getVelocity().getZ() * 0.75)));
						}
						if (entity instanceof Damageable) {
							stackMap.put((Damageable) entity, stackMap.containsKey(entity) ? stackMap.get(entity) + 1 : 1);
						}
						if (!gravityMap.containsKey(entity)) {
							gravityMap.put(entity, entity.hasGravity());	
						}
						if (stackMap.containsKey(entity)) {
							if (stackMap.get(entity) >= 40 && stackMap.get(entity) % 20 == 0) {
								if (entity instanceof Damageable) ((Damageable) entity).damage(stackMap.get(entity) * 0.075);
							}
						}
						entity.setGravity(true);
					}
					if (type == 2) {
						if (entity instanceof Projectile && !vectorMap.containsKey(entity)) {
							Projectile projectile = (Projectile) entity;
							vectorMap.put(projectile, projectile.getVelocity());
							projectile.setGravity(false);
							projectile.setVelocity(zeroVec);
						}
						if (!(entity instanceof Projectile)) {
							entity.setVelocity(VectorUtil.validateVector(entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(0.4)));
						}
					}
					if (type == 3) {
						if (!(entity instanceof Projectile) && !gravitySet.contains(entity)) {
							entity.setVelocity(new Vector(entity.getVelocity().getX() * 1.2, 0.5, entity.getVelocity().getZ() * 1.2));
							gravitySet.add(entity);
						}
						if (!gravityMap.containsKey(entity)) {
							gravityMap.put(entity, entity.hasGravity());
						}
						entity.setGravity(false);
					}
				}
			}
		}
		
		@EventHandler
		public void onPlayerMove(PlayerMoveEvent e) {
			if (type == 1) {
				for (Player player : LocationUtil.getNearbyEntities(Player.class, location, range, range, predicate)) {
					if (player.getLocation().getY() >= location.getY() - 0.5) {
						if (player.equals(e.getPlayer())) {
							final double fromY = e.getFrom().getY(), toY = e.getTo().getY();
							if (toY > fromY) {
								player.setVelocity(new Vector(player.getVelocity().getX() * 0.01, -10, player.getVelocity().getZ() * 0.01));
							}
						}	
					}
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
			super.onEnd();
		}
		
		@Override
		public void onSilentEnd() {
			if (!vectorMap.isEmpty()) {
				for (Entry<Projectile, Vector> entry : vectorMap.entrySet()) {
					vectorMap.forEach(Projectile::setVelocity);
					entry.getKey().setGravity(true);
				}
				vectorMap.clear();
			}
			if (type == 1 || type == 3) {
				gravityMap.forEach(Entity::setGravity);
			}
			gravityMap.clear();
			gravitySet.clear();
			stackMap.clear();
			HandlerList.unregisterAll(this);
			super.onSilentEnd();
		}
		
	}
	
	public class Bullet extends AbilityTimer implements Listener {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private Vector forward;
		private int stacks1 = 0;
		private boolean turns1 = true;
		private boolean add = false;
		private Arrow arrow;
		private double length;
		private final int gravityType1;

		private RGB gradation1;

		@SuppressWarnings("serial")
		private List<RGB> gradations1 = new ArrayList<RGB>() {
		};

		private Location lastLocation;

		private Bullet(LivingEntity shooter, Location startLocation, Arrow arrow, double length, int type) {
			super(200);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.arrow = arrow;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(),
					startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.lastLocation = startLocation;
			this.length = length;
			this.forward = arrow.getLocation().clone().subtract(entity.getLocation().clone()).toVector().normalize()
					.multiply(length);
			this.gravityType1 = type;
			switch(type) {
			case 1:
				gradations1.add(RGB.of(138, 9, 173));
				gradations1.add(RGB.of(149, 17, 181));
				gradations1.add(RGB.of(160, 25, 189));
				gradations1.add(RGB.of(170, 33, 197));
				gradations1.add(RGB.of(181, 41, 205));
				gradations1.add(RGB.of(191, 49, 213));
				gradations1.add(RGB.of(202, 58, 222));
				gradations1.add(RGB.of(212, 66, 230));
				gradations1.add(RGB.of(222, 74, 238));
				gradations1.add(RGB.of(233, 83, 247));
				gradations1.add(RGB.of(243, 91, 255));
				break;
			case 2:
				gradations1.add(RGB.of(65, 155, 55));
				gradations1.add(RGB.of(68, 165, 62));
				gradations1.add(RGB.of(70, 175, 68));
				gradations1.add(RGB.of(73, 186, 75));
				gradations1.add(RGB.of(75, 196, 82));
				gradations1.add(RGB.of(78, 206, 89));
				gradations1.add(RGB.of(80, 215, 95));
				gradations1.add(RGB.of(83, 225, 102));
				gradations1.add(RGB.of(86, 235, 109));
				gradations1.add(RGB.of(88, 245, 115));
				gradations1.add(RGB.of(91, 255, 122));
				break;
			case 3:
				gradations1.add(RGB.of(22, 178, 186));
				gradations1.add(RGB.of(29, 186, 193));
				gradations1.add(RGB.of(35, 193, 199));
				gradations1.add(RGB.of(42, 200, 206));
				gradations1.add(RGB.of(49, 208, 213));
				gradations1.add(RGB.of(56, 215, 220));
				gradations1.add(RGB.of(63, 222, 227));
				gradations1.add(RGB.of(70, 229, 234));
				gradations1.add(RGB.of(77, 236, 241));
				gradations1.add(RGB.of(84, 243, 248));
				gradations1.add(RGB.of(91, 251, 255));
				break;
			}
		}

		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int i) {
			this.forward = arrow.getLocation().clone().subtract(entity.getLocation().clone()).toVector().normalize()
					.multiply(length);
			Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()),
						unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount)
						throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			};iterator.hasNext();) {
				final Location location = iterator.next();
				entity.setLocation(location);
				add = !add;
				if (add) {
					if (turns1)
						stacks1++;
					else
						stacks1--;
					if (stacks1 % (gradations1.size() - 1) == 0) {
						turns1 = !turns1;
					}
					gradation1 = gradations1.get(stacks1);
				}
				ParticleLib.REDSTONE.spawnParticle(location, gradation1);
			}
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

		@EventHandler
		private void onProjectileHit(final ProjectileHitEvent e) {
			if (e.getEntity().equals(arrow)) {
				stop(false);
			}
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
				new Bullet(deflectedPlayer, lastLocation, arrow, length, gravityType1).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
			}

		}

	}
	
}