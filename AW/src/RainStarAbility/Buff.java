package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;

@AbilityManifest(name = "근육돼지", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c근육 강화§f: §6힘 §f또는 §8저항§f $[AMPLIFIER] 버프를 획득합니다. $[EFFECT_CHANGE]초마다 효과는 변경됩니다.",
        "§7철괴 좌클릭 §8- §e파운딩§f: 바라보는 방향으로 도약하고 이후 찍어내립니다.",
        " 범위 내 대상에게 피해입히고 $[STUN_DURATION]초간 §e기절§f시킵니다. $[LEFT_COOLDOWN]",
        "§7철괴 우클릭 §8- §a스테로이드§f: $[CHANNELING]초간 이동 불가 후 체력을 $[HEAL_AMOUNT] §d회복§f합니다. $[RIGHT_COOLDOWN]",
		"§b[§7아이디어 제공자§b] §dhandony"
		},
		summarize = {
		""
		})

public class Buff extends AbilityBase {
	
	public Buff(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> LEFT_COOLDOWN = abilitySettings.new SettingObject<Integer>(
			Buff.class, "left-cooldown", 70, "# 철괴 좌클릭 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> RIGHT_COOLDOWN = abilitySettings.new SettingObject<Integer>(
			Buff.class, "right-cooldown", 80, "# 철괴 우클릭 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> AMPLIFIER = abilitySettings.new SettingObject<Integer>(
			Buff.class, "amplifier", 0, "# 포션 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }

	};
	
	public static final SettingObject<Double> EFFECT_CHANGE = abilitySettings.new SettingObject<Double>(
			Buff.class, "effect-change", 5.0, "# 효과 변경 주기") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Double> STUN_DURATION = abilitySettings.new SettingObject<Double>(
			Buff.class, "stun-duration", 2.0, "# 기절 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> CHANNELING = abilitySettings.new SettingObject<Double>(
			Buff.class, "channeling", 3.0, "# 이동 불가 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> HEAL_AMOUNT = abilitySettings.new SettingObject<Double>(
			Buff.class, "heal-amount", 8.0, "# 체력 회복량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
}
