package RainStarSynergy;

import org.bukkit.event.player.PlayerChatEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;

@AbilityManifest(
		name = "진은검", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7철괴 우클릭 §8- §c규칙 지정§f: 8가지의 제시된 규칙 중 하나를 선택 가능합니다.",
		" 선택 후에는 다시는 바꿀 수 없습니다.",
		"§7패시브 §8- §cㄴㅂ§f: 지정된 규칙을 어긴 플레이어는 경고 스택을 1씩 적립하며,",
		" 3스택을 넘길 때 즉시 처형됩니다."
		})

public class TrueSilverSword {

	
	@SubscribeEvent
	public void onPlayerChat(PlayerChatEvent e) {
		if (e.getMessage().get)
	}
	
	
}
