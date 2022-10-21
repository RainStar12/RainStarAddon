package RainStarAbility;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.util.Vector;

import RainStarEffect.Burn;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.math.geometry.vector.VectorIterator;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "인페르노", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"§c불 속성§f의 화염 검사, 인페르노.",
		"§7패시브 §8- §c업화의 주인§f: 화염이 붙은 적을 타격하면 §4불꽃§f을 획득해",
		" §4불꽃§f이 자신의 화염 피해를 대체하고, 하나당 화염 피해를 10% 더 입습니다.",
		"§7검 공격 §8- §c열화폭참§f: 자신이 타격한 대상을 2초간 추가 발화시킵니다.",
		" 대상이 이미 5초 이상 발화 도중이면 대신 대상에게 추가 피해를 입힙니다.",
		" §7추가 피해량§f: §e(§7(§c대상이 가진 화염 지속시간§e + §4불꽃§e§7) × §b0.2§e)",
		"§7철괴 우클릭 §8- §c화력전개§f: 나와 $[RANGE]칸 이내의 모든 플레이어를 $[DURATION]초간 추가 발화시키고,",
		" 나를 포함한 모든 대상에게 §4화상§f 상태이상을 겁니다. $[COOLDOWN]",
		" §4화상§f이 지속하는 동안, 자신은 화염계 피해를 절반만큼 역으로 회복합니다.",
		"§7상태이상 §8- §4화상§f: 모든 화염 계열 피해를 무시할 수 없으며 2.5배로 입습니다.",
		" 화염이 꺼질 때 꺼지기 전의 화염 지속시간에 비례해 피해를 입습니다."},
		summarize = {
		"§7근접 공격 시§f 5초 이하 발화중 대상에게 2초간 추가 §c발화§f시킵니다.",
		"5초 이상 §c발화§f중인 대상에게는 추가 피해를 입힐 수 있습니다.",
		"기본적으로 화염계 피해를 무시하지만 §c발화 중인 대상§f을 타격하면 §c발화§f합니다.",
		"§7철괴 우클릭 시§f 나와 주변 대상들을 $[DURATION]초 추가 발화시키며, §4화상§f을 겁니다.",
		"§4화상§f에 걸린 적은 화염계 피해를 무조건 받으며 2.5배로 입습니다.",
		"본인은 화상 지속시간 동안 화염계 피해를 역회복합니다."
		})

public class Inferno extends AbilityBase implements ActiveHandler {

