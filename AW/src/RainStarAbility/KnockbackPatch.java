package RainStarAbility;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.player.PlayerMoveEvent;

import RainStarEffect.Irreparable;
import RainStarEffect.SnowflakeMark;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@Beta

@AbilityManifest(name = "넉백패치", rank = Rank.A, species = Species.OTHERS, explain = {
		"넉백 버그 패치용 능력",
		"현재 기능은 넉백 오류로 넉백되지 않는 플레이어에게 부여 시,",
		"대상이 한 번이라도 움직이면 넉백 버그가 풀리게 됩니다."
		})

public class KnockbackPatch extends AbilityBase implements ActiveHandler {

	public KnockbackPatch(Participant participant) {
		super(participant);
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		e.getPlayer().getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0);
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			SnowflakeMark.apply(getParticipant(), TimeUnit.SECONDS, 200, 1);
			return true;
		}
		return false;
	}
	
	
}
