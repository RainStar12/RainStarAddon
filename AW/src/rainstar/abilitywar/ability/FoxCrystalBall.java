package rainstar.abilitywar.ability;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.SuperRegen;

@AbilityManifest(name = "여우 구슬", rank = Rank.L, species = Species.OTHERS, explain = {
		"주변 $[EATING_RANGE]칸 내 생명체가 회복할 때 정기를 획득합니다.",
		"구슬의 효과는 $[CHANGE_PERIOD]초마다 현재 상황에 적합하게 변경됩니다.",
		"§a회복 §7-§f 5초 지속의 §d§n초회복§f 상태를 하나 부여합니다. $[MANA_HEALING]",
		"§b속사 §7-§f $[RAPID_RANGE]칸 내의 가장 가까운 적에게 §c고정 피해§f §50.211§f의 투사체를 §b속사§f합니다.",
		" 속사 피해는 기본 무적 시스템을 무시하고 계속 입힙니다. $[MANA_RAPID]",
		" 속사 피해를 입은 적은 밀려나지 않고 반대로 끌려옵니다.",
		"§d유도 §7-§f 대상의 방향으로 빠르게 나아가는 §d유도 미사일§f을 발사합니다.",
		" 미사일은 지형지물 혹은 생명체에 충돌 시 폭발합니다. $[MANA_HOMING]",
		"§e관통 §7-§f $[PIERCE_RANGE]칸 내의 가장 먼 대상에게 직격하는 특수 투사체를 발사합니다.",
		" 투사체는 적을 §e§n기절§f시키고 지형지물과 생명체, 방어력을 §e관통§f합니다. $[MANA_PIERCE_SHOT]",
		"§8은신 §7-§f 신속 및 타게팅 불능 상태가 됩니다. §3소모 §7: §f0",
		"§b[§7아이디어 제공자§b] §5Phillip_MS"
		},
		summarize = {
		"주변 생명체의 회복을 먹고 사는 구슬이 지원사격을 보내줍니다.",
		"일정 주기마다 구슬의 효과가 현재 상황에 적합하게 변경됩니다.",
		"§2[§a회복§2] §3[§b속사§3] §5[§d유도§5] §6[§e관통§6] §8[§7은신§8]"
		})

public class FoxCrystalBall extends AbilityBase {
	
