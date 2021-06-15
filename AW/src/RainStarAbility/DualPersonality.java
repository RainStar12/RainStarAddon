package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "이중인격", rank = Rank.S, species = Species.GOD, explain = {
		"§7패시브 §8- §c듀얼리즘§f: 두 개의 인격을 가진 채 게임을 시작합니다.",
		" 다른 인격은 능력이 처음으로 활성화될 때의 체력과 인벤토리를 가지고 시작합니다.", 
		"§7인격 §8- §a환상 인격§f: ",
		"§7인격 §8- §a현실 인격§f: 당신은 능력자 전쟁과 연관이 없습니다.",
		" 모든 능력을 환상이라 보기에 모든 능력의 타게팅에서 배제되며,",
		" "
		})

public class DualPersonality extends AbilityBase {
	
	public DualPersonality(Participant participant) {
		super(participant);
	}

}
