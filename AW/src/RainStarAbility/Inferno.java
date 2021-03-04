package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "인페르노", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"§c불 속성§f의 군주 마검사, 인페르노.",
		"§7패시브 §8- §c업화의 주인§f: 자신의 발화 시간 1초당 작열하는 불꽃을 하나씩",
		" 최대 10개까지 획득할 수 있습니다. 불꽃 하나당 자신이 받는 불 계열 피해량이",
		" 5%씩 상승합니다. 작열하는 불꽃이 있을 때, 검을 휘두르면 전방에 불을 내뿜습니다.",
		"§7검 공격 §8- §c열화폭참§f: ",
		"§7검 우클릭 §8- §c화력전개§f: 나를 10초간 추가 발화시킵니다.",
		" "
		})

public class Inferno extends AbilityBase {

	public Inferno(Participant participant) {
		super(participant);
	}
	
}
