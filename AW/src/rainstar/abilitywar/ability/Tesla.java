package rainstar.abilitywar.ability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.Note.Tone;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;
import daybreak.google.common.collect.Iterables;
import rainstar.abilitywar.effect.ElectricShock;
import rainstar.abilitywar.effect.SnowflakeMark;

@AbilityManifest(
		name = "테슬라", rank = Rank.S, species = Species.HUMAN, explain = {
		"§e전기 속성§f의 천재 과학자, 테슬라.",
		"§7패시브 §8- §e변압§f: 근접 피해 외 자신이 주는 모든 피해는 피해를 주는 대신",
		" 피해량에 비례하여 대상을 기절시킵니다.",
		"§7철괴 좌클릭 §8- §e자기 폭풍§f: 바라보는 방향에 10초간 원 파티클이 생겨 다시", 
		" 좌클릭 시 전자기장을 생성해 범위 내 대상들을 중심으로 계속해서 끌어당기고,",
		" 7초 후 자기 폭발을 일으킵니다. $[LEFT_COOLDOWN]",
		" 폭발에 휘말린 대상은 14초의 감전 효과를 얻습니다.",
		"§7철괴 우클릭 §8- §e레일건§f: 다음 활 발사 시 초고속의 유도 탄환을 발사해 적중 대상에게",
		" 근접 피해를 주고, 플라즈마 폭발을 일으키고 대상의 위치로 순간 이동합니다. $[RIGHT_COOLDOWN]",
		"§7상태이상 §8- §d감전§f: 매 2초마다 0.5초간 기절합니다. 감전 도중에 기절 효과가 새로이",
		" 들어올 때마다 획득한 기절의 시간에 비례해 0.5초당 1의 피해를 끝날 때 입습니다.",
		"§8[§7HIDDEN§8] §b초전도§f: 원소 반응."
		},
		summarize = {
		"자신이 주는 모든 §c근접 피해 외 피해§f는 피해량에 비례한 §e기절 부여§f로 대체됩니다.",
		"§7철괴 좌클릭 시§f 자기 폭풍을 만들어내 플레이어들을 끌어당기고 폭발을 일으켜",
		"대상들을 §d감전§f시킵니다. $[LEFT_COOLDOWN]",
		"§7철괴 우클릭 시§f 레일건을 차징해 발사합니다. $[RIGHT_COOLDOWN]",
		"탄은 유도되고 적중 시 순간이동하며 폭발시킵니다.",
		"§d감전된 대상§f은 일정 시간마다 잠깐씩 기절하고, 별개의 기절 효과가 새로 들어오면",
		"§d감전 효과§f가 끝날 때 기절 시간에 비례해 피해를 입습니다."
		})

@SuppressWarnings("serial")
public class Tesla extends AbilityBase implements ActiveHandler {
	
