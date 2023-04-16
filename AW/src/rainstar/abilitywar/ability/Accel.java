package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;

@AbilityManifest(name = "순간 가속", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §b액셀§f: §3가속§f을 획득하고, $[ACCEL_DURATION]초간 §3가속§f × $[SPEED_UP]%만큼 §b이동 속도§f가",
		" §a증가§f합니다. $[MAX_STACK]번 사용 시 §3가속§f을 초기화하고 §c쿨타임§f을 가집니다. $[COOLDOWN]",
		"§7근접 공격 §8- §eE=mc²§f: 내 현재 §b이동 속도§f에 비례하여 피해량이 강력해집니다."
		},
		summarize = {
		""
		})
public class Accel extends AbilityBase {
	
	public Accel(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> ACCEL_DURATION = 
			abilitySettings.new SettingObject<Double>(Accel.class, "accel-duration", 5.0,
					"# 가속 지속시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> SPEED_UP = 
			abilitySettings.new SettingObject<Integer>(Accel.class, "speed-up", 10, 
					"# 가속 당 이동 속도 증가치", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> MAX_STACK = 
			abilitySettings.new SettingObject<Integer>(Accel.class, "max-stack", 7, 
					"# 최대 중첩가능 횟수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Accel.class, "cooldown", 60, 
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

	private final int duration = (int) (ACCEL_DURATION.getValue() * 20);
	private final double speedup = SPEED_UP.getValue() * 0.01;
	private final int maxstack = MAX_STACK.getValue();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	private int accel = 0;
	
	
	
}
