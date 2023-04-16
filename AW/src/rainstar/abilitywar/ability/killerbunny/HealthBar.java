package rainstar.abilitywar.ability.killerbunny;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@ModuleBase(HealthBar.class)
public class HealthBar extends GameTimer implements Module {

	public final AbstractGame abstractGame;
	public final Map<Participant, IHologram> holograms = new HashMap<>();
	public final DecimalFormat df = new DecimalFormat("0.0");
	
	public Set<Player> viewers = new HashSet<>();
	
	public HealthBar(AbstractGame abstractGame) {
		abstractGame.super(TaskType.INFINITE, -1);
		setPeriod(TimeUnit.TICKS, 1);
		this.abstractGame = abstractGame;
	}
	
	@Override
	public void run(int count) {
		for (Participant participant : abstractGame.getParticipants()) {
			if (holograms.containsKey(participant)) {
				for (Player player : viewers) {
					holograms.get(participant).display(player);
				}
				double healthratio = (participant.getPlayer().getHealth() / participant.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) * 100;
				holograms.get(participant).teleport(participant.getPlayer().getEyeLocation().add(0, 0.6, 0));
				holograms.get(participant).setText("§d♥ " + (healthratio <= 33.3 ? "§5" : "§c") + df.format(healthratio) + "%");
			} else {
				IHologram hologram = NMS.newHologram(participant.getPlayer().getWorld(), participant.getPlayer().getEyeLocation().getX(),
						participant.getPlayer().getEyeLocation().getY() + 0.6, participant.getPlayer().getEyeLocation().getZ(), "§7");
				holograms.put(participant, hologram);
			}
		}
	}
	
	@EventHandler()
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (holograms.keySet().contains(e.getParticipant())) {
			holograms.get(e.getParticipant()).unregister();
			holograms.remove(e.getParticipant());
		}
	}
	
	public void addPlayer(Player player) {
		viewers.add(player);
	}

	@Override
	public void register() {
		start();
	}

	@Override
	public void unregister() {
		for (IHologram hologram : holograms.values()) {
			hologram.unregister();
		}
		holograms.clear();
	}

}