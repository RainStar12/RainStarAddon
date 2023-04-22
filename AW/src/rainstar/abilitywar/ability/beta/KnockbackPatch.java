package rainstar.abilitywar.ability.beta;

import org.bukkit.attribute.Attribute;
import org.bukkit.event.player.PlayerMoveEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.annotations.Beta;

@Beta

@AbilityManifest(name = "넉백패치", rank = Rank.A, species = Species.OTHERS, explain = {
		"넉백 버그 패치용 능력",
		"현재 기능은 넉백 오류로 넉백되지 않는 플레이어에게 부여 시,",
		"대상이 한 번이라도 움직이면 넉백 버그가 풀리게 됩니다."
		})

public class KnockbackPatch extends AbilityBase {

	public KnockbackPatch(Participant participant) {
		super(participant);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		e.getPlayer().getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0);
	}
	
}