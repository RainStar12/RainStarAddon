package RainStarAbility.GhostStory;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "도시전설", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c한눈 팔지 마§f: $[RestartConfig]초간 나를 쳐다보지 않는 플레이어는 나를 볼 수 없습니다.",
		"§7근접 공격 §8- §c눈 깜짝할 새§f: 날 보지 못하는 대상을 때릴 때 악령 효과를 부여합니다.",
		" 또한 악령 효과가 걸린 플레이어에게 대상에게 추가 피해를 입힐 수 있습니다.",
		"§7철괴 우클릭 §8- §c괴담은 퍼져나가고§f: $[DurationConfig]초간 내가 바라보는 모든 대상에게",
		" 악령 효과가 부여됩니다. $[CooldownConfig]",
		"§2[§a아이디어 제공자§f: §ejjapdook§2]"
		})

public class AbstractGhostStory extends AbilityBase {
	
	public AbstractGhostStory(Participant participant) {
		super(participant);
	}
	
	

}
