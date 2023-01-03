package rainstar.abilitywar.system.rune;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.logging.Logger;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;

public abstract class RuneBase {

	private final Participant participant;
	private final RuneManifest manifest;
	private static final Logger logger = Logger.getLogger(RuneBase.class);
	private final List<ActionbarChannel> actionbarChannels = new LinkedList<>();
	
	protected RuneBase(final Participant participant, RuneManifest manifest) throws IllegalStateException {
		this.participant = participant;
		this.manifest = manifest;
	}
	
	public final Player getPlayer() {
		return participant.getPlayer();
	}
	
	public final Participant getParticipant() {
		return participant;
	}

	public final String getName() {
		return manifest.name();
	}
	
	public final Material getMaterial() {
		return manifest.material();
	}
	
	public final ActionbarChannel newActionbarChannel() {
		final ActionbarChannel channel = participant.actionbar().newChannel();
		actionbarChannels.add(channel);
		return channel;
	}
	
	public final RuneManifest manifest() {
		return manifest;
	}
	
	public static ItemStack createItem() {
		ItemStack rune = new ItemStack();
		return null;
		
	}
	
	
}
