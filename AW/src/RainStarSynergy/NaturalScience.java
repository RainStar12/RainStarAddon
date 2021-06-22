package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(name = "이과", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b스킬명§f: $[RESTOCK]초마다 무작위의 상태이상 하나가 충전된",
		" 투척용 고통 및 무작위 부정 효과의 포션 하나를 비주류 손에 지급받습니다.",
		" 비주류 손을 사용할 수 없습니다.",
		"§7패시브 §8- §b스킬명§f: 상태이상 효과를 받을 때 "
		})

public class NaturalScience extends Synergy {
	
	public NaturalScience(Participant participant) {
		super(participant);
	}
	
	
	
	

}