	public Tesla(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> LEFT_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Tesla.class, "left-cooldown", 170,
            "# 좌클릭 쿨타임") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }

    };
    
	public static final SettingObject<Integer> RIGHT_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Tesla.class, "right-cooldown", 110,
            "# 우클릭 쿨타임") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }

    };
	
	private final Cooldown leftcool = new Cooldown(LEFT_COOLDOWN.getValue(), "자기 폭풍");
	private final Cooldown rightcool = new Cooldown(RIGHT_COOLDOWN.getValue(), "레일건 충전");
	
	private final ActionbarChannel ac = newActionbarChannel();
	private boolean charged = false;
	private boolean onetime = true;
	private int chargestack = 0;
	private static final Set<Material> nocheck;
	private Location targetblock;
	private PotionEffect levitation = new PotionEffect(PotionEffectType.LEVITATION, 80, 4, true, false);
	private static final Circle circle = Circle.of(7, 120);
	private static final Circle explosioncircle = Circle.of(2.5, 7);
	private static final RGB color = new RGB(107, 102, 254);
	private static final Circle CIRCLE = Circle.of(0.5, 15);
	private static final RGB gradation1 = RGB.of(96, 74, 123), gradation2 = RGB.of(87, 82, 134), gradation3 = RGB.of(78, 90, 144), 
			gradation4 = RGB.of(69, 99, 156), gradation5 = RGB.of(60, 107, 166), gradation6 = RGB.of(50, 116, 179),
			gradation7 = RGB.of(41, 124, 189), gradation8 = RGB.of(32, 133, 201), gradation9 = RGB.of(22, 141, 212), 
			gradation10 = RGB.of(41, 151, 196), gradation11 = RGB.of(5, 157, 233), gradation12 = RGB.of(66, 169, 174),
			gradation13 = RGB.of(53, 176, 188), gradation14 = RGB.of(103, 188, 141), gradation15 = RGB.of(105, 196, 140), 
			gradation16 = RGB.of(147, 207, 100), gradation17 = RGB.of(154, 215, 95), gradation18 = RGB.of(189, 226, 61),
			gradation19 = RGB.of(205, 235, 47), gradation20 = RGB.of(232, 245, 21), gradation21 = RGB.of(254, 254, 0);
	
	static { nocheck = ImmutableSet.of(MaterialX.AIR.getMaterial(), MaterialX.GRASS.getMaterial(), MaterialX.WATER.getMaterial(),
			MaterialX.LAVA.getMaterial()); }
	
	private List<RGB> gradations = new ArrayList<RGB>(){{
		add(gradation1); 
		add(gradation2);
		add(gradation3);
		add(gradation4);
		add(gradation5);
		add(gradation6);
		add(gradation7);
		add(gradation8);
		add(gradation9);
		add(gradation10);
		add(gradation11);
		add(gradation12);
		add(gradation13);
		add(gradation14);
		add(gradation15);
		add(gradation16);
		add(gradation17);
		add(gradation18);
		add(gradation19);
		add(gradation20);
		add(gradation21);
		}};
    
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
	
	private final AbilityTimer charging = new AbilityTimer() {
		
		@Override
		public void onStart() {
			SoundLib.ENTITY_ENDER_DRAGON_DEATH.playSound(getPlayer(), 0.8f, 1.8f);
		}
		
		@Override
		public void run(int count) {
			if (count % 20 == 0 && chargestack < 5) {
				chargestack++;
				if (chargestack == 5) {
					ac.update(Strings.repeat("§b§l⚡", chargestack).concat(Strings.repeat("§7§l⚡", 5 - chargestack)));
				} else {
					ac.update(Strings.repeat("§e§l⚡", chargestack).concat(Strings.repeat("§7§l⚡", 5 - chargestack)));	
				}
				switch(chargestack) {
				case 1: SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.2f);
						break;
				case 2: SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.3f);
						break;
				case 3: SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.4f);
						break;
				case 4: SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.5f);
						break;
				case 5: SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.6f);
    					SoundLib.BLOCK_IRON_DOOR_CLOSE.playSound(getPlayer(), 1, 1.7f);
    					charged = true;
						break;
				}
			}	
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onProjectileLaunch(EntityShootBowEvent e) {
		if (getPlayer().equals(e.getEntity()) && NMS.isArrow(e.getProjectile())) {
			if (charged) {
				SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 7, 1.75f);
				SoundLib.ENTITY_WITHER_HURT.playSound(getPlayer().getLocation(), 7, 1.4f);
				Arrow arrow = (Arrow) e.getProjectile();
				new Bullet(getPlayer(), arrow.getVelocity(), arrow.getLocation()).start();
				e.setCancelled(true);
				charged = false;
				chargestack = 0;
				charging.stop(false);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Projectile && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter()) && !player.equals(getPlayer())) {
				if (predicate.test(player)) {
					Stun.apply(getGame().getParticipant(player), TimeUnit.TICKS, (int) (e.getFinalDamage() * 7));
					e.setDamage(0);	
				}
			}
		}
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			if (!e.getCause().equals(DamageCause.ENTITY_ATTACK) && !e.getCause().equals(DamageCause.ENTITY_SWEEP_ATTACK) && !player.equals(getPlayer())) {
				if (predicate.test(player)) {
					Stun.apply(getGame().getParticipant(player), TimeUnit.TICKS, (int) (e.getFinalDamage() * 7));
					e.setDamage(0);	
				}
			}
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !leftcool.isCooldown()) {
	    	if (!skill.isRunning()) {
	    		skill.start();
	    	} else {
	    		skill.stop(false);
		    	return true;
	    	}
		}
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !rightcool.isCooldown()) {
			if (!charging.isRunning()) {
				charging.start();
				return rightcool.start();
			} else {
				getPlayer().sendMessage("§b[§e!§b] §f이미 차징된 §e레일건§f 탄환이 있습니다.");
			}
		}
		return false;
	}
	
	private final Duration skill = new Duration(200, leftcool) {

		@Override
		protected void onDurationProcess(int count) {
			targetblock = LocationUtil.floorY(getPlayer().getTargetBlock(nocheck, 30).getLocation());
			if (count % 2 == 0) {
				for (Location loc : circle.toLocations(targetblock).floor(targetblock.getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
				}	
			}
			if (count % 20 == 0) {
				SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			new MagneticStorm(targetblock).start();
		}

	}.setPeriod(TimeUnit.TICKS, 1);
	
	private class MagneticStorm extends AbilityTimer {
		
		private Location location;
		private int stack = 0;
		private boolean turn = true;
		private RGB gradation;
		private Vectors winds;
		
		private MagneticStorm(Location location) {
			super(140);
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
		}
		
		@Override
		protected void onStart() {
			winds = new Vectors();
			Random random = new Random();
			for (int i = 0; i < 15; i++) {
				winds.add(Vector.getRandom().multiply(new Vector(
						random.nextBoolean() ? 6 : -6,
						0,
						random.nextBoolean() ? 6 : -6
				)).setY(Math.random() * 1.4));
			}
		}
		
		@Override
		protected void run(int count) {
			if (count % 2 == 0) {
				if (turn) stack++;
				else stack--;
				if (stack % (gradations.size() - 1) == 0) {
					turn = !turn;
				}
				gradation = gradations.get(stack);
				for (Location loc : circle.toLocations(location).floor(location.getY())) {
					ParticleLib.REDSTONE.spawnParticle(loc, gradation);
				}	
			}
			
			if (count % 4 == 0) {
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, 7, predicate)) {
					player.setVelocity(VectorUtil.validateVector(location.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.35)));
				}
			}
			
			if (count % 20 == 0) {
				SoundLib.ENTITY_MINECART_INSIDE.playSound(location, 0.75f, 1.5f);
			}
			
			if (count % 50 == 0) {
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, 7, predicate)) {
					player.getWorld().strikeLightning(player.getLocation());
				}
			}
			
			for (Location loc : winds.rotateAroundAxisY(15).toLocations(location.clone().add(0, 0.5, 0))) {
				ParticleLib.CLOUD.spawnParticle(loc, 0, 0, 0, 1, 0.1f);
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			for (Location loc : explosioncircle.toLocations(location)) {
				location.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 1.35f, false, false);
				ParticleLib.EXPLOSION_HUGE.spawnParticle(loc);
				ParticleLib.EXPLOSION_HUGE.spawnParticle(loc);
				location.getWorld().strikeLightning(loc);
			}
			for (Player p : LocationUtil.getNearbyEntities(Player.class, location, 6, 6, predicate)) {
				if (getGame().getParticipant(p).hasEffect(SnowflakeMark.registration) && onetime) {
					ElectricShock.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 20);
					SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 20, 3);
	    			getPlayer().sendMessage("§8[§7HIDDEN§8] §b절대 영도§f에 도달한 플레이어를 감전시켰습니다.");
	    			getPlayer().sendMessage("§8[§7HIDDEN§8] §b초전도§f를 달성하였습니다.");
	    			p.addPotionEffect(levitation);
	    			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());
	    			onetime = false;
				} else {
					ElectricShock.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 14);	
				}
			}
		}
		
	}
	
	public class Bullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Predicate<Entity> predicate;
		private final Iterator<Vector> twist;
		private final Set<Player> hitPlayer = new HashSet<>();
		private Vector forward;
		private int stacks = 0;
		private boolean turns = true;

		private RGB shootgradation;
		
		private final RGB shootgradation1 = RGB.of(0, 181, 254), shootgradation2 = RGB.of(30, 189, 228), shootgradation3 = RGB.of(65, 198, 197),
				shootgradation4 = RGB.of(97, 207, 196), shootgradation5 = RGB.of(127, 215, 142), shootgradation6 = RGB.of(158, 224, 114),
				shootgradation7 = RGB.of(191, 233, 85), shootgradation8 = RGB.of(222, 241, 58), shootgradation9 = RGB.of(254, 250, 29);
		
		private List<RGB> shootgradations = new ArrayList<RGB>(){{
			add(shootgradation1);
			add(shootgradation2);
			add(shootgradation3);
			add(shootgradation4);
			add(shootgradation5);
			add(shootgradation6);
			add(shootgradation7);
			add(shootgradation8);
			add(shootgradation9);
		}};
		
		private Location lastLocation;
		
		private Bullet(LivingEntity shooter, Vector arrowVelocity, Location startLocation) {
			super(60);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.twist = Iterables.cycle(CIRCLE.clone().rotateAroundAxisY(-shooter.getLocation().getYaw()).rotateAroundAxis(VectorUtil.rotateAroundAxisY(shooter.getLocation().getDirection().setY(0).normalize(), 90), shooter.getLocation().getPitch() + 90)).iterator();
			this.forward = arrowVelocity.multiply(10);
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (hitPlayer.contains(entity)) return false;
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
		}
		
		@Override
		protected void run(int i) {
			Player nearest = LocationUtil.getNearestEntity(Player.class, entity.getLocation(), predicate);
			if (nearest != null) {
				this.forward = nearest.getLocation().add(0, 1, 0).clone().subtract(lastLocation.clone()).toVector().normalize().multiply(10);
			} else {
				stop(false);
			}
			Location newLocation = lastLocation.clone().add(forward);
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
				for (Player player : LocationUtil.getConflictingEntities(Player.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), 1, 1.4f);
					ParticleLib.EXPLOSION_HUGE.spawnParticle(player.getLocation());
					player.getWorld().strikeLightningEffect(player.getLocation());
					player.damage(15, getPlayer());
					player.getWorld().createExplosion(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 1.4f, false, false);
					hitPlayer.add(player);
					new BukkitRunnable() {
						@Override
						public void run() {
							getPlayer().teleport(player.getLocation());
						}
					}.runTaskLater(AbilityWar.getPlugin(), 2L);
					stop(false);
				}
				if (turns) stacks++;
				else stacks--;
				if (stacks % (shootgradations.size() - 1) == 0) {
					turns = !turns;
				}
				shootgradation = shootgradations.get(stacks);
				ParticleLib.REDSTONE.spawnParticle(location, shootgradation);
				if (i % 2 == 0) {
					ParticleLib.REDSTONE.spawnParticle(location.add(twist.next()), color);
				}
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
