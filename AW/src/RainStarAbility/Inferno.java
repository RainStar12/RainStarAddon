package RainStarAbility;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;

@AbilityManifest(name = "인페르노", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"§c불 속성§f의 군주 마검사, 인페르노.",
		"§7패시브 §8- §c업화의 주인§f: 자신이 발화 도중일 때 발화 시간 1초당 작열하는 불꽃을",
		" 최대 10개까지 획득합니다. 작열하는 불꽃 하나당 자신이 받을 화염 피해가",
		" 10%씩 증가합니다. 또한 체력이 30% 이하가 될 때, 화염 피해를 역회복합니다.",
		"§7검 공격 §8- §c열화폭참§f: 대상을 0.5초간 추가 발화시킵니다.",
		" 매 세번째 타격은 추가 발화를 걸지 않는 대신 작열하는 불꽃 하나당 대상에게",
		" 0.3의 추가 피해를 입힙니다. 대상이 발화 도중이 아니라면, 추가 피해 대신",
		" 작열하는 불꽃 스택 에 비례하여 대상을 발화시킵니다.",
		"§7검 우클릭 §8- §c화력전개§f: 나를 10초간 추가 발화시킵니다.",
		" "
		})

public class Inferno {

}
