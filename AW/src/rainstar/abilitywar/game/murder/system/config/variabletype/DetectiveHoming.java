package rainstar.abilitywar.game.murder.system.config.variabletype;

import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import rainstar.abilitywar.game.murder.innocent.Mediumship;
import rainstar.abilitywar.game.murder.murderer.CorruptionDetectiveMurderer;
import rainstar.abilitywar.game.murder.murderer.FanaticMurderer;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveHomingUp;

@ModuleBase(DetectiveHoming.class)
public class DetectiveHoming implements ListenerModule {
	
	private AbstractGame game;
	
	public DetectiveHoming(AbstractGame game) {
		this.game = game;
	}
	
	@EventHandler
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		for (Participant participant : game.getParticipants()) {
			if (participant.hasAbility()) {
				if (participant.getPlayer().equals(e.getEntity().getShooter()) && e.getEntity() instanceof Arrow) {	
					if (participant.getAbility().getClass().equals(DetectiveHomingUp.class)) new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length(), (Player) e.getEntity().getShooter(), 5).start();		
					else if (participant.getAbility().getClass().equals(CorruptionDetectiveMurderer.class)) new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length(), (Player) e.getEntity().getShooter(), 3).start();		
					else if (participant.getAbility().getClass().getSimpleName().contains("Detective")) new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length(), (Player) e.getEntity().getShooter(), 3).start();
					else if (participant.getAbility().getClass().equals(Mediumship.class)) {
						Mediumship mediumship = (Mediumship) participant.getAbility();
						if (mediumship.getSubAbility() != null) {
							if (mediumship.getSubAbility().getClass().equals(DetectiveHomingUp.class)) new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length(), (Player) e.getEntity().getShooter(), 5).start();	
							else if (mediumship.getSubAbility().getClass().getSimpleName().contains("Detective")) new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length(), (Player) e.getEntity().getShooter(), 3).start();	
						}
					} else if (participant.getAbility().getClass().equals(FanaticMurderer.class)) {
						FanaticMurderer fanatic = (FanaticMurderer) participant.getAbility();
						if (fanatic.getSubAbility() != null) {
							if (fanatic.getSubAbility().getClass().equals(DetectiveHomingUp.class)) new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length(), (Player) e.getEntity().getShooter(), 5).start();	
							else if (fanatic.getSubAbility().getClass().getSimpleName().contains("Detective")) new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length(), (Player) e.getEntity().getShooter(), 3).start();	
						}
					}
				}	
			}
		}
	}
	
	private class Homing extends SimpleTimer implements Listener {
		
		private final Arrow arrow;
		private final double lengths;
		private final Player shooter;
		private final double range;
		
		private final Predicate<Entity> predicate = new Predicate<Entity>() {
			@Override
			public boolean test(Entity entity) {
				if (entity.equals(shooter)) return false;
				return !(entity instanceof Player) || (game.isParticipating(entity.getUniqueId())
						&& !((MurderMystery) game).isDead(entity.getUniqueId())
						&& game.getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
			}
		};
		
		private Homing(Arrow arrow, double length, Player shooter, double range) {
			super(TaskType.REVERSE, 100);
			setPeriod(TimeUnit.TICKS, 1);
			this.arrow = arrow;
			this.lengths = length;
			this.shooter = shooter;
			this.range = range;
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int arg0) {
			if (arrow != null && arg0 < 100) {
				Player p = LocationUtil.getNearestEntity(Player.class, arrow.getLocation(), predicate);
				if (p != null) {
					if (arrow.getLocation().distanceSquared(p.getEyeLocation()) <= range) {
						arrow.setGravity(false);
						arrow.setVelocity(VectorUtil.validateVector((p.getPlayer().getEyeLocation().toVector()
									.subtract(arrow.getLocation().toVector())).normalize().multiply(lengths)));	
					}
				}
			}
		}
		
		@EventHandler
		public void onProjectileHit(ProjectileHitEvent e) {
			if (e.getEntity().equals(arrow)) {
				if (e.getHitBlock() != null) stop(false);
				else {
					this.pause();
					arrow.setGravity(true);
					new BukkitRunnable() {
						@Override
						public void run() {
							stop(false);
						}
					}.runTaskLater(AbilityWar.getPlugin(), 1L);
				}
			}
		}
		
		@Override
		protected void onEnd() {
			HandlerList.unregisterAll(this);
			arrow.setGravity(true);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
}
