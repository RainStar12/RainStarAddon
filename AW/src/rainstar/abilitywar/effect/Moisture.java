package rainstar.abilitywar.effect;

import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;

@EffectManifest(name = "습기", displayName = "§3습기", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.MOVEMENT_INTERRUPT, EffectType.COMBAT_RESTRICTION
}, description = {
		"이동 속도가 25%, 공격력이 15% 감소합니다.",
		"이 상태이상은 시간이 중첩됩니다."
})
public class Moisture {

}