	public FoxCrystalBall(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> PIERCE_DAMAGE 
	= abilitySettings.new SettingObject<Double>(FoxCrystalBall.class,
			"pierce-damage", 1.0, "# 관통의 대미지") {
		
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> HOMING_DAMAGE 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"homing-damage", 15, "# 폭발 대미지") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> EATING_RANGE 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"eating-range", 15, "# 정기 흡수의 범위") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> PIERCE_RANGE 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"pierce-range", 14, "# 관통 상태의 범위") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Double> RAPID_RANGE 
	= abilitySettings.new SettingObject<Double>(FoxCrystalBall.class,
			"rapid-range", 4.5, "# 속사 상태의 범위") {
		
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> HOMING_RANGE 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"homing-range", 20, "# 유도 상태의 적 도망을 인식하는 범위") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> CHANGE_PERIOD 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"change-period", 10, "# 구슬 효과의 변경 주기 (단위: 초)") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> MANA_PIERCE_SHOT 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"mana-pierce", 25, "# 관통 상태의 마나 소모량") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§3소모 §7: §f" + getValue();
        }
		
	};
	
	public static final SettingObject<Integer> MANA_HEALING 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"mana-heal", 70, "# 회복 상태의 마나 소모량") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§3소모 §7: §f" + getValue();
        }
		
	};
	
	public static final SettingObject<Integer> MANA_RAPID 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"mana-rapid", 2, "# 속사 상태의 마나 소모량") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§3소모 §7: §f" + getValue();
        }
		
	};
	
	public static final SettingObject<Integer> MANA_HOMING 
	= abilitySettings.new SettingObject<Integer>(FoxCrystalBall.class,
			"mana-homing", 75, "# 유도 상태의 마나 소모량") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§3소모 §7: §f" + getValue();
        }
		
	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer()))
				return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
							participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	manaUpdater.start();
	    	crystalball.start();
	    }
	}
	
	private Random random = new Random();
	
	private int type = 0;
	private int rapidcount = 0;
	private String skillName = "§8은신";
	private int mana = 0;
	private int requireMana = 0;
	private BossBar bossBar = null;
	private Player target = null;
	
	private Location currentForward;
	private Location current;
	private Vector forward, left, right;
	private boolean side = false;
	
	private int shotDelay = 0;
	private double addtionalMana = 0.0;
	private Location location;
	
	private ActionbarChannel ac = newActionbarChannel();
	
	private boolean isLowHealth = false;
	private boolean isClose = false;
	private boolean isFar = false;
	private boolean isAttackable = false;
	
	private final double damage = PIERCE_DAMAGE.getValue();
	private final int explosion = HOMING_DAMAGE.getValue();
	private final int period = CHANGE_PERIOD.getValue();
	
	private final int eatRange = EATING_RANGE.getValue();
	private final int pierceRange = PIERCE_RANGE.getValue();
	private final double rapidRange = RAPID_RANGE.getValue();
	private final int homingRange = HOMING_RANGE.getValue();
	
	private final int pierceMana = MANA_PIERCE_SHOT.getValue();
	private final int healMana = MANA_HEALING.getValue();
	private final int rapidMana = MANA_RAPID.getValue();
	private final int homingMana = MANA_HOMING.getValue();
	
	private final AbilityTimer manaUpdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§b마나§7: §c" + mana + " §7| " + skillName, BarColor.WHITE, BarStyle.SEGMENTED_10);
    		bossBar.setProgress(mana * 0.01);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		if (getPlayer().getHealth() <= (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.25)) isLowHealth = true;
    		else isLowHealth = false;
    		
    		if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), rapidRange, rapidRange, predicate).size() > 0) isClose = true;
    		else isClose = false;
    		
    		if (target != null) {
    			if (!LocationUtil.isInCircle(getPlayer().getLocation(), target.getLocation(), homingRange)) {
    				if (predicate.test(target)) isFar = true;
    				else isFar = false;
    			} else isFar = false;
    		} else isFar = false;
    		
    		if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), pierceRange, pierceRange, predicate).size() > 0) isAttackable = true;
    		else isAttackable = false;
    		
    		if (count % (period * 20) == 0) {
    			change();
    		}
    		
    		if (shotDelay == 0) {
        		if (mana >= requireMana) {
        			shot();
        		}	
    		} else shotDelay = Math.max(0, shotDelay - 1);
    		
    		if (!getParticipant().attributes().TARGETABLE.getValue() && type != 0) getParticipant().attributes().TARGETABLE.setValue(true); 
    		
    		if (count % 4 == 0) {
    			manaGain(1);
    		}
    		
    		ac.update("§a다음 변경§f: " + (int) (10 - (count % (period * 20) / 20)) + "초");
    		bossBar.setProgress(mana * 0.01);
			bossBar.setTitle("§b마나§7: §c" + mana + " §7| " + skillName);
    	}
    	
		@Override
		public void onEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true); 
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true); 
			bossBar.removeAll();
		}
		
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
	public void change() {
		if (isLowHealth) {
			type = 1;
			bossBar.setColor(BarColor.GREEN);
			skillName = "§a회복";
			requireMana = healMana;
		} else if (isClose) {
			type = 2;
			bossBar.setColor(BarColor.BLUE);
			skillName = "§b속사";
			requireMana = rapidMana;
		} else if (isFar) {
			type = 3;
			bossBar.setColor(BarColor.PURPLE);
			skillName = "§d유도";
			requireMana = homingMana;
		} else if (isAttackable) {
			type = 4;
			bossBar.setColor(BarColor.YELLOW);
			skillName = "§e관통";
			requireMana = pierceMana;
		} else {
			type = 0;
			bossBar.setColor(BarColor.WHITE);
			skillName = "§8은신";
			requireMana = 0;
		}
		SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));
	}
	
	public void shot() {
		manaUse(requireMana);
		switch(type) {
		case 0:
			PotionEffects.SPEED.addPotionEffect(getPlayer(), 100, 0, false);
			getParticipant().attributes().TARGETABLE.setValue(false);
			Location myLoc = getPlayer().getLocation().clone();
			myLoc.add(0, 1, 0);
			Vector myDirection = myLoc.toVector().subtract(location.toVector()).normalize();
			forward = myDirection.clone().normalize();
            left = VectorUtil.rotateAroundAxisY(forward.clone(), 90);
            right = VectorUtil.rotateAroundAxisY(forward.clone(), -90);
            forward.multiply(.3);
            currentForward = location.clone();
            current = location.clone();
			for (int i = 0; current.distanceSquared(myLoc) > 1 && i < 500; i++) {
				for (int j = 0; j < 2; j++) {
					if (current.distanceSquared(myLoc) < 2) forward = myLoc.toVector().subtract(current.toVector()).normalize().multiply(0.5);
					currentForward.add(forward);
					side = !side;
					final Location loctarget = currentForward.clone().add((side ? left : right).clone().multiply(.1 + Math.random() * (1 - .1)));
					for (final Iterator<Location> iterator = Line.iteratorBetween(current, loctarget, 15); iterator.hasNext();) {
						Location next = iterator.next();
						ParticleLib.REDSTONE.spawnParticle(next, RGB.WHITE);
					}
					current = loctarget;
				}
			}
			break;
		case 1:
			SuperRegen.apply(getParticipant(), TimeUnit.TICKS, 100);
			if (!location.equals(getPlayer().getLocation())) {
				for (Location loc : Line.between(location, getPlayer().getLocation().clone().add(0, 1, 0), 15).toLocations(location)) {
					ParticleLib.REDSTONE.spawnParticle(loc, RGB.LIME);
				}	
			}
			SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 1, 1.5f);
			ParticleLib.HEART.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 1);
			break;
		case 2:
			Player rapidTarget = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
			if (rapidTarget != null) {
				if (LocationUtil.isInCircle(getPlayer().getLocation(), rapidTarget.getLocation(), rapidRange)) {
					new RapidBullet(getPlayer(), location, rapidTarget.getLocation().clone().add(0, 1, 0).toVector().subtract(location.toVector())).start();
					SoundLib.BLOCK_WOODEN_DOOR_CLOSE.playSound(location, 1, 2);
					rapidcount++;
				} else manaGain(requireMana);
			} else manaGain(requireMana);
			break;
		case 3:
			if (target != null) {
				if (!LocationUtil.isInCircle(getPlayer().getLocation(), target.getLocation(), homingRange)) {
					new Missile(getPlayer(), getPlayer().getLocation(), getPlayer().getLocation().getDirection(), 1).start();
					SoundLib.ENTITY_SHULKER_SHOOT.playSound(location, 2f, 0.75f);
				} else manaGain(requireMana);
			} else manaGain(requireMana);
			break;
		case 4:
			Player attackTarget = getFarthestEntity(getPlayer().getLocation(), predicate);
			if (attackTarget != null) {
				Location targetLoc = attackTarget.getLocation().clone();
				targetLoc.add(0, 1, 0);
				Vector direction = targetLoc.toVector().subtract(location.toVector()).normalize();
				forward = direction.clone().normalize();
	            left = VectorUtil.rotateAroundAxisY(forward.clone(), 90);
	            right = VectorUtil.rotateAroundAxisY(forward.clone(), -90);
	            forward.multiply(.3);
	            currentForward = location.clone();
	            current = location.clone();
				for (int i = 0; current.distanceSquared(targetLoc) > 1 && i < 1500; i++) {
					for (int j = 0; j < 2; j++) {
						if (current.distanceSquared(targetLoc) < 2) forward = targetLoc.toVector().subtract(current.toVector()).normalize().multiply(0.5);
						currentForward.add(forward);
						side = !side;
						final Location loctarget = currentForward.clone().add((side ? left : right).clone().multiply(.1 + Math.random() * (1 - .1)));
						for (final Iterator<Location> iterator = Line.iteratorBetween(current, loctarget, 15); iterator.hasNext();) {
							Location next = iterator.next();
							ParticleLib.REDSTONE.spawnParticle(next, RGB.YELLOW);
							for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, next, 1.25, 1.25, predicate)) {
								Damages.damageFixed(damageable, getPlayer(), (float) damage);
								PotionEffects.BLINDNESS.addPotionEffect((LivingEntity) damageable, 20, 1, true);
								if (damageable instanceof Player) Stun.apply(getGame().getParticipant((Player) damageable), TimeUnit.TICKS, 5);
							}
						}
						current = loctarget;
					}
				}
				SoundLib.ENTITY_WITHER_SKELETON_HURT.playSound(location, 1.5f, 2);
			} else manaGain(requireMana);
			break;
		}
	}
	
	private final AbilityTimer crystalball = new AbilityTimer() {
		
		private double y = 1;
		private boolean yUp = false;
		
		@Override
		public void onStart() {
			location = getPlayer().getLocation();
		}
		
		@Override
		public void run(int count) {
			double angle = Math.toRadians(count * 5);
			double x = Math.cos(angle);
			double z = Math.sin(angle);
				
			if (y >= 1.6) yUp = false;
			else if (y <= 0.4) yUp = true;
				
			y = yUp ? Math.min(1.6, y + 0.01) : Math.max(0.4, y - 0.01);
				
			location = getPlayer().getLocation().clone().add(x, y, z);   	
			ParticleLib.SPELL_WITCH.spawnParticle(location, 0, 0, 0, 1, 0);
			ParticleLib.REDSTONE.spawnParticle(location, RGB.PURPLE);
		}
		
		@Override
		public void onEnd() {
		}
		
		@Override
		public void onSilentEnd() {
			onEnd();
		}
		
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
	public void manaUse(int value) {
		mana = Math.max(0, mana - value);
		switch(type) {
		case 0:
			shotDelay =+ 19;
			break;
		case 1:
			shotDelay =+ 29;
			break;
		case 2:
			if (rapidcount >= 10) {
				shotDelay =+ 79;
				rapidcount = 0;
			} else shotDelay =+ 1;
			break;
		case 3:
			shotDelay =+ 129;
			break;
		case 4:
			shotDelay =+ 59;
			break;
		}
	}
	
	public void manaGain(int value) {
		mana = Math.min(100, mana + value);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager())) {
			if (e.getEntity() instanceof Player) {
				target = (Player) e.getEntity();
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (!e.getEntity().equals(getPlayer())) {
			if (LocationUtil.isInCircle(getPlayer().getLocation(), e.getEntity().getLocation(), eatRange)) {
				if (predicate.test(e.getEntity())) {
					if ((int) (e.getAmount() * 10) - (e.getAmount() * 10) < 0) {
						addtionalMana =+ (e.getAmount() * 10);
					} else {
						manaGain((int) (e.getAmount() * 10));
						addtionalMana =+ (int) (e.getAmount() * 10) - (e.getAmount() * 10);
					}
					if (addtionalMana > 1) {
						manaGain(1);
						addtionalMana = Math.max(0, addtionalMana - 1);
					}
				}
			}
		}
	}
	
	private Player getFarthestEntity(Location center, Predicate<Entity> predicate) {
		double distance = Double.MIN_VALUE;
		Player current = null;

		Location centerLocation = center.clone();
		if (center.getWorld() == null) return null;
		for (Player p : LocationUtil.getNearbyEntities(Player.class, center, pierceRange, pierceRange, predicate)) {
			double compare = centerLocation.distanceSquared(p.getLocation());
			if (compare > distance && (predicate == null || predicate.test(p))) {
				distance = compare;
				current = p;
			}
		}

		return current;
	}
	
	public class RapidBullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;

		private Location lastLocation;

		private RapidBullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity) {
			super(5);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new RapidBullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.forward = arrowVelocity.multiply(3.5);
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

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
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
					stop(true);
					return;
				}
				for (Player player : LocationUtil.getConflictingEntities(Player.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(player)) {
						if (Damages.canDamage(player, DamageCause.PROJECTILE, 0.211)) {
							Damages.damageArrow(player, shooter, (float) 0.000001);
							Healths.setHealth(player, player.getHealth() - 0.211);
							player.setNoDamageTicks(1);
							player.setVelocity(VectorUtil.validateVector(getPlayer().getLocation().toVector().subtract(player.getLocation().toVector()).normalize().setY(0).multiply(0.15)));
						}
						stop(true);
						return;
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, RGB.of(1, 254, 254));
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
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
				final Player deflectedPlayer = deflector.getPlayer();
				new RapidBullet(deflectedPlayer, lastLocation, newDirection).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
				RapidBullet.this.stop(false);
			}

		}

	}
	
	public class Missile extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private Vector forward;
		private final Predicate<Entity> predicate;

		private ArmorStand armorstand;
		private boolean changed = false;
		
		private Location lastLocation;
		
		private Missile(LivingEntity shooter, Location startLocation, Vector arrowVelocity, double angle) {
			super(200);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
			this.forward = arrowVelocity.setY(arrowVelocity.getY() * 0.7);
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
		protected void onStart() {
			if (getPlayer().getName().equals("Phillip_MS")) changed = true;
			else if (random.nextInt(10) < 2) changed = true;
			
			if (changed) {
				armorstand = lastLocation.getWorld().spawn(lastLocation, ArmorStand.class);
				armorstand.setVisible(false);
				armorstand.setInvulnerable(true);
				armorstand.setSmall(true);
				NMS.removeBoundingBox(armorstand);
				ItemStack phillip_head = Skulls.createSkull("Phillip_MS");
				
				EntityEquipment equipment = armorstand.getEquipment();
				
				equipment.setHelmet(phillip_head);
			}
		}
		
		@Override
		protected void run(int i) {
			if (target != null) {
				this.forward = target.getLocation().add(0, 1, 0).clone().subtract(lastLocation.clone()).toVector().normalize().multiply(1.1);
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
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (i < 180) {
					if (type.isSolid()) {
						SoundLib.ENTITY_GENERIC_EXPLODE.playSound(entity.getLocation(), 1f, 1f);
						for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, entity.getLocation(), 5, 5, predicate)) {
							livingEntity.setNoDamageTicks(0);
							Damages.damageExplosion(livingEntity, getPlayer(), explosion);
						}
						stop(true);
						return;
					}
					entity.setLocation(location);
					for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
						if (damageable.isValid() && !damageable.isDead() && !shooter.equals(damageable)) {
							SoundLib.ENTITY_GENERIC_EXPLODE.playSound(entity.getLocation(), 1f, 1f);
							for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, entity.getLocation(), 5, 5, predicate)) {
								livingEntity.setNoDamageTicks(0);
								Damages.damageExplosion(livingEntity, getPlayer(), explosion);
							}
							stop(false);
							return;
						}
					}	
				}
				if (!changed) {
					ParticleLib.REDSTONE.spawnParticle(location, RGB.PURPLE);
					ParticleLib.CRIT.spawnParticle(location, 0, 0, 0, 1, 0);	
				} else {
					armorstand.teleport(location.clone().add(0, -1, 0));
					armorstand.getLocation().setDirection(forward);
				}
				ParticleLib.FLAME.spawnParticle(location, 0, 0, 0, 1, 0.15f);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			if (changed) armorstand.remove();
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			if (changed) armorstand.remove();
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
				new Missile(deflectedPlayer, lastLocation, newDirection, getPlayer().getLocation().getDirection().getY() * 90).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
				Missile.this.stop(false);
			}

		}

	}

}