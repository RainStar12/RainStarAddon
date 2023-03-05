package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "파이로매니악", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7철괴 좌클릭 §8- §cTNT 캐논§f: 바라보는 방향으로 §cTNT§f를 날립니다. §c쿨타임 §7: §f60초",
		"§7철괴 우클릭 §8- §3리벤지 붐§f: 5.0초간 공격을 할 수 없고, 받는 피해량이 20% 감소합니다.",
		" 종료 시 피해를 준 대상들에게 준 피해량만큼 폭발 피해를 입힙니다. §c쿨타임 §7: §f110초",
		"§7패시브 §8- §c파이로매니악§f: 폭발 피해량을 받지 않고 그 절반만큼 §d회복§f합니다.",
		" 근접 치명타 공격 3회마다 자신의 위치에 고정 피해 §c2§f의 폭발을 일으킵니다.",
		"§a[§e능력 제공자§a] §3Kim"
		},
		summarize = {
		""
		})
public class Pyromaniac extends AbilityBase {
	
	public Pyromaniac(Participant participant) {
		super(participant);
	}

}
