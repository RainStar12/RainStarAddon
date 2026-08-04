package rainstar.abilitywar.game.murder.system.config.statictype;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import rainstar.abilitywar.game.murder.innocent.Ghost;
import rainstar.abilitywar.game.murder.innocent.Mediumship;
import rainstar.abilitywar.game.murder.murderer.SpecterMurderer;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveMonitoring;

@ModuleBase(GhostChat.class)
public class GhostChat implements ListenerModule {

	private final AbstractGame game;
	
	public GhostChat(AbstractGame game) {
		this.game = game;
	}
	
	private Map<Participant, String> firstChatMap = new HashMap<>();

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onChat(final AsyncPlayerChatEvent e) {
		MurderMystery murderMystery = (MurderMystery) game;
		if (game.isParticipating(e.getPlayer())) {
			Participant chatter = game.getParticipant(e.getPlayer());
			if (murderMystery.isDead(e.getPlayer().getUniqueId())) {
				e.getRecipients().clear();
				for (Participant participant : game.getParticipants()) {
					if (((MurderMystery) game).isDead(participant.getPlayer().getUniqueId())) {
						e.getRecipients().add(participant.getPlayer());
					}
					if (participant.hasAbility()) {
						if (participant.getAbility().getClass().equals(Mediumship.class)) {
							e.getRecipients().add(participant.getPlayer());
						}
					}
				}
				if (game.hasModule(GhostJobs.class)) {
					GhostJobs ghostjob = game.getModule(GhostJobs.class);
					if (ghostjob.getGhostJob(chatter) != null && ghostjob.getGhostJob(chatter).getAbilityClass().equals(SpecterMurderer.class) && ghostjob.getTarget(chatter) != null) {
						e.getRecipients().add(ghostjob.getTarget(chatter).getPlayer());
					}
				}
				e.setFormat("§8[§7유령§8] §7§o" + e.getPlayer().getName() + "§f§o: §7§o" + e.getMessage());
				if (!firstChatMap.containsKey(game.getParticipant(e.getPlayer()))) firstChatMap.put(game.getParticipant(e.getPlayer()), e.getMessage());
			} else if (game.getParticipant(e.getPlayer()).hasAbility() && game.getParticipant(e.getPlayer()).getAbility().getClass().equals(Mediumship.class)) {
				e.getRecipients().clear();
				for (Participant participant : game.getParticipants()) {
					if (((MurderMystery) game).isDead(participant.getPlayer().getUniqueId())) {
						e.getRecipients().add(participant.getPlayer());
					}
					if (participant.hasAbility()) {
						if (participant.getAbility().getClass().equals(Mediumship.class) || participant.getAbility().getClass().equals(DetectiveMonitoring.class) || participant.getAbility().getClass().equals(Ghost.class)) {
							e.getRecipients().add(participant.getPlayer());
						}
					}
				}
				e.setFormat("§3[§b영매§3] §b§o" + e.getPlayer().getName() + "§f§o: §7§o" + e.getMessage());
			}	
		}
	}
	
	public String getFirstGhostChat(Participant participant) {
		if (firstChatMap.containsKey(participant)) return firstChatMap.get(participant);
		else return "§7";
	}
	
}