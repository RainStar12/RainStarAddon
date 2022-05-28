package RainStarAbility;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "왕", rank = Rank.S, species = Species.HUMAN, explain = {
		"§e왕§f의 §c위압감§f으로 $[RANGE]칸 내의 플레이어를 매우 느리게 만듭니다.",
		"영역 내에서 §a액티브 스킬§f 사용 시, §c쿨타임§f을 제외한 §6지속 시간§f이 $[TIMER_DECREASE]% 감소합니다.",
		"또한 대상은 $[DURATION]초간 나약함 $[AMPLIFIER]을 받습니다.",
		"위풍당당한 걸음 탓에 기본 이동 속도가 느립니다."
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
			abilitySettings.new SettingObject<Double>(King.class, "duration", 5.0,
            "# 위압감 범위", "# 단위: 칸") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Double> DURATION = 
			abilitySettings.new SettingObject<Double>(King.class, "duration", 3.0,
            "# 나약함 지속 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> AMPLIFIER = 
			abilitySettings.new SettingObject<Integer>(King.class, "amplifier", 0,
            "# 나약함 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
		@Override
		public String toString() {
			return "" + 1 + getValue();
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
    
	private final double decrease = 1 - (TIMER_DECREASE.getValue() * 0.01);
    private final double range = RANGE.getValue();
    private final int amplifier = AMPLIFIER.getValue();
    private final int duration = (int) (DURATION.getValue() * 20);
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
	public void onPlayerMove(PlayerMoveEvent e) {
		final double fromY = e.getFrom().getY(), toY = e.getTo().getY();
		if (toY > fromY) {
			double dx, dy, dz;
			final Location from = e.getFrom(), to = e.getTo();
			dx = to.getX() - from.getX();
			dy = to.getY() - from.getY();
			dz = to.getZ() - from.getZ();
			if (e.getPlayer().equals(getPlayer())) {
				e.getPlayer().setVelocity(new Vector((dx * 0.85), dy, (dz * 0.85)));
				ParticleLib.VILLAGER_ANGRY.spawnParticle(getPlayer().getLocation(), 0.1, 0, 0.1, 1, 1);
			} else if (LocationUtil.isInCircle(getPlayer().getLocation(), e.getPlayer().getLocation(), range) && predicate.test(e.getPlayer()))
				e.getPlayer().setVelocity(new Vector((dx * 0.6), (dy * 0.1), (dz * 0.6)));	
		} else if (e.getPlayer().isOnGround()) {
			if (e.getPlayer().equals(getPlayer())) {
				e.getPlayer().setVelocity(e.getPlayer().getVelocity().multiply(0.85));
				ParticleLib.VILLAGER_ANGRY.spawnParticle(getPlayer().getLocation(), 0.1, 0, 0.1, 1, 1);
			} else if (LocationUtil.isInCircle(getPlayer().getLocation(), e.getPlayer().getLocation(), range) && predicate.test(e.getPlayer()))
				e.getPlayer().setVelocity(e.getPlayer().getVelocity().multiply(0.6));	
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
			PotionEffects.WEAKNESS.addPotionEffect(getPlayer(), duration, amplifier, true);
		}
	}
	
}