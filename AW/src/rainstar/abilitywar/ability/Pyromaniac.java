package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;

@AbilityManifest(name = "파이로매니악", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7철괴 좌클릭 §8- §cTNT 캐논§f: 바라보는 방향으로 §cTNT§f를 날립니다. $[TNT_CANNON_COOLDOWN]",
		"§7철괴 우클릭 §8- §3리벤지 붐§f: $[REVENGE_BOOM_DURATION]초간 공격을 할 수 없고, 받는 피해량이 $[REVENGE_BOOM_DECREASE]% 감소합니다.",
		" 종료 시 피해를 준 대상들에게 준 피해량만큼 폭발 피해를 입힙니다. $[REVENGE_BOOM_COOLDOWN]",
		"§7패시브 §8- §c파이로매니악§f: 폭발 피해량을 받지 않고 그 절반만큼 §d회복§f합니다.",
		" 근접 치명타 공격 $[CRIT_COUNT]회마다 자신의 위치에 고정 피해 §c$[EXPLOSIVE_DAMAGE]§f의 폭발을 일으킵니다.",
		"§a[§e능력 제공자§a] §3Dire5778"
		},
		summarize = {
		""
		})
public class Pyromaniac extends AbilityBase {
	
	public Pyromaniac(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> TNT_CANNON_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Pyromaniac.class, "tnt-cannon-cooldown", 60,
			"# TNT 캐논 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> REVENGE_BOOM_DURATION = 
			abilitySettings.new SettingObject<Double>(Pyromaniac.class, "revenge-boom-duration", 5.0,
			"# 리벤지 붐 지속시간", "# 단위: 초") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> REVENGE_BOOM_DECREASE = 
			abilitySettings.new SettingObject<Integer>(Pyromaniac.class, "revenge-boom-decrease", 20,
			"# 리벤지 붐 받는 피해량 감소량", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> REVENGE_BOOM_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Pyromaniac.class, "revenge-boom-cooldown", 110,
			"# 리벤지 붐 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> CRIT_COUNT = 
			abilitySettings.new SettingObject<Integer>(Pyromaniac.class, "crit-count", 3,
			"# 근접 치명타 공격 횟수마다 폭발 발동") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> EXPLOSIVE_DAMAGE = 
			abilitySettings.new SettingObject<Double>(Pyromaniac.class, "explosive-damage", 2.5,
			"# 고정 피해 폭발 대미지") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
}
