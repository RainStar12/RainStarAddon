package RainStarGame;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.WeakHashMap;

@ModuleBase(NoDelay.class)
public final class NoDelay implements ListenerModule {

	public static final AttributeModifier PLUS_FIVE = new AttributeModifier(UUID.fromString("73a02314-fe33-11ea-adc1-0242ac120002"), "zerotick", 6, Operation.ADD_NUMBER);

	private final Map<LivingEntity, Integer> entities;

	private void addModifier(final Player player) {
		addModifier0(player, Attribute.GENERIC_ATTACK_SPEED, PLUS_FIVE);
	}

	private void addModifier0(final Player player, final Attribute attribute, final AttributeModifier modifier) {
		try {
			player.getAttribute(attribute).addModifier(modifier);
		} catch (IllegalArgumentException ignored) {}
	}

	private void removeModifier(final LivingEntity livingEntity) {
		livingEntity.getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(PLUS_FIVE);
	}

	public NoDelay(AbstractGame game) {
		final Collection<? extends Participant> participants = game.getParticipants();
		this.entities = new WeakHashMap<>(participants.size());
		for (Participant participant : participants) {
			final Player player = participant.getPlayer();
			if (player.isOnline()) {
				addModifier(player);
			}
		}
	}

	@EventHandler
	private void onPlayerJoin(final PlayerJoinEvent e) {
		final Player player = e.getPlayer();
		addModifier(player);
	}

	@EventHandler
	private void onPlayerQuit(final PlayerQuitEvent e) {
		final Player player = e.getPlayer();
		entities.remove(player);
		removeModifier(player);
	}

	@Override
	public void unregister() {
		HandlerList.unregisterAll(this);
		for (final Iterator<Entry<LivingEntity, Integer>> iterator = entities.entrySet().iterator(); iterator.hasNext();) {
			final Entry<LivingEntity, Integer> entry = iterator.next();
			final LivingEntity livingEntity = entry.getKey();
			if (livingEntity instanceof Player) {
				removeModifier(livingEntity);
			}
			iterator.remove();
		}
	}
}