package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "꼬마", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §a보호본능§f: 자신을 공격한 적에게 §b망설임§f을 1 부여합니다.",
		" §b망설임§f × $[CHANCE]%의 확률로 대상은 시야가 뒤틀리고, §b망설임§f이 초기화됩니다.",
		"§7철괴 우클릭 §8- §c방범벨§f: 적들을 §b망설임§f × $[STUN]초간 §e기절§f시킵니다. $[COOLDOWN]",
		"§a[§e능력 제공자§a] §6Ddang_67"
		},
		summarize = {
		""
		})
public class Kid extends AbilityBase {
	
	public Kid(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> CHANCE = 
			abilitySettings.new SettingObject<Integer>(Kid.class, "chance", 20,
			"# 망설임 당 시야가 뒤틀릴 확률") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> STUN = 
			abilitySettings.new SettingObject<Double>(Kid.class, "stun-duration", 1.0,
			"# 기절 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Kid.class, "chance", 20,
			"# 망설임 당 시야가 뒤틀릴 확률") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};

}
