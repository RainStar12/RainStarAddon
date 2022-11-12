package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "선견지명", rank = Rank.S, species = Species.HUMAN, explain = {
		"검 들고 F키를 눌러 자신이 가장 마지막으로 피해를 입힌 대상을 $[DURATION]초간 지정합니다.",
		"지정된 대상이 §a액티브 스킬§f을 사용하면 스킬이 캔슬되고, $[STUN]초간 기절됩니다.",
		"예측에 성공할 때마다 이동 속도가 "
		},
		summarize = {
		""
		})
public class Foresight extends AbilityBase {
	
	public Foresight(Participant participant) {
		super(participant);
	}
	
	
	
	@SubscribeEvent
	public void onPreActive(AbilityPreActiveSkillEvent e) {
		
	}
	
	

}
