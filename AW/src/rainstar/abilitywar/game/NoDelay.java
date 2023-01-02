package rainstar.abilitywar.game;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@ModuleBase(NoDelay.class)
public final class NoDelay implements ListenerModule {

	public static final AttributeModifier PLUS_FIVE = new AttributeModifier(UUID.fromString("f218c7c4-c879-403e-b16c-df81e8383a63"), "zerotick", 6, Operation.ADD_NUMBER);

	private final Set<LivingEntity> entities;

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
		this.entities = new HashSet<>(participants.size());
		for (Participant participant : participants) {
			final Player player = participant.getPlayer();
			if (player.isOnline()) {
				entities.add(player);
				addModifier(player);
			}
		}
	}

	@EventHandler
	private void onPlayerJoin(final PlayerJoinEvent e) {
		final Player player = e.getPlayer();
		entities.add(player);
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
		for (final Iterator<LivingEntity> iterator = entities.iterator(); iterator.hasNext();) {
			final LivingEntity livingEntity = iterator.next();
			if (livingEntity instanceof Player) {
				removeModifier(livingEntity);
			}
			iterator.remove();
		}
	}
}