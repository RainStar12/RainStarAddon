package rainstar.abilitywar.ability.beta;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.nms.Hand;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.effect.Chill;
import rainstar.abilitywar.effect.TimeInterrupt;

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
		TimeInterrupt.apply(getParticipant(), TimeUnit.TICKS, 100);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer())) {
			LivingEntity l = (LivingEntity) e.getEntity();
			new AbilityTimer(TaskType.NORMAL, 10) {
				@Override
				public void run(int count) {
					l.setNoDamageTicks(0);
					if (count == 7) {
						NMS.swingHand(getPlayer(), Hand.OFF_HAND);
						SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation());
						l.damage(2);	
					}
					if (count == 10) {
						NMS.swingHand(getPlayer(), Hand.OFF_HAND);
						SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation());
						l.damage(2);
					}
				}
			}.setPeriod(TimeUnit.TICKS, 1).start();
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			for (Participant participant : getGame().getParticipants()) {
				Chill.apply(participant, TimeUnit.TICKS, 200);
			}
			return true;
		}
		return false;
	}
	
	
}