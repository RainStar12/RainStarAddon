package rainstar.abilitywar.synergy.normalization.ancientability;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;

@AbilityManifest(name = "고대의 영혼수확자", rank = Rank.SPECIAL, species = Species.UNDEAD, explain = {
        "생명체가 죽을 경우 그 자리에 60초간 영혼이 남으며,",
        "가까이 가면 수확할 수 있습니다. 철괴 우클릭 시 수확한 영혼을 모두 방출해",
        "7초간 주위를 떠돌게 합니다. $[COOLDOWN]",
        "주위를 떠도는 영혼들은 생명체에 닿으면 해당 생명체에게 대미지를 주며,",
        "7초 후에는 영혼들을 모두 온 사방으로 흩뿌리며 닿은 생명체들에게",
        "큰 대미지를 줍니다. 스킬을 맞은 모든 적에게 구속 디버프를 부여하며",
        "철괴를 좌클릭하면 수확한 영혼의 개수를 확인합니다."
		})
public class AncientReaper {

}
