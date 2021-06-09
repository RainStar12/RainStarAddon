package RainStarSynergy;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityBase.Update;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "백발백중",
		rank = Rank.S, 
		species = Species.HUMAN, 
		explain = {
		"다른 생명체를 바라보면 생명체가 §e발광§f합니다.",
		"이때 내 투사체는 해당 생명체에게 §5유도§f됩니다."
		})

public class Infallibility extends Synergy {

	public Infallibility(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			looking.start();
		}
	}
	
	private Set<Projectile> projectile = new HashSet<>();
	private LivingEntity target = null;
	double lengths = 0;
	private PotionEffect glowing = new PotionEffect(PotionEffectType.GLOWING, 10, 1, true, false);
	
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
	
	private final AbilityTimer looking = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (LocationUtil.getEntityLookingAt(LivingEntity.class, getPlayer(), 100, predicate) != null) {
				if (target != LocationUtil.getEntityLookingAt(LivingEntity.class, getPlayer(), 100, predicate)) {
					target = LocationUtil.getEntityLookingAt(LivingEntity.class, getPlayer(), 100, predicate);
					SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));	
				}
				target.addPotionEffect(glowing);
			} else {
				target = null;
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && e.getEntity() != null) {
			lengths = e.getEntity().getVelocity().length();
			projectile.add(e.getEntity());
			new Homing(e.getEntity()).start();
		}
	}
	
	class Homing extends AbilityTimer {
		
		private Projectile myprojectile;
		
		private Homing(Projectile projectiles) {
			super(TaskType.REVERSE, 300);
			setPeriod(TimeUnit.TICKS, 1);
			this.myprojectile = projectiles;
		}
		
		@Override
		protected void run(int arg0) {
			if (projectile.contains(myprojectile) && target != null) {
				myprojectile.setGravity(false);
				myprojectile.setVelocity(VectorUtil.validateVector((target.getLocation().clone().add(0, 1, 0).toVector()
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