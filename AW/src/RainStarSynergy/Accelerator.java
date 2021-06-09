package RainStarSynergy;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import RainStarEffect.Confusion;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "액셀러레이터", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b스태미나§f: 스태미나를 회복하여 총 5초에 1 게이지가 찹니다.",
		" 스태미나는 전투 도중에는 더 적게 차오르고, 더 많이 소모합니다.",
		"§7검 들고 F키 §8- §3대시§f: 바라보는 방향의 수평으로 짧게 대시합니다. §c소모 §7: §b3",
		" 대시로 이동한 거리에 대시 잔상이 남아 닿은 적에게 피해를 입힙니다.",
		" 대시 잔상에 누군가가 맞을 때마다 스태미나를 회복합니다. §d회복 §7: §b0.1",
		"§7공격 후 대시 §8- §e광속§f: 근접 공격 후 0.15초 내에 대시할 경우 대상에게 4초간",
		" §c출혈§f 및 무작위 방향으로 튕겨나가는 §6혼란§f 효과를 부여합니다. §d회복 §7: §b0.5"})

public class Accelerator extends Synergy {

	public Accelerator(Participant participant) {
		super(participant);
	}
	
	private static final Set<Material> swords;
	private double stamina = 20;
	private BossBar bossBar = null;
	
	private Location startLocation;
	private Participant target;
	private static final Vector zerov = new Vector(0, 0, 0);
	int timer = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(75) * 5 : 5);
	private PotionEffect invisible = new PotionEffect(PotionEffectType.INVISIBILITY, 3, 0, true, false);
	private ItemStack[] armors;
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	staminaupdater.start();
	      } 
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
	
	public void staminaUse(double value) {
		stamina = Math.max(0, stamina - value);
	}
	
	public void staminaGain(double value) {
		stamina = Math.min(20, stamina + value);
	}
	
	private final AbilityTimer attacked = new AbilityTimer(3) {
		
		@Override
		public void run(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer staminaupdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("스태미나", BarColor.BLUE, BarStyle.SEGMENTED_20);
    		bossBar.setProgress(stamina * 0.05);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		if (timer == 0) {
    			staminaGain(0.75);
    			bossBar.setProgress(stamina * 0.05);
    		} else {
    			staminaGain((double) 1 / (timer * 20));
    			bossBar.setProgress(stamina * 0.05);	
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
	
	private final AbilityTimer dashing = new AbilityTimer(1) {
		
		@Override
		public void onStart() {
	    	getPlayer().addPotionEffect(invisible);
	    	getPlayer().setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().normalize().multiply(10)));
	    	getPlayer().getInventory().setArmorContents(null);
			getParticipant().attributes().TARGETABLE.setValue(false);
	    	if (attacked.isRunning()) {
		   		Bleed.apply(getGame(), target.getPlayer(), TimeUnit.SECONDS, 2);
		    	Confusion.apply(target, TimeUnit.SECONDS, 2, 10);
		    	staminaGain(0.5);
			}
	   	}
	 
	   	@Override
		public void run(int count) {
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.5f);
	   	}
	   	
	   	@Override
	   	public void onEnd() {
	   		onSilentEnd();
	   	}
	    	
	   	@Override
	    public void onSilentEnd() {
			getPlayer().setVelocity(zerov);
			getPlayer().getInventory().setArmorContents(armors);
	   		SoundLib.ENTITY_FIREWORK_ROCKET_BLAST.playSound(getPlayer().getLocation());
			getParticipant().attributes().TARGETABLE.setValue(true);
			new BukkitRunnable() {
				
				@Override
				public void run() {
					new AfterImage().start();
				}
				
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
	   	}
	    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
    		if (!dashing.isRunning()) {
    			if (stamina >= 3) {
        	    	startLocation = getPlayer().getLocation();
        	    	armors = getPlayer().getInventory().getArmorContents();
            		staminaUse(3);
                	dashing.start();
        		} else {
        			getPlayer().sendMessage("§f[§c!§f] §c스태미나가 부족합니다.");
        		}	
    		}
    		e.setCancelled(true);
    	}
    }
    
    @SubscribeEvent(onlyRelevant = true)
    public void onEntityDamage(EntityDamageEvent e) {
    	if (dashing.isRunning() && e.getEntity().equals(getPlayer())) {
    		e.setCancelled(true);
    	}
    	if (!e.isCancelled() && getPlayer().equals(e.getEntity()) && e.getCause().equals(DamageCause.FALL)) {
			getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
			e.setCancelled(true);
		}
    }
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			target = getGame().getParticipant((Player) e.getEntity());
    		if (attacked.isRunning()) attacked.setCount(3);
    		else attacked.start();
    	}
    	onEntityDamage(e);
    }
    
    @SubscribeEvent
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
    	onEntityDamage(e);
    }
    
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && staminaupdater.isRunning()) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
    
    public class AfterImage extends AbilityTimer {
    	
    	Set<Damageable> damagedcheck = new HashSet<>();
    	Location saveloc1;
    	Location saveloc2;
    	
    	private AfterImage() {
    		super(TaskType.REVERSE, 60);
    		setPeriod(TimeUnit.TICKS, 1);
    	}
    	
    	@Override
    	protected void onStart() {
	   		for (Location loc : Line.between(startLocation, getPlayer().getLocation(), (int) Math.min(300, (25 * Math.sqrt(startLocation.distance(getPlayer().getLocation()))))).toLocations(startLocation)) {
	   			ParticleLib.END_ROD.spawnParticle(loc.add(0, 1, 0), 0, 0, 0, 1, 0);
	   			saveloc1 = startLocation;
	   			saveloc2 = getPlayer().getLocation();
	   		}
    	}
    	
    	@Override
    	protected void run(int count) {
    		for (Damageable p : LocationUtil.rayTraceEntities(Damageable.class, saveloc1, saveloc2, 0.75, predicate)) {
    			if (!p.equals(getPlayer()) && !damagedcheck.contains(p)) {
    				if (p instanceof Player) {
    					if (getGame().getParticipant((Player) p).hasEffect(Confusion.registration)) {
            				if (count < 55) {
            				Damages.damageMagic(p, getPlayer(), true, 1);
            				staminaGain(0.1);
	        			    	new AbilityTimer(10) {
	        			    			
	        			    		@Override
	        			    		protected void onStart() {
	        			    			damagedcheck.add(p);
	        			    		}
	        			    		
	        			    		@Override
	        			    		protected void onEnd() {
	        			    			onSilentEnd();
	        			    		}
	        			    			
	        			    		@Override
	        			    		protected void onSilentEnd() {
	        			    			damagedcheck.remove(p);
	        			    		}
	        			    			
	        			    	}.setPeriod(TimeUnit.TICKS, 1);
            				}
            			} else {
            				Damages.damageMagic(p, getPlayer(), true, 1);
            				staminaGain(0.1);
        			    	new AbilityTimer(10) {
    			    			
        			    		@Override
        			    		protected void onStart() {
        			    			damagedcheck.add(p);
        			    		}
        			    		
        			    		@Override
        			    		protected void onEnd() {
        			    			onSilentEnd();
        			    		}
        			    			
        			    		@Override
        			    		protected void onSilentEnd() {
        			    			damagedcheck.remove(p);
        			    		}
        			    			
        			    	}.setPeriod(TimeUnit.TICKS, 1);
            			}
    				} else {
        				Damages.damageMagic(p, getPlayer(), true, 1);
        				staminaGain(0.1);
    			    	new AbilityTimer(10) {
			    			
    			    		@Override
    			    		protected void onStart() {
    			    			damagedcheck.add(p);
    			    		}
    			    		
    			    		@Override
    			    		protected void onEnd() {
    			    			onSilentEnd();
    			    		}
    			    			
    			    		@Override
    			    		protected void onSilentEnd() {
    			    			damagedcheck.remove(p);
    			    		}
    			    			
    			    	}.setPeriod(TimeUnit.TICKS, 1);
    				}
    			}
    		}
    	}
    	
    	@Override
    	protected void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	protected void onSilentEnd() {
    		damagedcheck.clear();
    	}
    	
    }
	
}
