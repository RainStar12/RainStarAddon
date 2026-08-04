package rainstar.abilitywar.game.murder.system.config.statictype;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import rainstar.abilitywar.game.murder.murderer.SpecterMurderer;

@ModuleBase(GhostJobs.class)
public class GhostJobs implements ListenerModule {
	
	public GhostJobs() {
	}
	
	private final Map<Participant, AbilityRegistration> jobMap = new HashMap<>();
	private final Map<Participant, Participant> targetMap = new HashMap<>();

	public AbilityRegistration getGhostJob(Participant participant) {
		if (jobMap.containsKey(participant)) {
			return jobMap.get(participant);
		}
		return null;
	}
	
	public Participant getTarget(Participant participant) {
		if (targetMap.containsKey(participant) ) {
			return targetMap.get(participant);
		}
		return null;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onMurder(final MurderEvent e) {
		if (e.getTarget().hasAbility()) {
			if (jobMap.containsKey(e.getTarget())) jobMap.remove(e.getTarget());
			jobMap.put(e.getTarget(), e.getTarget().getAbility().getRegistration());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onArrowKill(final ArrowKillEvent e) {
		if (e.getTarget().hasAbility()) {
			if (e.getShooter().hasAbility()) {
				if (!(e.getShooter().getAbility() instanceof AbstractMurderer) && !(e.getTarget().getAbility() instanceof AbstractMurderer)) {
					if (jobMap.containsKey(e.getShooter())) jobMap.remove(e.getShooter());
					jobMap.put(e.getShooter(), e.getShooter().getAbility().getRegistration());
				}
			}
			if (jobMap.containsKey(e.getTarget())) jobMap.remove(e.getTarget());
			jobMap.put(e.getTarget(), e.getTarget().getAbility().getRegistration());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	private void onSpecterKill(final ArrowKillEvent e) {
		if (e.getTarget().hasAbility()) {
			if (!e.isCancelled() && e.getTarget().getAbility().getClass().equals(SpecterMurderer.class)) {
				SpecterMurderer specter = (SpecterMurderer) e.getTarget().getAbility();
				targetMap.put(e.getTarget(), specter.getTarget());
			}
		}
	}
	
	@Override
	public void unregister() {
		jobMap.clear();
	}
	
}
