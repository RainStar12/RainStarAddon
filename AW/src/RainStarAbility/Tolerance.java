package RainStarAbility;

import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "내성", rank = Rank.B, species = Species.HUMAN, explain = {
		"생명체와 발사체에 의한 피해가 아닌 피해에 내성을 가져 피해를 입지 않습니다."
		})
public class Tolerance extends AbilityBase {
	
	public Tolerance(Participant participant) {
		super(participant);
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (!e.getCause().equals(DamageCause.PROJECTILE) && !e.getCause().equals(DamageCause.ENTITY_ATTACK) && !e.getCause().equals(DamageCause.ENTITY_EXPLOSION) && !e.getCause().equals(DamageCause.ENTITY_SWEEP_ATTACK)) e.setCancelled(true);
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

}