package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "선견지명", rank = Rank.S, species = Species.HUMAN, explain = {
		"철괴로 적을 타격해서 대상을 지정할 수 있습니다.",
		"철괴 우클릭 시, 지정된 대상이 5초 이내로 능력을 사용할 때"
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
