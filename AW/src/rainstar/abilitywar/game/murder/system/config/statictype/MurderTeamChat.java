package rainstar.abilitywar.game.murder.system.config.statictype;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveMonitoring;

@ModuleBase(MurderTeamChat.class)
public class MurderTeamChat implements ListenerModule {

	private final AbstractGame game;
	
	public MurderTeamChat(AbstractGame game) {
		this.game = game;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onChat(final AsyncPlayerChatEvent e) {
		if (game.isParticipating(e.getPlayer()) && !((MurderMystery) game).isDead(e.getPlayer().getUniqueId())) {
			if (e.getMessage().charAt(0) == '`') {
				e.getRecipients().clear();
				if (game.getParticipant(e.getPlayer()).hasAbility()) {
					if (game.getParticipant(e.getPlayer()).getAbility() instanceof AbstractMurderer) {
						for (Participant participant : game.getParticipants()) {
							if (participant.hasAbility()) {
								if (participant.getAbility() instanceof AbstractMurderer) {
									e.getRecipients().add(participant.getPlayer());
								} else if (participant.getAbility().getClass().equals(DetectiveMonitoring.class)) {
									participant.getPlayer().sendMessage("§4[§5머더§4] §4???§8: §c" + e.getMessage().replaceFirst("`", ""));
								}
							} else if (game.hasModule(GhostJobs.class)) {
								GhostJobs ghostjob = game.getModule(GhostJobs.class);
								if (ghostjob.getGhostJob(participant) != null && AbstractMurderer.class.isAssignableFrom(ghostjob.getGhostJob(participant).getAbilityClass())) {
									e.getRecipients().add(participant.getPlayer());
								}
							}
						}
						e.setFormat("§4[§5머더§4] §4" + e.getPlayer().getName() + "§8: §c" + e.getMessage().replaceFirst("`", ""));
					} else e.getPlayer().sendMessage("§4[§c!§4] §c당신은 머더 팀이 아닙니다.");
				} else e.getPlayer().sendMessage("§4[§c!§4] §c당신은 참가자가 아닙니다.");
			}
		}
	}
	
}