package rainstar.abilitywar.utils;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

public class Healing {

	private Healing() {}
	
	public static double heal(final Player player, final double health, final RegainReason reason) {
		final EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, health, reason);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			player.setHealth(Math.min(player.getHealth() + event.getAmount(), player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
		}
		return player.getHealth();
	}
	
}
