package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "글리치드 크라운", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = {
        "§7철괴 좌클릭 §8- §c?????§f: 최대 10개까지, 무작위 능력을 배열에 집어넣습니다.",
        " 배열에 능력이 많을수록 순환 속도는 점점 더 빨라집니다.",
        "§7철괴 우클릭 §8- §7§k?????§f: 빠르게 순환하고 있는 능력들 중, 우클릭한 타이밍의",
        " 현재 능력을 자신의 능력으로 변경합니다. 이후 §e§o글리치드 크라운§f 효과는 사라집니다."
        },
        summarize = {
        "§7철괴 좌클릭§f 시 배열에 §a능력§f을 하나 추가하고 순환 속도를 가속합니다. §8(§7최대 10§8)",
        "§7철괴 우클릭§f 시 그 타이밍의 §a능력§f을 자신의 §a능력§f으로 변경합니다."
        })
public class GlitchedCrown extends AbilityBase {
	
	public GlitchedCrown(Participant participant) {
		super(participant);
	}
	
	

}
