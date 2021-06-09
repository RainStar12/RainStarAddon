package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(
		name = "별이 빛나는 밤", rank = Rank.A, species = Species.OTHERS, explain = {
		"§7패시브 §8- §e밤하늘§f: 이동할 때마다 시간을 밤으로 점점 바꿉니다.",
		" 밤이 되기 전까진 모든 능력을 사용할 수 없습니다.",
		"§7표식 §8- §e별무리§f: 근접 공격을 시도할 때마다 대상에게 표식을 부여합니다.",
		" 표식이 부여될 때마다 대상은 내게 끌어당겨지고"
		})

public class StarryNight extends Synergy {
	
	public StarryNight(Participant participant) {
		super(participant);
	}

}