	public Inferno(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Inferno.class, "cooldown", 80,
            "# 쿨타임") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
	public static final SettingObject<Integer> RANGE = 
			abilitySettings.new SettingObject<Integer>(Inferno.class, "range", 5,
            "# 사거리") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DURATION = 
			abilitySettings.new SettingObject<Integer>(Inferno.class, "duration", 10,
            "# 지속시간") {
        @Override
        public boolean condition(Integer value) {
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
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private final int range = RANGE.getValue();
	private final int duration = DURATION.getValue();
	private int burningflame = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private final Crescent crescent1 = Crescent.of(2.2, 55);
	private final Crescent crescent2 = Crescent.of(2.2, 10);
	private int particleSide = 15;
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	passive.start();
	    } 
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		if (getPlayer().getFireTicks() >= 0) {
    			if (burningflame <= 0) {
    				getPlayer().setFireTicks(0);
    			}
    		}
    		if (getPlayer().getFireTicks() % 20 == 0) {
    			if (getPlayer().getFireTicks() != burningflame * 20) {
    				getPlayer().setFireTicks(burningflame * 20);
    			}
    		}
    		if (count % 20 == 0) {
        		flameSet(-1);
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	private final AbilityTimer circle = new AbilityTimer(TaskType.NORMAL, 50) {
		
		private VectorIterator iterator;
		private Location center;
		
    	@Override
		public void onStart() {
    		this.iterator = Circle.infiniteIteratorOf(range, (range * 5));
			center = getPlayer().getLocation();
			SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(center, 1, 1.3f);
    	}
		
    	@Override
		public void run(int i) {
			for (int j = 0; j < 5; j++) {
				Location loc = center.clone().add(iterator.next());
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), getPlayer().getLocation().getY(), loc.getBlockX(), loc.getBlockZ()) + (i * 0.1));
				ParticleLib.FLAME.spawnParticle(loc, 0, 0, 0, 1, 0);
			}
			ParticleLib.FLAME.spawnParticle(center);
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
    
	public void flameSet(int value) {
		if (value <= 0) {
			burningflame = Math.max(0, burningflame + value);
		} else {
			if (burningflame < 10) {
				burningflame = Math.min(10, burningflame + value);	
			}
		}
		ac.update("§c♨ §e" + burningflame);
	}
	
	public void flameOverSet(int value) {
		burningflame = burningflame + value;
		ac.update("§c♨ §e" + burningflame);
	}
	
    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
    	if (e.getEntity().equals(getPlayer())) {
    		if (e.getCause() == DamageCause.FIRE || e.getCause() == DamageCause.FIRE_TICK ||
					e.getCause() == DamageCause.LAVA || e.getCause() == DamageCause.HOT_FLOOR) {
    			if (burningflame >= 1) {
    				e.setDamage(e.getDamage() + (e.getDamage() * (burningflame * 0.1)));
    				if (getParticipant().hasEffect(Burn.registration)) {
    					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), e.getDamage() * 0.5, RegainReason.CUSTOM);
    					Bukkit.getPluginManager().callEvent(event);
    					if (!event.isCancelled()) {
    						Healths.setHealth(getPlayer(), getPlayer().getHealth() + (e.getDamage() * 0.5));
    					}
    					e.setDamage(0);
    				}
    			} else {
    				e.setCancelled(true);
    			}
    			if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
    				e.setCancelled(true);
    				getPlayer().setHealth(1);
    			}
    		}
    	}
    }
    
    @SubscribeEvent
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
    	onEntityDamage(e);
    }
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	onEntityDamage(e);
    	if (e.getDamager().equals(getPlayer())) {
    		if (e.getEntity().getFireTicks() > 0) {
    			flameSet((int) (e.getEntity().getFireTicks() * 0.0125));
    			new CutParticle(particleSide).start();
    			particleSide *= -1;
    		}
    		if (e.getEntity().getFireTicks() >= 100) {
    			e.setDamage(e.getDamage() + Math.min(10, (e.getEntity().getFireTicks() * 0.01)) + (burningflame * 0.2));
    		} else {
    			if (e.getEntity().getFireTicks() <= 0) {
        			e.getEntity().setFireTicks(e.getEntity().getFireTicks() + 60);
    			} else {
        			e.getEntity().setFireTicks(e.getEntity().getFireTicks() + 40);	
    			}
    		}
    	}
    }
    
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cool.isCooldown()) {
			for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
				Burn.apply(getGame().getParticipant(p), TimeUnit.SECONDS, duration);
				p.setFireTicks(p.getFireTicks() + (duration * 20));
			}
			Burn.apply(getParticipant(), TimeUnit.SECONDS, duration);
			flameOverSet(duration);
			if (circle.isRunning()) {
				circle.stop(false);
			}
			circle.start();
			return cool.start();
		}
		return false;
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors1;
		private final Vectors crescentVectors2;
		private RGB COLOR;
		private Random random = new Random();
		private int number;

		private CutParticle(double angle) {
			super(3);
			setPeriod(TimeUnit.TICKS, 1);
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(getPlayer().getLocation().getDirection().setY(0).normalize(), 90), getPlayer().getLocation().getDirection().setY(0).normalize(), angle);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors1 = crescent1.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -15);
			this.crescentVectors2 = crescent2.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -15);
		}

		@Override
		protected void onStart() {
			number = random.nextInt(3);
			SoundLib.ENTITY_GENERIC_EXTINGUISH_FIRE.playSound(getPlayer().getLocation(), 1, (float) (0.5 + ((random.nextInt(10) + 1) * 0.1)));
		}
		
		@Override
		protected void run(int count) {
			switch(count) {
			case 1:
				COLOR = RGB.of(254, 89, 1);
				break;
			case 2:
				COLOR = RGB.of(254, 14, 1);
				break;
			case 3:
				COLOR = RGB.of(152, 1, 1);
				break;
			}
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1.3, 0);
			for (Location loc : crescentVectors1.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
			}
			for (Location loc : crescentVectors2.toLocations(baseLoc)) {
				ParticleLib.FLAME.spawnParticle(loc, 0, 0, 0, 1, 0.05);
			}
			switch(number) {
			case 0:
				crescentVectors1.rotateAroundAxis(axis, 10);
				crescentVectors2.rotateAroundAxis(axis, 10);
				break;
			case 1:
				crescentVectors1.rotateAroundAxis(axis, 20);
				crescentVectors2.rotateAroundAxis(axis, 20);
				break;
			case 2:
				crescentVectors1.rotateAroundAxis(axis, 30);
				crescentVectors2.rotateAroundAxis(axis, 30);
				break;
			}
		}

	}
    
}