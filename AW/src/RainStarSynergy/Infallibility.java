package RainStarSynergy;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "백발백중",
		rank = Rank.S, 
		species = Species.HUMAN, 
		explain = {
		"자신이 발사하는 모든 발사체가 가장 마지막에 타격한 적에게",
		"자동으로 유도되어 발사됩니다."
		})

public class Infallibility extends Synergy {

	public Infallibility(Participant participant) {
		super(participant);
	}
	
	private Set<Projectile> projectile = new HashSet<>();
	private Player target = null;
	double lengths = 0;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			target = (Player) e.getEntity();
		}
		if (NMS.isArrow(e.getDamager())) {
			Arrow ar = (Arrow) e.getDamager();
			if (ar.getShooter().equals(getPlayer()) && e.getEntity() instanceof Player 
					&& !e.getEntity().equals(getPlayer())) {
				target = (Player) e.getEntity();
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && e.getEntity() != null) {
			lengths = e.getEntity().getVelocity().length();
			projectile.add(e.getEntity());
			if (target != null && predicate.test(target)) {
				new Homing(e.getEntity()).start();
			}
		}
	}
	
	class Homing extends AbilityTimer {
		
		private Projectile myprojectile;
		
		private Homing(Projectile projectiles) {
			super(TaskType.REVERSE, 100);
			setPeriod(TimeUnit.TICKS, 1);
			this.myprojectile = projectiles;
		}
		
		@Override
		protected void run(int arg0) {
			if (projectile.contains(myprojectile) && target != null) {
				myprojectile.setGravity(false);
				myprojectile.setVelocity(VectorUtil.validateVector((target.getPlayer().getEyeLocation().toVector()
						.subtract(myprojectile.getLocation().toVector())).normalize().multiply(lengths)));
			}
		}
		
		@Override
		protected void onEnd() {
			myprojectile.setGravity(true);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
}