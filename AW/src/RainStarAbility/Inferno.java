package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "인페르노", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"§c불 속성§f의 군주 마검사, 인페르노.",
		"§7패시브 §8- §c업화의 주인§f: 화염 계열 피해를 입을 때마다 §c작§6열§e하는 §6불§c꽃§f을 하나씩 획득합니다.",
		" 검을 휘두를 때마다 불꽃 다섯개를 소모해 화염 검기를 내뿜습니다.",
		" 불꽃은 ",
		"§7검 공격 §8- §c열화폭참§f: ",
		"§7검 우클릭 §8- §c화력전개§f: 나를 10초간 추가 발화시킵니다.",
		" "
		})

public class Inferno extends AbilityBase {

	public Inferno(Participant participant) {
		super(participant);
	}
	
}
