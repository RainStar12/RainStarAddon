package rainstar.abilitywar.ability;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import rainstar.abilitywar.effect.Irreparable;

@AbilityManifest(name = "러시안 룰렛", rank = Rank.S, species = Species.HUMAN, explain = {
		"탄이 비었을 때 철괴 우클릭 시, 실탄 하나를 넣고 실린더를 돌립니다. $[COOLDOWN]",
		"이후 철괴 우클릭으로 한 발씩 발사 가능합니다. 실탄을 발사할 경우,",
		"맞힌 대상에게 §c회복 불능§f $[HEAL_BAN]초와 §b트루 대미지§f $[TRUE_DMG]%를 입힙니다.",
		"다만 빗맞힐 경우에는 자신이 $[STUN]초간 §e기절§f합니다."
		},
		summarize = {
		"실린더가 빈 상태에서 §7철괴 우클릭으로§f 10칸 중 한 곳에 실탄을 장전합니다.",
		"이후 §7철괴 우클릭으로§f 한 발씩 발사하고, 실탄 발사 시",
		"맞힌 대상에게 §c회복 불능§f $[HEAL_BAN]초와 §b트루 대미지§f $[TRUE_DMG]%를 입힙니다.",
		"다만 빗맞힐 경우에는 자신이 $[STUN]초간 §e기절§f합니다."
		})
public class RussianRoulette extends AbilityBase implements ActiveHandler {
	
	public RussianRoulette(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(RussianRoulette.class, "cooldown", 66,
            "# 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
	
	public static final SettingObject<Double> HEAL_BAN = 
			abilitySettings.new SettingObject<Double>(RussianRoulette.class, "heal-ban-duration", 15.0,
            "# 회복 불능 지속 시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> STUN = 
			abilitySettings.new SettingObject<Double>(RussianRoulette.class, "stun", 5.0,
            "# 기절 지속 시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> TRUE_DMG = 
			abilitySettings.new SettingObject<Integer>(RussianRoulette.class, "projectile-true-damage", 33,
            "# 대상에게 입힐 트루 대미지", "# 단위: 최대 체력의 %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
    private ActionbarChannel ac = newActionbarChannel();
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private int cylinder = 0;
    private Random random = new Random();
    private int truecylinder = 0;
    private Bullet bullet = null;
    private final int healban = (int) (HEAL_BAN.getValue() * 20);
    private final int stun = (int) (STUN.getValue() * 20);
    private final int damage = TRUE_DMG.getValue();
    
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update(Strings.repeat("§b●", cylinder) + Strings.repeat("§b○", 10 - cylinder));
		}
	}
    
    private final AbilityTimer roulette = new AbilityTimer(TaskType.REVERSE, 10) {
    	
    	@Override
    	public void onStart() {
    		truecylinder = random.nextInt(10) + 1;
    	}
    	
    	@Override
    	public void run(int count) {
    		SoundLib.BLOCK_IRON_DOOR_CLOSE.playSound(getPlayer(), 1, 1.7f);
    		if (truecylinder == count) ac.update(Strings.repeat("§b○", Math.max(0, count - 1)) + "§d●" + Strings.repeat("§b○", Math.max(0, 10 - count)));
    		else ac.update(Strings.repeat("§b○", Math.max(0, count - 1)) + "§b●" + Strings.repeat("§b○", Math.max(0, 10 - count)));
    	}
    	
    	@Override
    	public void onEnd() {
    		cylinder = 10;
    		ac.update(Strings.repeat("§b●", cylinder) + Strings.repeat("§b○", 10 - cylinder));
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
    
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (cylinder == 0 && !cooldown.isCooldown() && !roulette.isRunning()) {
				return roulette.start();
			}
			if (cylinder >= 1 && bullet == null) {
	    		if (truecylinder == cylinder) {
	    			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 7, 1.75f);
	    			new Bullet(getPlayer(), getPlayer().getEyeLocation(), getPlayer().getLocation().getDirection(), damage, RGB.of(176, 254, 249)).start();
	    		} else SoundLib.BLOCK_STONE_BUTTON_CLICK_OFF.playSound(getPlayer().getLocation(), 7, 1.8f);
	    		cylinder--;
	    		ac.update(Strings.repeat("§b●", cylinder) + Strings.repeat("§b○", 10 - cylinder));
	    		if (cylinder == 0) cooldown.start();
			}
		}
		return false;
	}
	
	public class Bullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final double damage;
		private final Predicate<Entity> predicate;
		private boolean checkhit = false;
		private final RGB color;
		private Location lastLocation;
		private Set<LivingEntity> hits = new HashSet<>();
		
		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, double damage, RGB color) {
			super(20);
			setPeriod(TimeUnit.TICKS, 1);
			RussianRoulette.this.bullet = this;
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-1.25, -1.25, -1.25, 1.25, 1.25, 1.25);
			this.forward = arrowVelocity.multiply(3.75);
			this.damage = damage;
			this.color = color;
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(shooter)) return false;
					if (hits.contains(entity)) return false;
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
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (type.isSolid()) {
					stop(false);
					return;
				}
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						checkhit = true;
						double maxHP = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        if (Damages.canDamage(livingEntity, getPlayer(), DamageCause.PROJECTILE, (maxHP * damage * 0.01))) {
                            Damages.damageArrow(livingEntity, shooter, 1);
                            hits.add(livingEntity);
                            if (livingEntity instanceof Player) {
                            	Player p = (Player) livingEntity;
                            	Healths.setHealth(p, Math.max(1, p.getHealth() - (maxHP * damage * 0.01)));
                            } else livingEntity.setHealth(Math.max(1, livingEntity.getHealth() - (maxHP * damage * 0.01)));
						    SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer(), 1f, 1.2f);
                        }
                        if (livingEntity instanceof Player) {
                            Player p = (Player) livingEntity;
                            Irreparable.apply(getGame().getParticipant(p), TimeUnit.TICKS, healban);
                        }
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, color);
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			if (!checkhit) Stun.apply(getGame().getParticipant(getPlayer()), TimeUnit.TICKS, stun);
			hits.clear();
			bullet = null;
			entity.remove();
			RussianRoulette.this.bullet = null;
		}

		@Override
		protected void onSilentEnd() {
			if (!checkhit) Stun.apply(getGame().getParticipant(getPlayer()), TimeUnit.TICKS, stun);
			hits.clear();
			bullet = null;
			entity.remove();
			RussianRoulette.this.bullet = null;
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
				new Bullet(deflectedPlayer, lastLocation, newDirection, damage, color).start();
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
