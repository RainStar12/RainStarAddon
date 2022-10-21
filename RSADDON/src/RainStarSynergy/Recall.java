package RainStarSynergy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.customentity.CustomEntitySetLocationEvent;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@SuppressWarnings("deprecation")
@AbilityManifest(
		name = "리콜", rank = Rank.B, species = Species.HUMAN, explain = {
		"일반 투사체를 바라보면 투사체를 쏜 주인에게 되돌아갑니다.",
		"되돌아가는 투사체는 주인에게 §c2.5배의 피해§f를 입힙니다.",
		"원거리 투사체들을 방어해주는 §b50HP§f의 실드를 가지고 있습니다.",
		"실드는 일반 투사체에 §c-$[LOSS_NORMAL]HP§f, 특수 투사체에 §c-$[LOSS_SPECIAL]HP§f를 잃습니다.",
		"실드에 닿는 일반 투사체는 2초간 정지, 특수 투사체는 되돌려보냅니다.",
		"§bHP§f가 §c0§f 이하가 되면 쿨타임을 가집니다. $[COOLDOWN]"
		})

public class Recall extends Synergy {

	public Recall(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN
	= synergySettings.new SettingObject<Integer>(Recall.class,
			"cooldown", 75, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> LOSS_NORMAL
	= synergySettings.new SettingObject<Integer>(Recall.class,
			"loss-normal", 2, "# 일반 투사체 체력 감소") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> LOSS_SPECIAL
	= synergySettings.new SettingObject<Integer>(Recall.class,
			"loss-special", 5, "# 특수 투사체 체력 감소") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
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
	    	shield.start();
	    }
	}
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private int shieldHP = 50;
	private final int lossNormal = LOSS_NORMAL.getValue();
	private final int lossSpecial = LOSS_SPECIAL.getValue();
	
	private BossBar bossBar = null;
	private Set<CustomEntity> flected = new HashSet<>();
	private Map<Projectile, Player> shooterMap = new HashMap<>();
	private Map<Projectile, Recalling> recallMap = new HashMap<>();
	private Set<Projectile> myprojectiles = new HashSet<>(); 
	private Set<Projectile> stoppedprojectile = new HashSet<>(); 
	private final Map<Projectile, AntiGravitied> antigravityMap = new HashMap<>();
	private static final Circle circle = Circle.of(6, 60);
	private RGB color = RGB.of(72, 254, 254);
	
	private boolean deflect(Deflectable deflectable) {
		if (deflectable != null && !getPlayer().equals(deflectable.getShooter())) {
			deflectable.onDeflect(getParticipant(), deflectable.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().add(deflectable.getDirection().multiply(-1)));
    		if (ServerVersion.getVersion() >= 13) {
    			BlockData diamond = MaterialX.DIAMOND_BLOCK.getMaterial().createBlockData();
    			ParticleLib.FALLING_DUST.spawnParticle(deflectable.getLocation().clone(), 0.1, 0.1, 0.1, 15, 0, diamond);
    		} else {
    			ParticleLib.FALLING_DUST.spawnParticle(deflectable.getLocation().clone(), 0.1, 0.1, 0.1, 15, 0, new MaterialData(Material.DIAMOND_BLOCK));
    		}
			SoundLib.BLOCK_END_PORTAL_FRAME_FILL.playSound(deflectable.getLocation(), 1.5f, 0.75f);
			return true;
		}
		return false;
	}
	
	private final AbilityTimer shield = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§b실드 체력§7: §d" + shieldHP, BarColor.BLUE, BarStyle.SEGMENTED_10);
    		bossBar.setProgress(shieldHP * 0.02);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		if (shieldHP <= 0) cooldown.start();
    		if (cooldown.isRunning()) {
    			if (flected.size() > 0) flected.clear();
    			shieldHP = 50;
    			bossBar.setVisible(false);
    		} else {
    			bossBar.setVisible(true);
    			bossBar.setTitle("§b실드 체력§7: §d" + shieldHP);
    			bossBar.setColor(BarColor.BLUE);
        		bossBar.setProgress(shieldHP * 0.02);
        		
    			for (Projectile projectile : LocationUtil.getNearbyEntities(Projectile.class, getPlayer().getLocation(), 6, 6, null)) {
    				if (!projectile.isOnGround() && !myprojectiles.contains(projectile) && !stoppedprojectile.contains(projectile)) {
    					SoundLib.ITEM_SHIELD_BLOCK.playSound(projectile.getLocation(), 1, 1.5f);
    					new AntiGravitied(projectile).start();
    					shieldHP = Math.max(0, shieldHP - lossNormal);
    				}
    			}
    			if (count % 2 == 0) {
    				for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
    					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
    				}
    			}
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
	private void onCustomEntitySetLocation(final CustomEntitySetLocationEvent e) {
		if (cooldown.isRunning()) return;
		final CustomEntity customEntity = e.getCustomEntity();
		if (customEntity instanceof Deflectable && customEntity.getWorld() == getPlayer().getWorld() && customEntity.getLocation().distanceSquared(getPlayer().getLocation()) <= 36) {
			final Deflectable deflectable = (Deflectable) customEntity;
			if (!deflectable.getShooter().equals(getPlayer()) && !flected.contains(customEntity)) {
				flected.add(customEntity);
				SoundLib.ITEM_SHIELD_BLOCK.playSound(deflectable.getLocation(), 1, 1.5f);
				deflect(deflectable);
				shieldHP = Math.max(0, shieldHP - lossSpecial);	
			}
		}
	}

    
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
					e.setDamage(e.getDamage() * 2.5);
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
    
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && shield.isRunning()) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
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