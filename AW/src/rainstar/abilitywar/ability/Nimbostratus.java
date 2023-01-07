package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "비구름", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7패시브 §8- §b비구름§f: 자신을 한발짝 늦게 따라오는 구름이 $[RANGE]칸 내에 §b§l비§f를 내립니다.",
		" §b§l비§f에 맞은 적은 시간이 점점 쌓이는 상태이상 §3습기§f를 받습니다.",
		"§7철괴 우클릭 §8- §2기후 조작§f: $[DURATION]초간 비구름의 기후를 변경합니다.",
		" §e체력 절반 이상§7:§f ",
		" §c체력 절반 미만§7:§f ",
		"§9[§3습기§9]§f 이동 속도가 25%, 공격력이 15% 감소합니다."
		},
		summarize = {
		""
		})

public class Nimbostratus extends AbilityBase {
	
	public Nimbostratus(Participant participant) {
		super(participant);
	}
	
	

}
