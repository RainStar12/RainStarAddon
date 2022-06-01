package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import RainStarEffect.Petrification;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "메두사", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"적이 나를 $[LOOK_COUNT]초간 쳐다보면, 대상은 $[PETRIFICATION_DURATION]초간 §8석화§f됩니다. $[COOLDOWN]",
		"§0[§8석화§0]§f 이동할 수 없습니다. 피해를 99% 경감하여 받습니다.",
		" 7번째 피해를 받으면 3배의 피해를 입고 석화가 해제됩니다.",
		" 웅크리기를 연타하는 것으로 저항하여 석화 지속시간을 줄일 수 있습니다.",
		"§b[§7아이디어 제공자§b] §eYeow_ool §7/ §b_Daybreak_"
		})
public class Medusa extends AbilityBase {
	
	public Medusa(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> COOLDOWN = 
			abilitySettings.new SettingObject<Double>(Medusa.class, "cooldown", 20.0,
            "# 유닛별 쿨타임", "# 단위: 초", "# 석화 걸기에 성공 시 바로 적용됩니다.") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
		@Override
		public String toString() {
			return "§c유닛별 쿨타임 §7: §f" + getValue();
        }
    };
	
	public static final SettingObject<Double> PETRIFICATION_DURATION = 
			abilitySettings.new SettingObject<Double>(Medusa.class, "petrification-duration", 7.5,
            "# 바라볼 수 있는 시간 최대치", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Double> LOOK_COUNT = 
			abilitySettings.new SettingObject<Double>(Medusa.class, "look-count", 3.0,
            "# 바라볼 수 있는 시간 최대치", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> RANGE = 
			abilitySettings.new SettingObject<Integer>(Medusa.class, "range", 15,
            "# 인식 가능 거리", "# 단위: 칸") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
    @Override
    public void onUpdate(Update update) {
    	if (update == Update.RESTRICTION_CLEAR) {
    		passive.start();
    	}
    }
    
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
	
	private final int duration = (int) (PETRIFICATION_DURATION.getValue() * 20);
	private final int lookcount = (int) (LOOK_COUNT.getValue() * 20);
	private Map<Player, LookTimer> looktimers = new HashMap<>();
	private final int range = RANGE.getValue();
	private final Map<UUID, Long> lastPetri = new HashMap<>();
	private final int unitCooldown = (int) ((COOLDOWN.getValue() * 1000) * Wreck.calculateDecreasedAmount(25));
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			for (Participant p : getGame().getParticipants()) {
				Player player = p.getPlayer();
				if (LocationUtil.getEntityLookingAt(Player.class, player, range, predicate).equals(getPlayer())) {
					final long current = System.currentTimeMillis();
					if (current - lastPetri.getOrDefault(player.getUniqueId(), 0L) >= unitCooldown && !looktimers.containsKey(player)) {
						new LookTimer(player).start();
					}
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public class LookTimer extends AbilityTimer {
		
		private final Player player;
		private final ActionbarChannel ac;
		private final DecimalFormat df = new DecimalFormat("0.0");
		
		public LookTimer(Player player) {
			super(lookcount);
			setPeriod(TimeUnit.TICKS, 1);
			looktimers.put(player, this);
			this.player = player;
			ac = getGame().getParticipant(player).actionbar().newChannel();
		}
		
		@Override
		public void run(int count) {
			if (!LocationUtil.getEntityLookingAt(Player.class, player, range, predicate).equals(getPlayer())) this.stop(true);
			else ac.update("§c" + df.format(count));
		}
		
		@Override
		public void onEnd() {
			Petrification.apply(getGame().getParticipant(player), TimeUnit.TICKS, duration);
			final long current = System.currentTimeMillis();
			lastPetri.put(player.getUniqueId(), current);
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.unregister();
			looktimers.remove(player);
		}
		
	}
	
}
