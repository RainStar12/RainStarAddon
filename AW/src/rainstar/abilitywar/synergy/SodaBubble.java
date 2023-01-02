package rainstar.abilitywar.synergy;

import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;


@AbilityManifest(
		name = "소다<버블>", rank = Rank.L, species = Species.OTHERS, explain = {
		""
		})

public class SodaBubble extends Synergy {

	public SodaBubble(Participant participant) {
		super(participant);
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		
	}
	
}
