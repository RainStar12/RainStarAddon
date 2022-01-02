package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.annotations.Beta;

@Beta

@AbilityManifest(
		name = "리듬", rank = Rank.A, species = Species.OTHERS, explain = {
		"상단에 $[PERIOD]초마다 가득 차는 보스바가 생겨납니다.",
		"근접 공격 시, 보스바의 게이지에 따라 각기 다른 판정이 있습니다.",
		"§6§lP§e§lE§a§lR§b§lF§a§lE§e§lC§6§lT§7: §c$[PERFECT]배 §7| §d§lGREAT§7: §c$[GREAT]배 §7| §a§lGOOD§7: §c$[GOOD]배 §7| §b§lBAD§7: §c$[BAD]배 §7| §lMISS§7: §c$[MISS]배"
		})

public class Rhythm extends AbilityBase {
	
	public Rhythm(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Integer> PERIOD = abilitySettings.new SettingObject<Integer>(
			Rhythm.class, "period", 5, "# 보스바의 한 주기") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> PERFECT = abilitySettings.new SettingObject<Double>(
			Rhythm.class, "perfect", 1.4, "# 퍼펙트 대미지 배수") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> GREAT = abilitySettings.new SettingObject<Double>(
			Rhythm.class, "great", 1.2, "# 그렛 대미지 배수") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> GOOD = abilitySettings.new SettingObject<Double>(
			Rhythm.class, "good", 1.0, "# 굿 대미지 배수") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> BAD = abilitySettings.new SettingObject<Double>(
			Rhythm.class, "bad", 0.8, "# 배드 대미지 배수") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> MISS = abilitySettings.new SettingObject<Double>(
			Rhythm.class, "miss", 0.5, "# 미스 대미지 배수") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	
	
	
}