package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "이터널", rank = Rank.L, species = Species.HUMAN, explain = {
		"§b피격에 의한 기본 무적 시간§f이 $[MULTIPLY]배가 됩니다."
		})

public class Eternal extends AbilityBase {
	
	public Eternal(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> MULTIPLY = rsAbilitySettings.new SettingObject<Double>(Eternal.class,
			"multiply", 2.0, "# 무적 시간 배율") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final double multiply = MULTIPLY.getValue();
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) getPlayer().setMaximumNoDamageTicks((int) (getPlayer().getMaximumNoDamageTicks() * multiply));
		else if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) getPlayer().setMaximumNoDamageTicks(20);
	}

}