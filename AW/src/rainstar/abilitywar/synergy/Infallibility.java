package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.text.DecimalFormat;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "백발백중", rank = Rank.S, species = Species.HUMAN, explain = {
		"자신의 투사체가 $[RANGE]칸 내의 가장 가까운 적에게 유도됩니다.",
		"적중 시마다 빗나갈 때까지 투사체 속도가 $[INCREASE]% 가속됩니다."
		})

public class Infallibility extends Synergy {

	public Infallibility(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> RANGE = rsSynergySettings.new SettingObject<Integer>(Infallibility.class,
			"range", 100, "# 사거리") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> INCREASE = rsSynergySettings.new SettingObject<Integer>(Infallibility.class,
			"velocity-increase", 10, "# 투사체 가속량") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};

	private final int range = RANGE.getValue();
	private final double multiply = INCREASE.getValue() * 0.01;
	private int speed = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0");
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof ArmorStand) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
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
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update("§b가속§7: §d" + df.format(speed * multiply * 100) + "§f%");
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter())) {
			new Homing(e.getEntity(), e.getEntity().getVelocity().length() * (1 + (speed * multiply))).start();
		}
	}
	
	class Homing extends AbilityTimer implements Listener {
		
		private Projectile projectile;
		private double lengths;
		
		private Homing(Projectile projectile, Double length) {
			super(TaskType.REVERSE, 600);
			setPeriod(TimeUnit.TICKS, 1);
			this.projectile = projectile;
			this.lengths = length;
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int arg0) {
			if (projectile != null) {
				projectile.setGlowing(true);
				Player p = LocationUtil.getNearestEntity(Player.class, projectile.getLocation(), predicate);
				if (p != null) {
					if (projectile.getLocation().distanceSquared(p.getEyeLocation()) <= (range * range)) {
						projectile.setGravity(false);
						projectile.setVelocity(VectorUtil.validateVector((p.getPlayer().getEyeLocation().toVector()
									.subtract(projectile.getLocation().toVector())).normalize().multiply(lengths)));	
					}
				}
			}
		}
		
		@EventHandler
		public void onProjectileHit(ProjectileHitEvent e) {
			if (e.getEntity().equals(projectile)) {
				if (e.getHitBlock() != null) {
					speed = 0;
					ac.update("§b가속§7: §d" + df.format(speed * multiply * 100) + "§f%");
					stop(false);
				} else {
					this.pause();
					projectile.setGravity(true);
					projectile.setGlowing(false);
					speed++;
					ac.update("§b가속§7: §d" + df.format(speed * multiply * 100) + "§f%");
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
			projectile.setGravity(true);
			projectile.setGlowing(false);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
}