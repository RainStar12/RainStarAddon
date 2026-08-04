package rainstar.abilitywar.game.lite.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsLiteAbilitySettings;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "유도 화살", rank = Rank.S, species = Species.HUMAN, explain = {
		"§6활§f을 드는 동안 $[MAX_CHARGE]초에 걸쳐 §5유도 게이지바§f가 차오릅니다.",
		"§6활§f 발사 시 게이지를 소모해 비례한 §5유도력§f으로 화살이 §5유도§f됩니다."
		})

public class HomingArrow extends AbilityBase {
	
	public HomingArrow(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> MAX_CHARGE = rsLiteAbilitySettings.new SettingObject<Double>(HomingArrow.class,
			"max-charge", 5.0, "# 유도력 최대 충전 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE = rsLiteAbilitySettings.new SettingObject<Double>(HomingArrow.class,
			"homing-max-range", 7.0, "# 최대 유도 거리") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	@Override
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	handchecker.start();
	    }
	}
	
	private final int maxcharge = (int) (MAX_CHARGE.getValue() * 20);
	private final double range = RANGE.getValue();
	private BossBar bossBar = null;
	private double nowcharge = 0;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof ArmorStand) return false;
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
	
    public AbilityTimer handchecker = new AbilityTimer() {

    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§5유도력", BarColor.PURPLE, BarStyle.SOLID);
    		bossBar.setProgress(0);
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
        @Override
        public void run(int count) {
        	Material mainhand = getPlayer().getInventory().getItemInMainHand().getType();
        	Material offhand = getPlayer().getInventory().getItemInOffHand().getType();
            if (mainhand.equals(MaterialX.BOW.getMaterial()) || offhand.equals(MaterialX.BOW.getMaterial())) {
        		bossBar.addPlayer(getPlayer());
        		nowcharge = Math.min(1, nowcharge + ((mainhand.equals(MaterialX.BOW.getMaterial()) ? 1.0 : 0.5) / maxcharge));
        		bossBar.setProgress(nowcharge);
            } else {
            	bossBar.removePlayer(getPlayer());
            	nowcharge = 0;
            }
        }
        
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}

    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length(), (nowcharge * range)).start();
			nowcharge = 0;
		}
	}
    
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && handchecker.isRunning()) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
	
	private class Homing extends AbilityTimer implements Listener {
		
		private Arrow arrow;
		private double lengths;
		private double nowrange;
		
		private Homing(Arrow arrow, double length, double nowrange) {
			super(TaskType.REVERSE, 50);
			setPeriod(TimeUnit.TICKS, 1);
			this.arrow = arrow;
			this.lengths = length;
			this.nowrange = nowrange;
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int arg0) {
			if (arrow != null) {
				arrow.setGlowing(true);
				Player p = LocationUtil.getNearestEntity(Player.class, arrow.getLocation(), predicate);
				if (p != null) {
					if (arrow.getLocation().distanceSquared(p.getEyeLocation()) <= (nowrange * nowrange)) {
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
					arrow.setGlowing(false);
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
			arrow.setGlowing(false);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}

}
