package rainstar.abilitywar.ability;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.LimitedPushingList;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.Moisture;

@AbilityManifest(name = "비구름", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7패시브 §8- §b비구름§f: 자신을 한 발짝 늦게 따라오는 §b구름§f이 $[RANGE]칸 내에 §3§l비§f를 내립니다.",
		" §3§l비§f에 맞은 적은 시간이 점점 쌓이는 상태이상 §3§n습기§f를 $[MOISTURE_DURATION]초 받습니다.",
		" 자신은 §3§n습기§f 효과를 지속시간 $[HEAL_PERCENTAGE]%의 회복 효과로 대신 받습니다.",
		"§7철괴 우클릭 §8- §2기후 조작§f: $[DURATION]초간 §b구름§f의 범위가 $[ADD_RANGE]칸 증가합니다.",
		" 지속시간동안 §b구름§f은 자신이 마지막으로 공격한 적을 추격하고, $[CHANCE]%의 확률로",
		" §8먹구름§f이 되어 매 $[LIGHTNING_DELAY]초마다 번개를 내리쳐 $[STUN]초간 §e§n기절§f시킵니다. $[COOLDOWN]",
		"§9[§3습기§9]§f 이동 속도가 25%, 공격력이 15% 감소합니다."
		},
		summarize = {
		""
		})

public class Raincloud extends AbilityBase {
	
	public Raincloud(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(Raincloud.class, "range", 2.0,
            "# 구름의 범위", "# 단위: 칸") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> MOISTURE_DURATION = 
			abilitySettings.new SettingObject<Double>(Raincloud.class, "moisture-duration", 0.1,
            "# 습기 지속시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> HEAL_PERCENTAGE = 
			abilitySettings.new SettingObject<Integer>(Raincloud.class, "heal-percentage", 25,
            "# 회복 치환율", "# 단위: %", "# 25%라면, 1초에 0.25의 체력을 회복합니다.") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };

	public static final SettingObject<Integer> DURATION = 
			abilitySettings.new SettingObject<Integer>(Raincloud.class, "duration", 15,
            "# 기후 조작 지속시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> ADD_RANGE = 
			abilitySettings.new SettingObject<Double>(Raincloud.class, "add-range", 1.5,
            "# 기후 조작 도중 구름 범위 증가량", "# 단위: 칸") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> CHANCE = 
			abilitySettings.new SettingObject<Integer>(Raincloud.class, "chance", 20,
            "# 먹구름 확률", "# 단위: %") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> LIGHTNING_DELAY = 
			abilitySettings.new SettingObject<Double>(Raincloud.class, "lightning-delay", 2.5,
            "# 먹구름의 번개 주기", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> STUN = 
			abilitySettings.new SettingObject<Double>(Raincloud.class, "stun", 0.8,
            "# 기절 지속시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Raincloud.class, "cooldown", 90,
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
    
    private final double range = RANGE.getValue();
    private final int moisturedur = (int) (MOISTURE_DURATION.getValue() * 20);
    private final double healpercent = HEAL_PERCENTAGE.getValue() * 0.01;
    private final int duration = DURATION.getValue() * 20;
    private final double addrange = ADD_RANGE.getValue();
    private final int chance = CHANCE.getValue();
    private final int lightningdelay = (int) (LIGHTNING_DELAY.getValue() * 20);
    private final int stun = (int) (STUN.getValue() * 20);
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    
    private final LimitedPushingList<Location> locations = new LimitedPushingList<>(60);
    private final Random random = new Random();
    private boolean isDark = false;
    private Location cloudlocation = null;
    private double nowrange = range;
    private Player target;
    
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return true;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
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
    
    private final AbilityTimer cloud = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
    		locations.add(skill.isRunning() ? target.getLocation() : getPlayer().getLocation());
        	cloudlocation = locations.getFirst().add(0, 3.5, 0);	
    		ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), nowrange, 0.2, nowrange, (int) (range * 150), 0);
    		
    		for (Player player : LocationUtil.getEntitiesInCircle(Player.class, cloudlocation, nowrange, predicate)) {
    			if (player.getLocation().getY() <= cloudlocation.getY()) {
    				Moisture.apply(getGame().getParticipant(player), TimeUnit.TICKS, 2);
       				if (!getPlayer().equals(player)) {
    					
    				}
    			}
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final Duration skill = new Duration(duration, cooldown) {
		
    	@Override
    	protected void onDurationStart() {
    		locations.clear();
    		nowrange = range + addrange;
    		if (random.nextInt(100) + 1 <= chance) {
    			isDark = true;
    		}
    	}
    	
		@Override
		protected void onDurationProcess(int count) {
			
		}
    	
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
    		locations.clear();
			nowrange = range;
		}
		
    }.setPeriod(TimeUnit.TICKS, 1);
    
	@SubscribeEvent
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (e.getPlayer().equals(getPlayer()) && e.getEffectType().equals(Moisture.registration)) {
			e.setCancelled(true);
		}
    }
    
    
}
