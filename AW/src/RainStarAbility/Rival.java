package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "라이벌", rank = Rank.L, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c단 한 명의§f: 적에게 피해를 받으면 대상을 §c§n라이벌§f로 지정합니다.",
        " §c§n라이벌§f 외에게 피해를 받지 않으며 §c§n라이벌§f이 아닌 대상을 공격하면",
        " §c§n라이벌§f 지정이 해제됩니다. 혹은 $[DURATION]초가 지나도 해제됩니다.",
        "§7철괴 F키 §8- §c매칭 성사§f: 라이벌이 있을 때 대상에게, 없다면 가장 가까운 대상에게",
        " 매칭을 성사합니다. 대상의 위치로 이동 후 "
        },
        summarize = {
        ""
        })
public class Rival extends AbilityBase {
	
	public Rival(Participant participant) {
		super(participant);
	}

}
