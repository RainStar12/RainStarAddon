package rainstar.abilitywar.ability.beta;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.annotations.Beta;

@Beta
@AbilityManifest(name = "스피드테스터", rank = Rank.A, species = Species.OTHERS, explain = {
		"이동 속도값 테스팅 능력",
		"철괴를 들고 F키를 누르면 현재 이동 속도값을 출력합니다.",
		"철괴 우클릭 시 이동 속도 70% 증가 / 해제합니다."
		})

public class SpeedTester extends AbilityBase implements ActiveHandler {

	public SpeedTester(Participant participant) {
		super(participant);
	}
	
	private final AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.7, Operation.ADD_SCALAR);
	private boolean speed = false;
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && speed) {
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
			speed = false;
		}
	}
	
	@SubscribeEvent
	public void onPlayerSwap(PlayerSwapHandItemsEvent e) {
		if (getPlayer().equals(e.getPlayer()) && e.getMainHandItem().getType().equals(Material.IRON_INGOT)) {
			e.setCancelled(true);
			getPlayer().sendMessage("§avalue§f: §b" + getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue());
		}
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (!speed) {
				try {
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
					speed = true;
				} catch (IllegalArgumentException ignored) {
				}
			} else {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
				speed = false;
			}
			return true;
		}
		return false;
	}
	
}
