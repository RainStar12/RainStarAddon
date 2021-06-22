package RainStarSynergy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@SuppressWarnings("deprecation")
@AbilityManifest(
		name = "����", rank = Rank.B, species = Species.HUMAN, explain = {
		"�ڽ��� ��b����ü��f�� �������� �ʽ��ϴ�.",
		"�� ��b����ü��f�� ������ ��b����ü��f�� ���κ��� 3ĭ �� �Ÿ��� ������",
		"��b����ü��f�� 2�ʰ� ���ڸ����� ���߰� �˴ϴ�.",
		"0.3�� �̻� ü�� ���̴� 50ĭ ���� ��b����ü��f�� �ٶ󺸸� �ش� ��b����ü��f��",
		"�ڽ��� ���� ������ ������ �ǵ����ϴ�. ���� ��b����ü��f�� Ǯ���ϴ�.",
		"�ǵ��ư��� ȭ�쿡 �ǰݵǴ� ���� �� ����ü�� ��c1.5���� ���ء�f�� �����ϴ�."
		})

public class Recall extends Synergy {

	public Recall(Participant participant) {
		super(participant);
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	passive.start();
	    }
	}
	
	private Map<Projectile, Player> shooterMap = new HashMap<>();
	private Map<Projectile, Recalling> recallMap = new HashMap<>();
	private Set<Projectile> myprojectiles = new HashSet<>(); 
	private Set<Projectile> stoppedprojectile = new HashSet<>(); 
	private final Map<Projectile, AntiGravitied> antigravityMap = new HashMap<>();
	private static final Circle circle = Circle.of(5, 50);
	private RGB color = RGB.of(72, 254, 254);
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
			for (Projectile projectile : LocationUtil.getNearbyEntities(Projectile.class, getPlayer().getLocation(), 5, 5, null)) {
				if (!projectile.isOnGround() && !myprojectiles.contains(projectile) && !stoppedprojectile.contains(projectile)) {
					new AntiGravitied(projectile).start();
				}
			}
			if (count % 2 == 0) {
				for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
				}
			}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (shooterMap.containsKey(e.getDamager()) && e.getEntity().equals(getPlayer())) {
			if (shooterMap.get(e.getDamager()).equals(getPlayer())) {
				e.setCancelled(true);
				Projectile projectile = (Projectile) e.getDamager();
				shooterMap.remove(projectile);
				projectile.setVelocity(new Vector(0, 0, 0));
			}
			if (recallMap.containsKey(e.getDamager()) && shooterMap.containsKey(e.getDamager())) {
				if (e.getEntity().equals(getPlayer())) {
					e.setCancelled(true);
				}
				if (!shooterMap.get(e.getDamager()).equals(e.getEntity())) {
					e.setDamage(e.getDamage() * 1.5);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (e.getEntity().getShooter() != null) {
			if (e.getEntity().getShooter() instanceof Player) {
				if (!shooterMap.containsKey(e.getEntity())) {
					new Flight(e.getEntity()).start();
				}
				if (e.getEntity().getShooter().equals(getPlayer())) {
					myprojectiles.add(e.getEntity());
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getHitBlock() != null) {
			if (shooterMap.containsKey(e.getEntity())) {
				shooterMap.remove(e.getEntity());
			}	
		}
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerMove(PlayerMoveEvent e) {
    	if (LocationUtil.getEntityLookingAt(Projectile.class, getPlayer(), 50, predicate) != null) {
    		Projectile projectile = LocationUtil.getEntityLookingAt(Projectile.class, getPlayer(), 50, predicate);
    		if (!recallMap.containsKey(projectile) && shooterMap.containsKey(projectile)) {
    			if (antigravityMap.containsKey(projectile)) {
    				antigravityMap.get(projectile).stop(true);
    			} else new Recalling(projectile).start();
    		}
    	}
    }
    
    public class Flight extends AbilityTimer implements Listener {
    	
    	private final Projectile projectile;
    	
    	public Flight(Projectile projectile) {
    		super(TaskType.REVERSE, 6);
    		setPeriod(TimeUnit.TICKS, 1);
    		this.projectile = projectile;
    	}
    	
    	@EventHandler()
    	public void onProjectileHit(ProjectileHitEvent e) {
    		if (e.getEntity().equals(projectile)) {
    			stop(false);
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		shooterMap.put(projectile, (Player) projectile.getShooter());
			HandlerList.unregisterAll(this);
    	}
    	
    	@Override
    	public void onSilentEnd() {
			HandlerList.unregisterAll(this);
    	}
    	
    	
    }
    
    public class Recalling extends AbilityTimer {
    	
    	private final Projectile projectile;
    	private double length;
    	
    	public Recalling(Projectile projectile) {
    		super(TaskType.REVERSE, 40);
    		setPeriod(TimeUnit.TICKS, 1);
    		this.projectile = projectile;
    		recallMap.put(projectile, this);
    	}
    	
    	@Override
    	public void onStart() {
    		length = projectile.getVelocity().length();
    		if (ServerVersion.getVersion() >= 13) {
    			BlockData diamond = MaterialX.DIAMOND_BLOCK.getMaterial().createBlockData();
    			ParticleLib.FALLING_DUST.spawnParticle(projectile.getLocation().clone(), 0.1, 0.1, 0.1, 15, 0, diamond);
    		} else {
    			ParticleLib.FALLING_DUST.spawnParticle(projectile.getLocation().clone(), 0.1, 0.1, 0.1, 15, 0, new MaterialData(Material.DIAMOND_BLOCK));
    		}
			SoundLib.BLOCK_END_PORTAL_FRAME_FILL.playSound(projectile.getLocation(), 1.5f, 0.75f);
    	}
    	
    	@Override
    	public void run(int count) {
    		if (shooterMap.containsKey(projectile)) {
    			projectile.setVelocity(VectorUtil.validateVector((shooterMap.get(projectile).getLocation().clone().add(0, 1, 0).toVector()
    					.subtract(projectile.getLocation().toVector())).normalize().multiply(length)));		
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		recallMap.remove(projectile);
    	}
    	
    }
    
	class AntiGravitied extends AbilityTimer {
		
		private Projectile projectile;
		private final Vector zerov = new Vector(0, 0, 0);
		private final Map<Projectile, Vector> velocityMap = new HashMap<>();
		
		private AntiGravitied(Projectile projectile) {
			super(TaskType.NORMAL, 40);
			setPeriod(TimeUnit.TICKS, 1);
			this.projectile = projectile;
			antigravityMap.put(projectile, this);
		}
		
		@Override
		protected void onStart() {
			stoppedprojectile.add(projectile);
			velocityMap.put(projectile, projectile.getVelocity());
			projectile.setGlowing(true);
			projectile.setGravity(false);
			projectile.setVelocity(zerov);
		}
		
		@Override
		protected void onEnd() {
			projectile.setGlowing(false);
			projectile.setGravity(true);
			projectile.setVelocity(velocityMap.get(projectile));
			antigravityMap.remove(projectile);
		}
		
		@Override
		protected void onSilentEnd() {
			projectile.setGlowing(false);
			projectile.setGravity(true);
			projectile.setVelocity(velocityMap.get(projectile));
			antigravityMap.remove(projectile);
			new Recalling(projectile).start();
		}
		
	}
	
}
