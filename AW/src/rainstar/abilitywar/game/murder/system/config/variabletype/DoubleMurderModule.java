package rainstar.abilitywar.game.murder.system.config.variabletype;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.GameStartEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.Innocent;
import daybreak.abilitywar.game.list.murdermystery.ability.Murderer;
import daybreak.abilitywar.game.list.murdermystery.event.PreAddGoldEvent;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.random.Random;

@ModuleBase(DoubleMurderModule.class)
public class DoubleMurderModule implements ListenerModule {
	
	private AbstractGame game;

	public DoubleMurderModule(AbstractGame game) {
		this.game = game;
	}
	
	@Override
	public void register() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@Override
	public void unregister() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler()
	public void onGameStart(GameStartEvent e) {
		final Random random = new Random();
		final List<Participant> participants = new ArrayList<>(game.getParticipants());
		final int div = (int) Math.max(Math.ceil(participants.size() / 9.0), 1);
		final List<Participant> innocents = new ArrayList<>();
		for (Participant participant : participants) {
			if (participant.getAbility().getClass().equals(Innocent.class)) innocents.add(participant);
		}
		for (int i = 0; i < div; i++) {
			if (innocents.size() > 0) {
				final int index = random.nextInt(innocents.size());
				final Participant murderer = innocents.get(index);
				innocents.remove(murderer);
				try {
					murderer.setAbility(Murderer.class);
				} catch (ReflectiveOperationException ignored) {
				}
			} else break;
		}
	}
	
	@EventHandler
	private void onAddGold(PreAddGoldEvent e) {
		if (!(e.getParticipant().getAbility() instanceof AbstractMurderer)) {
			e.setAmount(Math.max(1, Math.min(64, e.getAmount() * 2)));
		}
	}

}
