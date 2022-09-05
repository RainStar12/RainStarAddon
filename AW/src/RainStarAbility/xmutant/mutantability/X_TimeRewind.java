package RainStarAbility.xmutant.mutantability;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;

@AbilityManifest(name = "X-시간 역행", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §b시간 역행§f: 시간을 역행해 $[TIME_CONFIG]초 전으로 돌아갑니다. 역행 중에는",
		" 어떠한 피해도 입지 않으며, 타게팅의 대상이 되지 않습니다. $[COOLDOWN_CONFIG]",
		" 역행 중 철괴를 다시 우클릭하면 역행 상태를 즉시 멈춥니다.",
		"§7패시브 §8- §b완벽한 타이밍§f: 시간 역행이 사용 가능한 상태에서 §c죽을 위기§f에 처하면",
		" 자동으로 시간을 지난 $[TIME_CONFIG]초간 가장 체력이 많은 때로 역행합니다.",
		"§7철괴 좌클릭 §8- §b§f: "
		}, 
		summarize = {
		"§7철괴 우클릭§f 시 §b$[TIME_CONFIG]초§f 전의 §5포션 효과§7, §a위치§7, §d체력§f으로 되돌아갑니다.",
		"치명적인 피해를 받을 때 §c쿨타임§f이 아니면 자동 §2역행§f합니다."
		})
public class X_TimeRewind {

}
