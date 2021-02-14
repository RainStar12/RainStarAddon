package RainStarAbility;

import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(
		name = "쿠로", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §8- §8어둠의 추종자§f: 자신이 있는 위치가 어두울수록 스킬 피해량이 증가합니다.",
		"§7근접 타격 후 F §8- §5차원 절단§f: 바라보는 방향으로 빠르게 질주합니다.",
		" 질주하며 지나간 공간을 절단시켜 주변 엔티티들을 끌어와 피해를 입히고",
		" 잠시간 차원의 저편으로 보내버립니다. $[SWORD_COOLDOWN]",
		"§7패시브 §8- §c마안 개방§f: 치명적인 피해를 입었을 때, 체력을 최대 체력의 절반까지",
		" 즉시 회복하고 마안을 개방합니다."
		})

public class Kuro extends AbilityBase {

	public Kuro(Participant participant) {
		super(participant);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager())) {
			
		}
	}
	
}
