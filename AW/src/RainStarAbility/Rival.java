package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "라이벌", rank = Rank.L, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c단 한 명의§f: 적에게 피해를 받으면 대상을 §c§n라이벌§f로 지정합니다.",
        " §c§n라이벌§f 외에게 피해를 주고받지 않습니다. $[RIVAL_DURATION]",
        "§7철괴 F키 §8- §b매칭 성사§f: §c§n라이벌§f이 없다면 바라보는 대상을 §c§n라이벌§f로 지정합니다.",
        " §c§n라이벌§f 지속시간을 $[RIVAL_ADDITIONAL_DURATION]초 증가시킵니다. §c§n라이벌§f의 체력을 내 체력까지 회복시키고,",
        " §c§n라이벌§f에게 순간 이동합니다. 남은 지속시간 동안 §c§n라이벌§f의 능력을 획득하고",
        " 공격력이 $[PERCENTAGE]% 증가합니다. $[COOLDOWN]",
        "§7§n라이벌§7 처치 §8- §e챔피언§f: $[WIN_DURATION]초간 이 능력의 모든 스킬이 §3비활성화§f되지만,",
        " 지금까지 처치했던 모든 §c§n라이벌§f들의 능력을 사용할 수 있습니다."
        },
        summarize = {
        ""
        })
public class Rival extends AbilityBase {
	
	public Rival(Participant participant) {
		super(participant);
	}

}
