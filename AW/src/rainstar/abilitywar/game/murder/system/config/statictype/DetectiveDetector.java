package rainstar.abilitywar.game.murder.system.config.statictype;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.Detective;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import rainstar.abilitywar.game.murder.murderer.ScammerMurderer;

@ModuleBase(DetectiveDetector.class)
public class DetectiveDetector implements ListenerModule {
	
	public DetectiveDetector(AbstractGame game) {
		this.game = game;
	}
	
	private final AbstractGame game;
	private Set<Hologram> holograms = new HashSet<>();
	private Set<FakeBlood> fakebloods = new HashSet<>();
	
	private String getJobname(AbilityBase ab) {
		String jobname = ab.getDisplayName();
		if (ab.getClass().equals(ScammerMurderer.class)) jobname = "시민: 광대";
		String color = "";
		if (ab instanceof AbstractInnocent) color = "§a";
		else if (ab instanceof AbstractMurderer) color = "§c";
		else if (ab instanceof Detective) color = "§3";
		return color + jobname;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	private void onMurder(final MurderEvent e) {
		if (!e.isCancelled() && e.getTarget().hasAbility()) {
			new Hologram(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0), getJobname(e.getTarget().getAbility())).start();	
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onArrowKill(final ArrowKillEvent e) {
		if (!e.isCancelled() && e.getTarget().hasAbility()) {
			new Hologram(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0), getJobname(e.getTarget().getAbility())).start();	
		}
	}
	
	@Override
	public void unregister() {
		for (Hologram hologram : holograms) {
			hologram.stop(false);
		}
		for (FakeBlood bloods : fakebloods) {
			bloods.stop(false);
		}	
	}
	
	public void fakeHologram(Location location, AbstractGame game) {
		new Hologram(location.clone().add(0, 1, 0), "§a시민: 광대").start();
		new FakeBlood(location.clone().add(0, 1, 0), game).start();
	}
	
	public class Hologram extends SimpleTimer {

		private final Location location;
		private final String name;
		private final IHologram hologram;

		public Hologram(final Location location, String name) {
			super(TaskType.INFINITE, -1);
			setPeriod(TimeUnit.TICKS, 5);
			this.location = location;
			this.name = name;
			this.hologram = NMS.newHologram(location.getWorld(), location.getX(),
					location.getY() + 0.6, location.getZ(), name);
			for (Participant participant : game.getParticipants()) {
				if (participant.getAbility() instanceof Detective) {
					hologram.display(participant.getPlayer());		
				}
			}
			holograms.add(this);
		}

		@Override
		protected void run(int count) {
			for (Participant participant : game.getParticipants()) {
				if (participant.hasAbility()) {
					if (participant.getAbility().getClass().getSimpleName().contains("Detective")) {
						hologram.display(participant.getPlayer());		
					}	
				}
			}
			hologram.teleport(location.clone().add(0, 0.6, 0));
			hologram.setText(name);
		}
		
		@Override
		protected void onEnd() {
			hologram.unregister();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
		}

	}
	
	private class FakeBlood extends SimpleTimer {

		private final AbstractGame game;
		private final Location location;
		private final RGB fakeRED = RGB.of(254, 80, 54);

		private FakeBlood(final Location location, AbstractGame game) {
			super(TaskType.INFINITE, -1);
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
			this.game = game;
			fakebloods.add(this);
		}

		@Override
		protected void run(int count) {
			for (Participant participant : game.getParticipants()) {
				if (participant.hasAbility()) {
					if (participant.getAbility().getClass().getSimpleName().contains("Detective")) {
						ParticleLib.REDSTONE.spawnParticle(participant.getPlayer(), location, fakeRED);		
					}	
				}
			}
		}

	}
	
}