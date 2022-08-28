package RainStarAbility;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.event.ParticipantEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "왕", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c위압§f: $[RANGE]칸 내의 플레이어를 매우 느리게 만듭니다.",
		" 범위 내 대상은 공격력이 $[DECREASE]% 감소합니다.",
		" 범위 내에서 §a액티브 스킬§f 사용 시, §c쿨타임§f을 제외한 §6지속 시간§f이 $[TIMER_DECREASE]% 감소합니다.",
		"§7패시브 §8- §b위풍당당!§f: 위풍당당한 걸음 탓에 기본 이동 속도가 느립니다.",
		" 그 대신 §3이동계 상태이상§f에 면역을 가지고, §b원거리 공격자§f를 내게 끌어옵니다."
		},
		summarize = {
		"주변 플레이어를 항시 느리게 만들고 공격력을 감소시킵니다.",
		"범위 내에서 누군가가 §a액티브 스킬§f을 사용하면 §6지속 시간§f이 감소합니다.",
		"기본 이동 속도가 느리지만, §3이동계 상태이상§f에 면역이며 §b원거리 공격자§f를 내게 끌어옵니다."
		})
public class King extends AbilityBase {

	public King(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> TIMER_DECREASE = 
			abilitySettings.new SettingObject<Integer>(King.class, "timer-decrease", 25,
            "# 지속 시간 감소율", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(King.class, "range", 5.0,
            "# 위압감 범위", "# 단위: 칸") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DECREASE = 
			abilitySettings.new SettingObject<Integer>(King.class, "damage-decrease", 15,
            "# 공격력 감소율", "# 단위: %") {
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
    
	private final double damageDecrease = 1 - (DECREASE.getValue() * 0.01);
	private final double decrease = 1 - (TIMER_DECREASE.getValue() * 0.01);
    private final double range = RANGE.getValue();
    private final Circle circle = Circle.of(range, (int) (range * 12));
    private final List<RGB> gradations = Gradient.createGradient(10, RGB.of(227, 1, 1), RGB.BLACK);
	private int stack = 0;
    
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			timer.start();
		}
	}
    
    private AbilityTimer timer = new AbilityTimer() {
	
    	@Override
    	public void run(int count) {
    		if (count % 2 == 0) {
        		if (stack > 0) {
        			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
    					loc.add(0, (stack * 0.1), 0);
        				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, gradations.get(stack - 1));
    				}
        		} else if (stack > -10) {
        			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
        				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, gradations.get(0));
    				}
        		} else stack = 11;
        		stack--;
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    @SubscribeEvent
    public void onEffectApply(ParticipantEffectApplyEvent e) {
    	if (e.getParticipant().equals(getParticipant())) {
    		final ImmutableSet<EffectType> effectType = e.getEffectType().getEffectType();
			if (effectType.contains(EffectType.MOVEMENT_RESTRICTION) || effectType.contains(EffectType.MOVEMENT_INTERRUPT)) {
				e.setCancelled(true);
			}
    	}
    }
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager != null && !getPlayer().equals(damager) && predicate.test(damager)) {
			if (LocationUtil.isInCircle(getPlayer().getLocation(), damager.getLocation(), range)) e.setDamage(e.getDamage() * damageDecrease);
			else if (getPlayer().equals(e.getEntity())) damager.setVelocity(VectorUtil.validateVector(getPlayer().getLocation().toVector().subtract(damager.getLocation().toVector()).normalize().setY(0).multiply(2)));
		}
    }
    
	@SuppressWarnings("deprecation")
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		final double fromY = e.getFrom().getY(), toY = e.getTo().getY();
		if (toY > fromY) {
			double dx, dy, dz;
			final Location from = e.getFrom(), to = e.getTo();
			dx = to.getX() - from.getX();
			dy = to.getY() - from.getY();
			dz = to.getZ() - from.getZ();
			if (LocationUtil.isInCircle(getPlayer().getLocation(), e.getPlayer().getLocation(), range) && predicate.test(e.getPlayer()))
				e.getPlayer().setVelocity(new Vector((dx * 0.7), (dy * 0.1), (dz * 0.7)));	
		} else if (e.getPlayer().isOnGround()) {
			if (e.getPlayer().equals(getPlayer())) {
				e.getPlayer().setVelocity(e.getPlayer().getVelocity().multiply(0.925));
				ParticleLib.DRIP_LAVA.spawnParticle(getPlayer().getLocation(), 0.15, 0, 0.15, 1, 1);
			} else if (LocationUtil.isInCircle(getPlayer().getLocation(), e.getPlayer().getLocation(), range) && predicate.test(e.getPlayer()))
				e.getPlayer().setVelocity(e.getPlayer().getVelocity().multiply(0.7));	
		}
	}
	
	@SubscribeEvent
	public void onActiveSkill(AbilityActiveSkillEvent e) {
		if (LocationUtil.isInCircle(getPlayer().getLocation(), e.getPlayer().getLocation(), range) && predicate.test(e.getPlayer())) {
			for (GameTimer t : e.getAbility().getTimers()) {
				if (!(t instanceof Cooldown.CooldownTimer)) t.setCount((int) (t.getCount() * decrease));
			}
			SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(e.getPlayer().getLocation(), 1, 1);
			ParticleLib.EXPLOSION_NORMAL.spawnParticle(e.getPlayer().getLocation(), 0.7, 0.7, 0.7, 40, 0);
		}
	}
	
}