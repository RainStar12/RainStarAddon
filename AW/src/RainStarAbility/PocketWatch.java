package RainStarAbility;

import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;

@AbilityManifest(name = "회중시계", rank = Rank.L, species = Species.OTHERS, explain = {
        "§7철괴 좌클릭 §8- §b빨리감기§f: 이동 속도, 회복 속도가 증가하는 §b시간 가속§f을 켜고 끕니다.",
        "§7패시브 §8- §3시간의 흐름§f: §b시간 가속§f을 연속 유지할수록 점점 그 효과가 증가합니다.",
        " §b시간 가속§f 해제 시나 $[MAX_TIME]초 사용 시 사용 시간 비례 §c쿨타임§f을 가지게 됩니다.",
        "§7가속 간 사망 §8- §a되감기§f: $[RANGE]칸 내 적의 시간을 $[DURATION]초간 정지시킵니다.",
        " 이 안에 정지된 적을 처치한다면 가속 간 가장 체력이 많은 시간으로 §a역행§f합니다.",
        " §a역행§f하지 못하면 사망합니다."
        },
        summarize = {
        ""
        })
public class PocketWatch extends AbilityBase {
	
	public PocketWatch(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> MAX_TIME = abilitySettings.new SettingObject<Integer>(PocketWatch.class,
			"max-time", 60, "# 시간 가속 최대 시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> MULTIPLY = abilitySettings.new SettingObject<Double>(PocketWatch.class,
			"multiply-cooldown", 2.5, "# 사용한 시간의 쿨타임 배수") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> RANGE = abilitySettings.new SettingObject<Double>(PocketWatch.class,
			"range", 7.0, "# 시간 정지 범위") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DURATION = abilitySettings.new SettingObject<Double>(PocketWatch.class,
			"duration", 4.5, "# 시간 정지 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final double range = RANGE.getValue();
	private final int duration = (int) (DURATION.getValue() * 20);
	private final int maxtime = MAX_TIME.getValue() * 20;
	private final double multiply = MULTIPLY.getValue();
	private int usedtime = 0;
	
	private final AbilityTimer timeaccel = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		if (usedtime < maxtime) {
    			usedtime++;
    			getPlayer().setWalkSpeed((float) (getPlayer().getWalkSpeed() + 0.0004));
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), usedtime * 0.0001, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + (usedtime * 0.0001));
				}
    		} else stop(false);
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

}
