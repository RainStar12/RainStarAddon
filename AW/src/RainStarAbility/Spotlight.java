package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "스포트라이트", rank = Rank.A, species = Species.OTHERS, explain = {
		"§7패시브 §8- §c스포트라이트§f: 스포트라이트의 범위 내의 모든 생명체들은 발광합니다.",
		" "
		},
		summarize = {
		"§7철괴 우클릭 시§f 무작위 범위 내 착지 중인 생명체들을 띄워올리고",
		"대상들이 다시 발에 땅이 닿으면 $[STUN]초 §e기절§f합니다. $[COOLDOWN]"
		})

public class Spotlight extends AbilityBase {
	
	public Spotlight(Participant participant) {
		super(participant);
	}

}
